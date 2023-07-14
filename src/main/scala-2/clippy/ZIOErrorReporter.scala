package clippy

import clippy.ansi.AnsiStringOps
import clippy.utils.{ErrorKind, Info, IsCannotProveMismatch, IsZIOTypeError}

import scala.reflect.internal.Reporter
import scala.reflect.internal.util.Position
import scala.tools.nsc.Settings
import scala.tools.nsc.reporters.FilteringReporter

final class ZIOErrorReporter(val settings: Settings, underlying: Reporter, showOriginalError: Boolean)
    extends FilteringReporter {

  private case class TitleMessages(envMismatch: String, errorMismatch: String, returnMismatch: String)
  private val mismatchMessages = TitleMessages(
    envMismatch = "Your effect requires the following environment, but it was not provided:",
    errorMismatch = "Your effect has an error type mismatch:",
    returnMismatch = "Your effect has a return type mismatch:"
  )

  private val overridingMessages = TitleMessages(
    envMismatch =
      "Your effect requires the following environment in overriding, but it was not presented in original type:",
    errorMismatch = "Your effect has an error type mismatch in overriding:",
    returnMismatch = "Your effect has a return type mismatch:"
  )

  private def makeError(found: Info, required: Info, msg: String, errorKind: ErrorKind): String = {
    val titleMessages = errorKind match {
      case ErrorKind.Overriding   => overridingMessages
      case ErrorKind.TypeMismatch => mismatchMessages
    }

    val (rDiff, eDiff, aDiff) = found.diff(required)

    def envMismatch =
      Option.when(rDiff.nonEmpty) {
        s"""${titleMessages.envMismatch}
           |
           |${rDiff.map(r => s"${"❯ ".red}${r.bold}").toList.mkString("\n")}
           |""".stripMargin
      }

    def errorMismatch =
      Option.when(eDiff) {
        s"""${titleMessages.errorMismatch}
           |
           |${"❯ ".red}${"Required"}: ${required.E.bold}
           |${"❯ ".red}${"Found   "}: ${found.E.bold}
           |""".stripMargin
      }

    def returnMismatch =
      Option.when(aDiff) {
        s"""${titleMessages.returnMismatch}
           |
           |${"❯ ".red}${"Required"}: ${required.A.bold}
           |${"❯ ".red}${"Found   "}: ${found.A.bold}
           |""".stripMargin
      }

    val originalMessage =
      Option.when(showOriginalError) {
        s"""|${"-" * 80}
            |$msg
            |""".stripMargin
      }

    val allErrors = envMismatch :: errorMismatch :: returnMismatch :: originalMessage :: Nil

    s"""|
        |${"  ZIO Type Mismatch  ".red.bold.inverted}
        |
        |${allErrors.flatten.mkString("\n")}
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
            val error = makeError(found, required, msg, ErrorKind.TypeMismatch)
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
