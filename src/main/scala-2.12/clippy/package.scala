package object clippy {

  implicit class OptionExt(val self: Option.type) extends AnyVal {
    def when[A](cond: Boolean)(a: => A): Option[A] =
      if (cond) Some(a) else None
  }
}
