package zio

import zio.ansi.AnsiStringOps
import zio.utils.{Info, IsZIOTypeMismatch}

import scala.reflect.internal.Reporter
import scala.reflect.internal.util.Position
import scala.tools.nsc._
import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.reporters._

final class Plugin(override val global: Global) extends plugins.Plugin {
  override val description: String = "Better error reporting for ZIO"
  override val name: String        = "ZIO Plugin"

  global.reporter = new ZIOErrorReporter(global.settings, global.reporter)

  override val components: List[PluginComponent] = Nil
}

class ZIOErrorReporter(val settings: Settings, underlying: Reporter) extends FilteringReporter {
  private def makeError(found: Info, required: Info): String = {
    def envMismatch = {
      val diff = found.R -- required.R -- Set("Any")

      if (diff.nonEmpty) {
        Some(s"""Your effect requires the following environment, but it was not provided:
                |
                |${diff.map(r => s"${"❯ ".red}$r").toList.mkString("\n")}
                |
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
                |
                |""".stripMargin)
      } else None
    }

    def returnMismatch = {
      val diff = found.A != required.A

      if (diff) {
        Some(s"""Your effect has a return type mismatch:
                |
                |❯ ${"Required".bold}: ${required.A.bold}
                |❯ ${"Found   ".bold}: ${found.A.bold}
                |
                |""".stripMargin)
      } else None
    }

    val allErrors = envMismatch :: errorMismatch /* :: returnMismatch */ :: Nil

    s"""
       |${"  ZIO Type Mismatch  ".red.bold.inverted}
       |
       |${allErrors.flatten.mkString("\n")}""".stripMargin
  }

  override def doReport(pos: Position, msg: String, severity: Severity): Unit =
    (severity, msg) match {
      case (Reporter.ERROR, IsZIOTypeMismatch(found, required)) =>
        val error = makeError(found, required)
        underlying.error(pos, error)
      case _ =>
        severity match {
          case Reporter.INFO    => underlying.echo(pos, msg)
          case Reporter.WARNING => underlying.warning(pos, msg)
          case Reporter.ERROR   => underlying.error(pos, msg)
        }
    }
}
