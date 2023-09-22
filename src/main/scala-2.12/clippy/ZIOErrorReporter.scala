package clippy

import clippy.utils._

import scala.reflect.internal.Reporter
import scala.reflect.internal.util.Position
import scala.tools.nsc.Settings
import scala.tools.nsc.reporters.FilteringReporter

final class ZIOErrorReporter(val settings: Settings, underlying: Reporter, showOriginalError: Boolean, additionalTypes: List[String])
    extends FilteringReporter {

  val IsZIOTypeError = new IsZIOTypeErrorExtractor(additionalTypes)
  override def doReport(pos: Position, msg: String, severity: Severity): Unit =
    severity match {
      case Reporter.ERROR =>
        msg match {
          case IsZIOTypeError(kind, found, required) =>
            val error = makeError(found, required, msg, kind, showOriginalError)
            underlying.error(pos, error)
          case IsCannotProveMismatch(found, required) =>
            val error = makeError(found, required, msg, ErrorKind.CannotProve, showOriginalError)
            underlying.error(pos, error)
          case _ =>
            underlying.error(pos, msg)
        }
      case _ =>
        severity match {
          case Reporter.INFO    => underlying.echo(pos, msg)
          case Reporter.WARNING => underlying.warning(pos, msg)
          case Reporter.ERROR   => underlying.error(pos, msg)
        }
    }
}
