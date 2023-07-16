package clippy

import clippy.OutputMessages.{cannotProveMessages, mismatchMessages, overridingMessages}
import clippy.ansi.AnsiStringOps

import scala.reflect.internal.Reporter
import scala.reflect.internal.util.Position
import scala.tools.nsc.Settings
import scala.tools.nsc.reporters.FilteringReporter

final class ZIOErrorReporter(val settings: Settings, underlying: Reporter, showOriginalError: Boolean)
    extends FilteringReporter {

  private def makeError(found: Info, required: Info, msg: String, errorKind: ErrorKind): String = {
    val outputMessages = errorKind match {
      case ErrorKind.Overriding   => overridingMessages
      case ErrorKind.TypeMismatch => mismatchMessages
      case ErrorKind.CannotProve  => cannotProveMessages
    }

    val messages = outputMessages.format(found, required)

    val originalMessage =
      Option.when(showOriginalError) {
        s"""|${"-" * 80}
            |$msg
            |""".stripMargin
      }

    val allMessages = messages ++ originalMessage :: Nil

    s"""|
        |${"  ZIO Type Mismatch  ".red.bold.inverted}
        |
        |${allMessages.flatten.mkString("\n")}
        |""".stripMargin
  }

  override def doReport(pos: Position, msg: String, severity: Severity): Unit =
    severity match {
      case Reporter.ERROR =>
        msg match {
          case IsZIOTypeError(kind, found, required) =>
            val error = makeError(found, required, msg, kind)
            underlying.error(pos, error)
          case IsCannotProveMismatch(found, required) =>
            val error = makeError(found, required, msg, ErrorKind.CannotProve)
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
