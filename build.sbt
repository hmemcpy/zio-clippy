import xerial.sbt.Sonatype._

inThisBuild(
  Seq(
    organization := "com.hmemcpy",
    homepage     := Some(url("https://github.com/hmemcpy/zio-clippy")),
    licenses     := License.Apache2 :: Nil,
    developers := List(
      Developer(
        "hmemcpy",
        "Igal Tabachnik",
        "hmemcpy@gmail.com",
        url("https://hmemcpy.com")
      )
    ),
    crossScalaVersions := List(
      "2.13.10",
      "2.12.15", // the version of scala used by sbt 1.6.2
      "2.12.16", // the version of scala used by sbt 1.7.2
      "2.12.17", // the version of scala used by sbt 1.8.2
      "3.2.2"
    ),
    scalaVersion := (ThisBuild / crossScalaVersions).value.head,
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio-test" % "2.0.10" % Test
    )
  )
)

val install = taskKey[Unit]("Install the ZIO Clippy compiler plugin.")

lazy val root = (project in file(".")).settings(
  name := "zio-clippy",
  Compile / unmanagedSourceDirectories ++= {
    val dir                  = (Compile / scalaSource).value
    val Some((major, minor)) = CrossVersion.partialVersion(scalaVersion.value)
    val specific =
      if (major == 2 && minor <= 12) file(s"${dir.getPath}-2.12") :: Nil
      else Nil

    file(s"${dir.getPath}-$major") :: specific
  },
  libraryDependencies ++= {
    if (scalaVersion.value.startsWith("3."))
      Seq(
        "org.scala-lang" % "scala3-compiler_3" % scalaVersion.value % "provided"
      )
    else
      Seq(
        "org.scala-lang" % "scala-compiler" % scalaVersion.value % "provided",
        "org.scala-lang" % "scala-reflect"  % scalaVersion.value % "provided"
      )
  },
  crossTarget := target.value / s"scala-${scalaVersion.value}",
  assembly / assemblyOption ~= {
    _.withIncludeScala(false)
  },
  assembly / assemblyJarName := "zio-clippy.jar",
  install := {
    streams.value.log.info(s"Installing ${zioPluginJar.value}")
    IO.write(zioPluginJar.value, IO.readBytes(assembly.value))

    val plugin = file(s"""${sys.props("user.home")}/.sbt/1.0/plugins/ZIOPlugin.scala""")
    streams.value.log.info(s"Installing $plugin")
    IO.copyFile(file("project/ZIOPlugin.scala"), plugin)
  },
  sonatypeCredentialHost := "s01.oss.sonatype.org",
  sonatypeRepository     := "https://s01.oss.sonatype.org/service/local",
  sonatypeProjectHosting := Some(GitHubHosting("hmemcpy", "zio-clippy", "Igal Tabachnik", "hmemcpy@gmail.com"))
)
