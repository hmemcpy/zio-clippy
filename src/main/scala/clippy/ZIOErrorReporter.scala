package clippy

import ansi.AnsiStringOps
import utils.{Info, IsZIOTypeMismatch}

import scala.reflect.internal.Reporter
import scala.reflect.internal.util.Position
import scala.tools.nsc.Settings
import scala.tools.nsc.reporters.FilteringReporter

final class ZIOErrorReporter(val settings: Settings, underlying: Reporter, showOriginalError: Boolean)
    extends FilteringReporter {
  private def makeError(found: Info, required: Info, msg: String): String = {
    def envMismatch = {
      val diff = found.R -- required.R -- Set("Any")

      if (diff.nonEmpty) {
        Some(s"""Your effect requires the following environment, but it was not provided:
                |
                |${diff.map(r => s"${"❯ ".red}$r").toList.mkString("\n")}
                |""".stripMargin)
      } else None
    }

    def errorMismatch = {
      val diff = found.E != required.E && required.E != "Any"

      if (diff) {
        Some(s"""Your effect has an error type mismatch:
                |
                |${"❯ ".red}${"Required".bold}: ${required.E.bold}
                |${"❯ ".red}${"Found   ".bold}: ${found.E.bold}
                |""".stripMargin)
      } else None
    }

    def returnMismatch = {
      val diff = found.A != required.A

      if (diff) {
        Some(s"""Your effect has a return type mismatch:
                |
                |${"❯ ".red}${"Required".bold}: ${required.A.bold}
                |${"❯ ".red}${"Found   ".bold}: ${found.A.bold}
                |""".stripMargin)
      } else None
    }

    val originalMessage = Option.when(showOriginalError) {
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
    (severity, msg) match {
      case (Reporter.ERROR, IsZIOTypeMismatch(found, required)) =>
        val error = makeError(found, required, msg)
        underlying.error(pos, error)
      case _ =>
        severity match {
          case Reporter.INFO    => underlying.echo(pos, msg)
          case Reporter.WARNING => underlying.warning(pos, msg)
          case Reporter.ERROR   => underlying.error(pos, msg)
        }
    }
}
