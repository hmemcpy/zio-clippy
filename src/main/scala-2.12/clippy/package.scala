package object clippy {

  implicit class OptionExt(val self: Option.type) extends AnyVal {
    def when[A](cond: Boolean)(a: => A): Option[A] =
      if (cond) Some(a) else None
  }
}

package scala.jdk {
  object CollectionConverters{

    // copied from Scala 2.13 because I don't want to deal with imports
    implicit class EnumerationHasAsScala[A](e: java.util.Enumeration[A]) {
      def asScala: Iterator[A] =  _root_.scala.collection.JavaConverters.enumerationAsScalaIterator(e)
    }
  }
}


