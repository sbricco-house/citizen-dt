package it.unibo.service.citizen

import it.unibo.core.data.{Data, DataCategory, Resource}
import it.unibo.core.microservice.coap._
import it.unibo.service.authentication.TokenIdentifier
import it.unibo.service.citizen.matcher.DataJsonMatcher
import org.eclipse.californium.core.{CoapClient, CoapResponse}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Millis, Seconds, Span}

import scala.concurrent.Promise
class ObservableCoapCitizenApi extends AnyFlatSpec with BeforeAndAfterEach with Matchers with ScalaFutures with DataJsonMatcher {
  implicit val defaultPatience: PatienceConfig = PatienceConfig(timeout = Span(5, Seconds), interval = Span(100, Millis))
  import ObservableCoapCitizenApi._

  "citizen microservice" should " support coap protocol" in {
    val coapClient = new CoapClient("localhost/citizen/50/state")
    assert(coapClient.ping())
  }

  "citizen resources " should "support state observing" in {
    val coapClient = createClientByCategory(Categories.medicalDataCategory)
    val observePromise = Promise[CoapResponse]()
    val handler : CoapResponse => Unit = data => observePromise.success(data)
    coapClient.observeWithToken(citizenToken.token, handler)
    whenReady(observePromise.future) {
      result => result.getResponseText shouldBe "{}"
    }
  }

  "citizen resources " should " should be notified via coap" in {
    val raw = CitizenMicroservices.parserRegistry.encode(heartbeatData).get.encode()
    val coapClient = createClientByCategory(Categories.heartBeatCategory)
    val observePromise = Promise[CoapResponse]()
    val handler : CoapResponse => Unit = data => if(!isEmpty(data)) { observePromise.success(data) }
    coapClient.observeWithTokenAndWait(citizenToken.token, handler)
    CitizenMicroservices.citizenService.updateState(citizenToken, Seq(heartbeatData))
    whenReady(observePromise.future) {
      result => result.getResponseText shouldBe raw
    }
  }

  "citizen resources " should " observed from different clients" in {
    val hearthBeatRaw = CitizenMicroservices.parserRegistry.encode(heartbeatData).get.encode()
    val bloodPressureRaw = CitizenMicroservices.parserRegistry.encode(bloodPressureData).get.encode()
    val hearthPromise = Promise[String]()
    val bloodPromise = Promise[String]()
    val handlerFactory : (Promise[String]) => (CoapResponse => Unit) = promise => data => {
      if(!isEmpty(data)) {
        promise.success(data.getResponseText)
      }
    }
    val heartbeatClient = createClientByCategory(Categories.bloodPressureCategory)
    val bloodClient = createClientByCategory(Categories.heartBeatCategory)

    bloodClient.observeWithTokenAndWait(citizenToken.token, handlerFactory(hearthPromise))
    heartbeatClient.observeWithTokenAndWait(citizenToken.token, handlerFactory(bloodPromise))

    CitizenMicroservices.citizenService.updateState(citizenToken, Seq(heartbeatData, bloodPressureData))

    whenReady(hearthPromise.future.zip(bloodPromise.future)) {
      result => {
        result._1 shouldBe hearthBeatRaw
        result._2 shouldBe bloodPressureRaw
      }
    }
  }

  "citizen resources " should " being notified from multiple update" in {
    val aData1 = Data("10", Resource("mi band 3"), Categories.heartBeatCategory, 10, 80)
    val aData2 = Data("11", Resource("apple watch"), Categories.heartBeatCategory, 11, 70)
    val aData3 = Data("12", Resource("unkown"), Categories.heartBeatCategory, 12, 75)

    val dataList = Set(aData1, aData2, aData3)
      .map(CitizenMicroservices.parserRegistry.encode)
      .map(_.get)
      .map(_.encode)
    val observePromise = Promise[Set[String]]()
    var elements = Set.empty[String]

    val handler : CoapResponse => Unit = data => {
      if(!isEmpty(data)) {
        elements += data.getResponseText
      }
      if(elements.size == 3) {
        observePromise.success(elements)
      }
    }
    val coapClient = createClientByCategory(Categories.heartBeatCategory)
    coapClient.observeWithTokenAndWait(citizenToken.token, handler)
    CitizenMicroservices.citizenService.updateState(citizenToken, Seq(aData1))
    CitizenMicroservices.citizenService.updateState(citizenToken, Seq(aData2))
    CitizenMicroservices.citizenService.updateState(citizenToken, Seq(aData3))

    whenReady(observePromise.future) {
      result => result shouldBe dataList
    }
  }
  "citizen resources " should " be robust from multiple update" in {
    val heartBeats = (0 to 50).map(x => Data(x + "", Resource("y"), Categories.heartBeatCategory, 10, 90)).toSet
    val bloodPressures = (0 to 50).map(x => Data(x + "", Resource("y"), Categories.bloodPressureCategory, 10, 90))

    val heartBeatsDecoded = heartBeats
      .map(CitizenMicroservices.parserRegistry.encode)
      .map(_.get)
      .map(_.encode)
    val observePromise = Promise[Set[String]]()
    var elements = Set.empty[String]

    val handler : CoapResponse => Unit = data => {
      if(!isEmpty(data)) {
        elements += data.getResponseText
      }
      if(elements.size == heartBeats.size) {
        observePromise.success(elements)
      }
    }
    val coapClient = createClientByCategory(Categories.heartBeatCategory)
    coapClient.observeWithTokenAndWait(citizenToken.token, handler)
    heartBeats.foreach(data => CitizenMicroservices.citizenService.updateState(citizenToken, Seq(data)))
    bloodPressures.foreach(data => CitizenMicroservices.citizenService.updateState(citizenToken, Seq(data)))

    whenReady(observePromise.future) {
      result => result shouldBe heartBeatsDecoded
    }
  }
  "citizen resources " should " NOT being observable from unkwon category" in {
    val aData = Data("10", Resource("bi"), Categories.heartBeatCategory, 10, 10)
    val raw = CitizenMicroservices.parserRegistry.encode(aData).get.encode()
    val coapClient = new CoapClient("""localhost/citizen/50/state?data_category=unkwon""")
    val result = coapClient.observeWithTokenAndWait("jwt1", (data : CoapResponse) => {})
    assert(result.isCanceled)
  }

  "citizen resources " should " NOT support state observing without token" in {
    val coapClient = createClientByCategory(Categories.medicalDataCategory)
    val observing = coapClient.observeAndWait((data : CoapResponse) => {})
    assert(observing.isCanceled)
  }

  "citizen resources " should " NOT being notified from other categories" in {
    val observePromise = Promise[CoapResponse]()
    val handler : CoapResponse => Unit = data => if(!isEmpty(data)) { observePromise.success(data) }
    val coapClient = createClientByCategory(Categories.bloodPressureCategory)
    coapClient.observeWithTokenAndWait(citizenToken.token, handler)
    CitizenMicroservices.citizenService.updateState(citizenToken, Seq(heartbeatData))
    Thread.sleep(1000)
    observePromise.isCompleted shouldBe false
  }

  //TODO add test on group category
  override def beforeEach(): Unit = {
    CoapBootstrap.boot()
  }

  override def afterEach(): Unit = {
    CoapBootstrap.teardown()
  }
}

object ObservableCoapCitizenApi {
  val citizenToken = TokenIdentifier("jwt1")
  val heartbeatData = Data("10", Resource("mi band 3"), Categories.heartBeatCategory, 100, 80)
  val bloodPressureData = Data("11", Resource("mi band 3"), Categories.bloodPressureCategory, 100, 32)

  def isEmpty(response : CoapResponse) : Boolean = response.getResponseText == "{}"
  def createClientByCategory(category : DataCategory) : CoapClient = {
    new CoapClient(s"coap://localhost/citizen/50/state?data_category=${category.name}")
  }
}
