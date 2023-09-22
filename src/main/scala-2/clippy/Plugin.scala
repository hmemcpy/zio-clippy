package clippy

import scala.tools.nsc._
import scala.tools.nsc.plugins.PluginComponent
final class Plugin(override val global: Global) extends plugins.Plugin {
  override val description: String = "Better error reporting for ZIO"
  override val name: String        = "clippy"

  private val knobs = Map(
    "show-original-error" -> "Shows the original Scala type mismatch error",
    "additional-types"    -> "Additional types (fully-qualified) to consider when extracting type mismatches"
  )

  override val optionsHelp: Option[String] = Some(
    knobs.map { case (key, help) =>
      s"  -P:$name:$key".padTo(31, ' ') ++ help
    }
      .mkString(System.lineSeparator)
  )

  override def init(options: List[String], error: String => Unit): Boolean = {
    val (known, unknown) = options.partition(s => knobs.keys.exists(s.startsWith))
    if (unknown.nonEmpty) {
      global.reporter.echo(s"ZIO Clippy - Unknown options: ${unknown.mkString(", ")}")
    }

    val showOriginalError = known.contains("show-original-error")
    val additionalTypes = options.find(_.contains("additional-types")).map(extractAdditionalTypes).getOrElse(Nil)

    global.reporter = new ZIOErrorReporter(global.settings, global.reporter, showOriginalError, additionalTypes)

    true
  }

  private def extractAdditionalTypes(s: String): List[String] =
    s.split(":")
      .tail
      .flatMap(_.split(","))
      .map(_.trim)
      .toList

  override val components: List[PluginComponent] = Nil
}
