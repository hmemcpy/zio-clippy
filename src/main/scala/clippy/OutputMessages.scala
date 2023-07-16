package clippy

import clippy.ansi.AnsiStringOps
import clippy.utils.Info

abstract class OutputMessages(val envTitle: String, val errorTitle: String, val returnTitle: String) {
  def format(found: Info, required: Info): List[String] = {
    val (rDiff, eDiff, aDiff) = found.diff(required)

    def envMismatch =
      Option.when(rDiff.nonEmpty) {
        s"""$envTitle
           |
           |${rDiff.map(r => s"${"❯ ".red}${r.bold}").toList.mkString("\n")}
           |""".stripMargin
      }

    def errorMismatch =
      Option.when(eDiff) {
        s"""$errorTitle
           |
           |${"❯ ".red}${"Required"}: ${required.E.bold}
           |${"❯ ".red}${"Found   "}: ${found.E.bold}
           |""".stripMargin
      }

    def returnMismatch =
      Option.when(aDiff) {
        s"""$returnTitle
           |
           |${"❯ ".red}${"Required"}: ${required.A.bold}
           |${"❯ ".red}${"Found   "}: ${found.A.bold}
           |""".stripMargin
      }

    (envMismatch :: errorMismatch :: returnMismatch :: Nil).flatten
  }
}

object OutputMessages {

  val mismatchMessages = new OutputMessages(
    envTitle = "Your effect requires the following environment, but it was not provided:",
    errorTitle = "Your effect has an error type mismatch:",
    returnTitle = "Your effect has a return type mismatch:"
  ) {}

  val overridingMessages = new OutputMessages(
    envTitle =
      "Your effect requires the following environment in overriding, but it was not presented in original type:",
    errorTitle = "Your effect has an error type mismatch in overriding:",
    returnTitle = "Your effect has a return type mismatch:"
  ) {}

  val cannotProveMessages = new OutputMessages(
    envTitle = """Your effect requires:
                 |
                 |%s
                 |
                 |but you're providing it with an environment:
                 |
                 |%s
                 |""".stripMargin,
    "",
    ""
  ) {
    override def format(found: Info, required: Info): List[String] = {
      val foundStr = s"${found.R.map(r => s"${"❯ ".red}${r.bold}").toList.mkString("\n")}"
      val reqStr   = s"${required.R.map(r => s"${"❯ ".red}${r.bold}").toList.mkString("\n")}"

      envTitle.format(reqStr, foundStr) :: Nil
    }
  }
}
