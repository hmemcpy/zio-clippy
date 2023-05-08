package zio

import scala.util.matching.Regex

object utils {
  case class Info private (R: Set[String], E: String, A: String)

  object Info {
    // The original error message will needlessly dealias nested definitions.
    private val substitutions = Map(
      "[+A]zio.ZIO[Any,Throwable,A]" -> "zio.Task"
    )

    private def normalize(s: String) =
      substitutions.foldLeft(s) { case (str, (k, v)) =>
        str.replace(k, v)
      }

    def from(r: String, e: String, a: String) =
      Info(
        R = r.split(raw" with ").map(normalize).toSet,
        E = e,
        A = a
      )

    def unapply(m: Regex.Match): Option[Info] =
      (m.group(1), Option(m.group(2)), Option(m.group(3)), Option(m.group(4))) match {
        case ("ZIO" | "ZLayer", Some(r), Some(e), Some(a)) => Some(from(r, e, a))
        case ("UIO" | "ULayer", _, _, Some(a))             => Some(from("Any", "Nothing", a))
        case ("URIO" | "URLayer", Some(r), _, Some(a))     => Some(from(r, "Nothing", a))
        case ("Task", _, _, Some(a))                       => Some(from("Any", "Throwable", a))
        case ("RIO", Some(r), _, Some(a))                  => Some(from(r, "Throwable", a))
        case _                                             => None
      }
  }

  object IsZIOTypeMismatch {
    val mismatch = raw"zio\.(ZIO|ZLayer)\[(.+),([^,\]]+),(.+)\]".r
    def unapply(msg: String): Option[(Info, Info)] =
      if (msg.contains("type mismatch;")) {
        mismatch.findAllMatchIn(msg).toList match {
          case _ :: Info(found) :: _ :: Info(required) :: Nil => Some(found, required)
          case Info(found) :: Info(required) :: _             => Some(found, required)
          case _                                              => None
        }
      } else None
  }

  object IsCannotProveMismatch {
    val provide = raw"Cannot prove that (.+) <:< (.+)\.".r
    def unapply(msg: String): Option[(Info, Info)] =
      msg match {
        case provide(found, required) => Some(Info.from(found, "", ""), Info.from(required, "", ""))
        case _                        => None
      }
  }

  def padString(longer: String, shorter: String): String = {
    val lengthDiff = longer.length - shorter.length
    if (lengthDiff <= 0) {
      shorter
    } else {
      val spaces = " " * (lengthDiff / 2)
      spaces + shorter + spaces
    }
  }

  def stripFqn(s: String) = s.split("\\.").last
}
