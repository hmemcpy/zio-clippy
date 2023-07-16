package clippy

import zio.Scope
import zio.test._
object RegexSpec extends ZIOSpecDefault {
  override def spec: Spec[TestEnvironment with Scope, Any] = suiteAll("type mismatch") {
    test("parses the type mismatch error") {
      val regex =
        raw"""type mismatch;
             | found   : zio.URIO[zio.Has[persistence.Persistence] with zio.console.Console with zio.console.Console,zio.ExitCode]
             |    (which expands to)  zio.ZIO[zio.Has[persistence.Persistence] with zio.Has[zio.console.Console.Service] with zio.Has[zio.console.Console.Service],Nothing,zio.ExitCode]
             | required: zio.URIO[zio.ZEnv,zio.ExitCode]
             |    (which expands to)  zio.ZIO[zio.Has[zio.clock.Clock.Service] with zio.Has[zio.console.Console.Service] with zio.Has[zio.system.System.Service] with zio.Has[zio.random.Random.Service] with zio.Has[zio.blocking.Blocking.Service],Nothing,zio.ExitCode]""".stripMargin

      val (found, required) = regex match {
        case IsZIOTypeError(_, found, required) => (found, required)
      }

      assertTrue(
        found.R == Set("zio.Has[persistence.Persistence]", "zio.Has[zio.console.Console.Service]"),
        required.R == Set(
          "zio.Has[zio.random.Random.Service]",
          "zio.Has[zio.clock.Clock.Service]",
          "zio.Has[zio.system.System.Service]",
          "zio.Has[zio.blocking.Blocking.Service]",
          "zio.Has[zio.console.Console.Service]"
        )
      )
    }

    test("parses the return type") {
      val regex =
        raw"""type mismatch;
             | found   : zio.ZIO[io.github.gaelrenoux.tranzactio.doobie.Connection with R,E,A]
             |    (which expands to)  zio.ZIO[zio.Has[doobie.util.transactor.Transactor[zio.Task]] with R,E,A]
             | required: zio.ZIO[io.github.gaelrenoux.tranzactio.doobie.Connection with R,Throwable,A]
             |    (which expands to)  zio.ZIO[zio.Has[doobie.util.transactor.Transactor[zio.Task]] with R,Throwable,A]""".stripMargin

      val (found, required) = regex match {
        case IsZIOTypeError(_, found, required) => (found, required)
      }

      assertTrue(found.A == "A", required.A == "A")
    }
  }
}
