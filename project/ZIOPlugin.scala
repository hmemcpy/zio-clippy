package zio

import sbt.*
import sbt.Keys.*

object ZIOPlugin extends AutoPlugin {

  object autoImport {
    val zioPluginJar = taskKey[File]("Location of the ZIO Plugin jar.")
  }

  override def trigger = allRequirements

  import autoImport.*
  override lazy val projectSettings: Seq[Setting[?]] = Seq(
    zioPluginJar := Def.taskDyn {
      val jar = file(s"""${sys.props("user.home")}/.cache/zio/lib/${scalaBinaryVersion.value}/zio-clippy.jar""")
      if (jar.isFile) Def.task {
        jar
      }
      else
        Def.task {
          streams.value.log.warn(
            s"ZIO Plugin not found. Try\n\n      sbt ++${scalaVersion.value}! install\n\nin the ZIO Plugin repo."
          )
          jar
        }
    }.value,
    scalacOptions ++= {
      val jar = zioPluginJar.value
      if (jar.isFile) Seq(s"-Xplugin:${jar.getAbsolutePath}")
      else Seq()
    },
    clean := {
      val _ = clean.value
      file(s"""${sys.props("user.home")}/.cache/zio${target.value}""").delete(): Unit
    }
  )
}
