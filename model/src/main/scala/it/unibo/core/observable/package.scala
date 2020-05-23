package it.unibo.core

import io.vertx.scala.core.http.WebSocketBase
import monix.reactive.Observable
import monix.reactive.subjects.PublishSubject

package object observable {

  implicit class RichObservable[R](observable : Observable[R]) {
    def fallback(cond : R => Boolean, action : R => Unit) : Observable[R] = {
      observable
        .filter(data => {
          val result = cond(data)
          if(!result) {
            action(data)
          }
          result
        })
    }
  }
  implicit class RichObservableOption[R](observable : Observable[Option[R]]) {
    def fallbackOption(action : => Unit) : Observable[Option[R]] = {
      observable.fallback(_.isDefined, _ => action)
    }
  }

  def observableFromWebSocket(webSocket : WebSocketBase) : Observable[String]  = {
    val source = PublishSubject[String]()
    webSocket.textMessageHandler(text => source.onNext(text))
    source
  }
}
