package clippy

import utils.Info
import zio.Scope
import zio.test._

object InfoSpec extends ZIOSpecDefault {
  override def spec: Spec[TestEnvironment with Scope, Any] = suiteAll("error rendering") {
    test("R diff") {
      val found = Info.from("zio.Has[persistence.Persistence] with zio.Has[zio.console.Console.Service]", "", "")
      val req = Info.from(
        "zio.Has[zio.random.Random.Service] with zio.Has[zio.clock.Clock.Service] with zio.Has[zio.system.System.Service] with zio.Has[zio.blocking.Blocking.Service] with zio.Has[zio.console.Console.Service]",
        "",
        ""
      )

      val (rDiff, _, _) = found.diff(req)

      assertTrue(rDiff == Set("zio.Has[persistence.Persistence]"))
    }
  }
}
