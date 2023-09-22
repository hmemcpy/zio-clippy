package clippy

import clippy.utils._

import scala.reflect.internal.Reporter
import scala.reflect.internal.util.{CodeAction, Position}
import scala.tools.nsc.Settings
import scala.tools.nsc.reporters.FilteringReporter

// copied over to avoid dealing with Scala 2.13.12
final class ZIOErrorReporter(val settings: Settings, underlying: Reporter, showOriginalError: Boolean, additionalTypes: List[String])
    extends FilteringReporter {

  val IsZIOTypeError = new IsZIOTypeErrorExtractor(additionalTypes)

  override def doReport(pos: Position, msg: String, severity: Severity, actions: List[CodeAction]): Unit =
    severity match {
      case Reporter.ERROR =>
        msg match {
          case IsZIOTypeError(kind, found, required) =>
            val error = makeError(found, required, msg, kind, showOriginalError)
            underlying.error(pos, error, actions)
          case IsCannotProveMismatch(found, required) =>
            val error = makeError(found, required, msg, ErrorKind.CannotProve, showOriginalError)
            underlying.error(pos, error, actions)
          case _ =>
            underlying.error(pos, msg, actions)
        }
      case _ =>
        severity match {
          case Reporter.INFO    => underlying.echo(pos, msg, actions)
          case Reporter.WARNING => underlying.warning(pos, msg, actions)
          case Reporter.ERROR   => underlying.error(pos, msg, actions)
        }
    }
}
