package it.unibo.macros.util

import scala.language.experimental.macros // enable travis CI to use macro compile options
import scala.reflect.macros.whitebox.{Context => Whitebox}
object MacroUtils {
  def getTypeOrNone[T](value : Any) : Option[Any] = macro getTypeOrNoneImpl[T]
  def getTypeOrNoneImpl[T : c.WeakTypeTag](c: Whitebox)(value : c.Expr[Any]): c.Expr[Option[Any]] = {
    import c.universe._
    val code =
      q"""${value}.asInstanceOf[Any] match {
        case x : ${weakTypeTag[T].tpe} => Some(x.asInstanceOf[Any])
        case _ => None
      }"""
    c.Expr[Option[Any]](code)
  }
}
