package it.unibo.covid.demo

/**
 * start multiple demo.
 * the only parameter is "howMany" i.e, how many demos starts with this main run.
 * each citizen dt has:
 *  ith id, 10000 + ith http port, 20000 + ith coap port.
 * e.g the 10th user, has id = 11, httpPort = 10011 and coapPort = 20011
 */
object MultiMockDemo extends App {
  val howMany = args.headOption.map(_.toInt).getOrElse(5)
  val httpStartPort = 10000
  val coapStartPort = 20000
  (0 until howMany)
    .map(index => Array(s"$index", s"${httpStartPort + index}", s"${coapStartPort + index}"))
    .foreach(MockDemo.main)
}
