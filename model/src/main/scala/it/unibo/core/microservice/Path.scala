package it.unibo.core.microservice

sealed trait PathCell {}

case class Relative(path : String) extends PathCell
case class Parameter(str: String) extends PathCell

object Path {
  implicit class RichPath(path: List[PathCell]) {
    def :/(pathString: String): List[PathCell] = path :+ Relative(pathString)

    def :?(pathString: String): List[PathCell] = path :+ Parameter(pathString)
  }
  val root = List()
}
