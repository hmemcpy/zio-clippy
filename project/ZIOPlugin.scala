package clippy

import sbt._
import sbt.Keys._

object ZIOPlugin extends AutoPlugin {

  object autoImport {
    val zioPluginJar = taskKey[File]("Location of the ZIO Plugin jar.")
  }

  override def trigger = allRequirements
  def afterScala2_13_12(scalaVersion: VersionNumber): Boolean =
    scalaVersion.matchesSemVer(SemanticSelector(">=2.13.12"))

  import autoImport._
  override lazy val projectSettings: Seq[Setting[_]] = Seq(
    zioPluginJar := Def.taskDyn {
      val Some((major, minor)) = CrossVersion.partialVersion(scalaVersion.value)
      val versionNumber        = VersionNumber(scalaVersion.value)
      val version = (major, minor) match {
        case (2, 13) if afterScala2_13_12(versionNumber) => scalaVersion.value
        case _                                           => scalaBinaryVersion.value
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
