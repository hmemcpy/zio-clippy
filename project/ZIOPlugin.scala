package clippy

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
      val Some((major, minor)) = CrossVersion.partialVersion(scalaVersion.value)
      val version = major match {
        case 2 => if (scalaVersion.value >= "2.13.12") scalaVersion.value else scalaBinaryVersion.value
        case _ => scalaBinaryVersion.value
      }
      val jar = file(s"""${sys.props("user.home")}/.cache/zio/lib/$version/zio-clippy.jar""")
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
