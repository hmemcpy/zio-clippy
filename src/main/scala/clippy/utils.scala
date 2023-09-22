package clippy

import clippy.OutputMessages.{cannotProveMessages, mismatchMessages, overridingMessages}
import clippy.ansi.AnsiStringOps

import scala.util.matching.Regex
object utils {
  val any    = "Any"
  val anySet = Set(any)

  implicit final class AnySyntax[A](private val a: A) extends AnyVal {
    def |>[B](f: A => B): B = f(a)
  }

  sealed trait ErrorKind extends Product with Serializable
  object ErrorKind {
    case object Overriding   extends ErrorKind
    case object TypeMismatch extends ErrorKind
    case object CannotProve  extends ErrorKind
  }

  case class Info private (R: Set[String], E: String, A: String) {
    self =>

    def diff(other: Info) =
      (
        self.R -- other.R -- anySet,
        self.E != other.E && other.E != any,
        self.A != other.A
      )
  }

  def makeError(found: Info, required: Info, msg: String, errorKind: ErrorKind, showOriginalError: Boolean): String = {
    val outputMessages = errorKind match {
      case ErrorKind.Overriding   => overridingMessages
      case ErrorKind.TypeMismatch => mismatchMessages
      case ErrorKind.CannotProve  => cannotProveMessages
    }

    val messages = outputMessages.format(found, required)

    val originalMessage =
      Option.when(showOriginalError) {
        s"""|${"-" * 80}
            |$msg
            |""".stripMargin
      }

    val allMessages = messages ++ originalMessage :: Nil

    s"""|
        |${"  ZIO Type Mismatch  ".red.bold.inverted}
        |
        |${allMessages.flatten.mkString("\n")}
        |""".stripMargin
  }

  object Info {
    val any    = "Any"
    val anySet = Set(any)

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
        R = r.split(" with ").map(_.trim |> normalize).toSet,
        E = e.trim,
        A = a.trim
      )

    def unapply(m: Regex.Match): Option[Info] =
      (m.group(1), Option(m.group(2)), Option(m.group(3)), Option(m.group(4)), Option(m.group(5))) match {
        case ("UIO" | "ULayer", _, _, _, Some(a))         => Some(from("Any", "Nothing", a))
        case ("URIO" | "URLayer", _, Some(r), _, Some(a)) => Some(from(r, "Nothing", a))
        case ("Task", _, _, _, Some(a))                   => Some(from("Any", "Throwable", a))
        case ("RIO", Some(r), _, _, Some(a))              => Some(from(r, "Throwable", a))
        case (_, _, Some(r), Some(e), Some(a))            => Some(from(r, e, a))
        case _                                            => None
      }
  }

  class IsZIOTypeErrorExtractor(additionalTypes: List[String]) {
    val brackets = raw"\[(.+),([^,\]]+),([^,\]]+)]"
    val default  = raw"zio\.(ZIO|ZLayer)"
    val matches  = buildRegex
    private def buildRegex: Regex = {
      val allRegex = default :: additionalTypes.map(_.replace(".", "\\."))
      (allRegex.mkString("(", "|", ")") + brackets).r
    }

    private def findMismatches(msg: String, kind: ErrorKind): Option[(ErrorKind, Info, Info)] =
      matches.findAllMatchIn(msg).toList match {
        case _ :: Info(found) :: _ :: Info(required) :: Nil => Some((kind, found, required))
        case Info(found) :: Info(required) :: _             => Some((kind, found, required))
        case _                                              => None
      }

    val errorKinds = Map(
      "type mismatch;"                   -> ErrorKind.TypeMismatch,
      "incompatible type in overriding;" -> ErrorKind.Overriding
      // "polymorphic expression cannot be instantiated to expected type;" -> ErrorKind.Polymorphic
    )

    def unapply(msg: String): Option[(ErrorKind, Info, Info)] = {
      val kind = errorKinds.collectFirst { case (k, v) if msg.contains(k) => v }
      kind.flatMap(findMismatches(msg, _))
    }
  }

  object IsCannotProveMismatch {
    val provide = raw"Cannot prove that (.+) <:< (.+)\.".r

    def unapply(msg: String): Option[(Info, Info)] =
      msg match {
        case provide(found, required) => Some((Info.from(found, "", ""), Info.from(required, "", "")))
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
