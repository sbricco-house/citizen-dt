package it.unibo.service.citizen

import it.unibo.core.data.{Data, Resource}
import it.unibo.service.citizen.matcher.DataJsonMatcher
import org.eclipse.californium.core.{CoapClient, CoapResponse}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Millis, Seconds, Span}
import it.unibo.core.microservice.coap._
import it.unibo.service.authentication.TokenIdentifier

import scala.concurrent.Promise
class ObservableCoapCitizenApi extends AnyFlatSpec with BeforeAndAfterAll with Matchers with ScalaFutures with DataJsonMatcher {
  implicit val defaultPatience: PatienceConfig = PatienceConfig(timeout = Span(5, Seconds), interval = Span(100, Millis))

  "citizen microservice" should " support coap protocol" in {
    val coapClient = new CoapClient("localhost/citizen/50/state")
    assert(coapClient.ping())
  }

  "citizen resources " should " support state observing" in {
    val coapClient = new CoapClient("""localhost/citizen/50/state?data_category=medical""")
    //TODO Add constant
    val observePromise = Promise[CoapResponse]()
    val handler : CoapResponse => Unit = data => observePromise.success(data)
    val observing = coapClient.observeWithToken("jwt1", handler)
    whenReady(observePromise.future) {
      result => result.getResponseText shouldBe "{}"
    }
  }

  "citizen resources " should " should be notified via coap" in {
    val aData = Data("10", Resource("bi"), Categories.hearBeatCategory, 10, 10)
    val raw = CitizenMicroservices.parserRegistry.encode(aData).get.encode()
    val coapClient = new CoapClient("""localhost/citizen/50/state?data_category=heartbeat""")
    //TODO Add constant
    val observePromise = Promise[CoapResponse]()
    val handler : CoapResponse => Unit = data => {
      if(data.getResponseText != "{}") {
        observePromise.success(data)
      }
    }
    coapClient.observeWithTokenAndWait("jwt1", handler)
    CitizenMicroservices.citizenService.updateState(TokenIdentifier("jwt1"), Seq(aData))
    whenReady(observePromise.future) {
      result => result.getResponseText shouldBe raw
    }
  }

  "citizen resources " should " NOT support state observing without token" in {
    val coapClient = new CoapClient("""localhost/citizen/50/state?data_category=medical""")
    val observing = coapClient.observeAndWait((data : CoapResponse) => {})
    assert(observing.isCanceled)
  }

  "citizen resources " should " NOT being observable from unkwon category" in {
    val aData = Data("10", Resource("bi"), Categories.hearBeatCategory, 10, 10)
    val raw = CitizenMicroservices.parserRegistry.encode(aData).get.encode()
    val coapClient = new CoapClient("""localhost/citizen/50/state?data_category=unkwon""")
    val result = coapClient.observeWithTokenAndWait("jwt1", (data : CoapResponse) => {})
    assert(result.isCanceled)
  }

  "citizen resources " should " NOT being notified from other categories" in {
    val aData = Data("10", Resource("bi"), Categories.hearBeatCategory, 10, 10)
    val raw = CitizenMicroservices.parserRegistry.encode(aData).get.encode()
    val observePromise = Promise[CoapResponse]()
    val handler : CoapResponse => Unit = data => {
      if(data.getResponseText != "{}") {
        observePromise.success(data)
      }
    }
    val coapClient = new CoapClient("""localhost/citizen/50/state?data_category=blood_pressure""")
    coapClient.observeWithTokenAndWait("jwt1", (data : CoapResponse) => {})
    Thread.sleep(1000)
    assert(! observePromise.isCompleted)
  }
  "citizen resources " should " observed from different clients" in {
    val aData = Data("10", Resource("bi"), Categories.hearBeatCategory, 10, 10)
    val anotherData = Data("10", Resource("bi"), Categories.bloodPressureCategory, 10, 10)
    val raw = CitizenMicroservices.parserRegistry.encode(aData).get.encode()
    val otherRaw = CitizenMicroservices.parserRegistry.encode(anotherData).get.encode()
    val firstPromise = Promise[String]()
    val secondPromise = Promise[String]()
    val handlerFactory : (Promise[String]) => (CoapResponse => Unit) = promise => data => {
      if(data.getResponseText != "{}") {
        promise.success(data.getResponseText)
      }
    }
    val coapClient = new CoapClient("""localhost/citizen/50/state?data_category=blood_pressure""")
    val otherClient = new CoapClient("""localhost/citizen/50/state?data_category=heartbeat""")

    otherClient.observeWithTokenAndWait("jwt1", handlerFactory(firstPromise))
    coapClient.observeWithTokenAndWait("jwt1", handlerFactory(secondPromise))

    CitizenMicroservices.citizenService.updateState(TokenIdentifier("jwt1"), Seq(aData, anotherData))

    whenReady(firstPromise.future.zip(secondPromise.future)) {
      result => {
        result._1 shouldBe raw
        result._2 shouldBe otherRaw
      }
    }
  }
  //TODO add test on group category

  "citizen resources " should " being notified from multiple update" in {
    val aData1 = Data("10", Resource("bi"), Categories.hearBeatCategory, 10, 10)
    val aData2 = Data("11", Resource("bi1"), Categories.hearBeatCategory, 11, 11)
    val aData3 = Data("12", Resource("bi2"), Categories.hearBeatCategory, 12, 12)

    val dataList = Set(aData1, aData2, aData3)
      .map(CitizenMicroservices.parserRegistry.encode)
      .map(_.get)
      .map(_.encode)
    val observePromise = Promise[Set[String]]()
    var elements = Set.empty[String]

    val handler : CoapResponse => Unit = data => {
      if(data.getResponseText != "{}") {
        elements += data.getResponseText
      }
      if(elements.size == 3) {
        observePromise.success(elements)
      }
    }
    val coapClient = new CoapClient("""localhost/citizen/50/state?data_category=heartbeat""")
    coapClient.observeWithTokenAndWait("jwt1", handler)
    CitizenMicroservices.citizenService.updateState(TokenIdentifier("jwt1"), Seq(aData1))
    CitizenMicroservices.citizenService.updateState(TokenIdentifier("jwt1"), Seq(aData2))
    CitizenMicroservices.citizenService.updateState(TokenIdentifier("jwt1"), Seq(aData3))

    whenReady(observePromise.future) {
      result => result shouldBe dataList
    }
  }
  override def beforeAll(): Unit = {
    CoapBootstrap.boot()
  }

  override def afterAll(): Unit = {
    CoapBootstrap.teardown()
  }
}
