package it.unibo.service.citizen

import java.net.NetworkInterface

import it.unibo.core.data.{Data, Resource}
import it.unibo.core.microservice.coap._
import it.unibo.service.citizen.CoapScope._
import it.unibo.service.citizen.matcher.DataJsonMatcher
import org.eclipse.californium.core.coap.CoAP.ResponseCode
import org.eclipse.californium.core.{CoapClient, CoapResponse}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}

import scala.concurrent.Promise
class ObservableCoapCitizenApi extends AnyFlatSpec with BeforeAndAfterEach with BeforeAndAfterAll with Matchers with ScalaFutures with DataJsonMatcher {
  implicit val defaultPatience: PatienceConfig = PatienceConfig(timeout = Span(25, Seconds), interval = Span(100, Millis))
  import ObservableCoapCitizenApi._
  "citizen microservice" should " support coap protocol" in {
    val coapClient = new CoapClient(s"localhost:${CoapScope.currentPort}/citizen/50/state")
    assert(coapClient.ping())
    coapClient.shutdown()
  }

  "citizen resources " should "support state observing" in {
    val coapClient = createClientByCategory(Categories.medicalDataCategory)
    val observePromise = Promise[CoapResponse]()
    val handler : CoapResponse => Unit = data => {
      if(!observePromise.isCompleted) {
        observePromise.success(data)
      }
    }
    val relation = coapClient.observeWithToken(CITIZEN_TOKEN.token, handler)
    whenReady(observePromise.future) {
      result => result.getResponseText shouldBe "{}"
    }
    relation.proactiveCancel()
    coapClient.shutdown()
  }

  "citizen resources " should " should be notified via coap" in {
    val raw = CitizenMicroservices.parserRegistry.encode(heartbeatData).get.encode()
    val coapClient = createClientByCategory(Categories.heartBeatCategory)
    val (observePromise, relation) = installExpectedOne(coapClient)
    CitizenMicroservices.citizenService.updateState(CITIZEN_TOKEN, Seq(heartbeatData)).future
    whenReady(observePromise.future) {
      result => result shouldBe raw
    }
    relation.proactiveCancel()
    coapClient.shutdown()
  }

  "citizen resources " should " observed from different clients" in {
    val hearthBeatRaw = CitizenMicroservices.parserRegistry.encode(heartbeatData).get.encode()
    val bloodPressureRaw = CitizenMicroservices.parserRegistry.encode(bloodPressureData).get.encode()

    val heartbeatClient = createClientByCategory(Categories.heartBeatCategory)
    val bloodClient = createClientByCategory(Categories.bloodPressureCategory)
    val (hearthPromise, hearthRelation) = installExpectedOne(heartbeatClient)
    val (bloodPromise, bloodRelation) = installExpectedOne(bloodClient)

    CitizenMicroservices.citizenService.updateState(CITIZEN_TOKEN, Seq(heartbeatData, bloodPressureData))

    whenReady(hearthPromise.future.zip(bloodPromise.future)) {
      result => {
        result._1 shouldBe hearthBeatRaw
        result._2 shouldBe bloodPressureRaw
      }
    }
    Seq(heartbeatClient, bloodClient).foreach(_.shutdown())
    heartbeatClient.shutdown()
    bloodClient.shutdown()
    hearthRelation.proactiveCancel()
    bloodRelation.proactiveCancel()
  }

  "citizen resources " should " being notified from multiple update" in {
    val aData1 = Data("10", Resource("mi band 3"), Categories.heartBeatCategory, 10, 80)
    val aData2 = Data("11", Resource("apple watch"), Categories.heartBeatCategory, 11, 70)
    val aData3 = Data("12", Resource("unkown"), Categories.heartBeatCategory, 12, 75)

    val dataList = Set(aData1, aData2, aData3)
      .map(CitizenMicroservices.parserRegistry.encode)
      .map(_.get)
      .map(_.encode)
    val coapClient = createClientByCategory(Categories.heartBeatCategory)
    val (observePromise, relation) = installExpectedMany(coapClient, dataList.size)
    CitizenMicroservices.citizenService.updateState(CITIZEN_TOKEN, Seq(aData1))
    CitizenMicroservices.citizenService.updateState(CITIZEN_TOKEN, Seq(aData2))
    CitizenMicroservices.citizenService.updateState(CITIZEN_TOKEN, Seq(aData3))

    whenReady(observePromise.future) {
      result => result shouldBe dataList
    }
    coapClient.shutdown()
    relation.proactiveCancel()
  }

  "citizen resources" should " be observed via group category" in {
    val hearthBeatRaw = CitizenMicroservices.parserRegistry.encode(heartbeatData).get.encode()
    val bloodPressureRaw = CitizenMicroservices.parserRegistry.encode(bloodPressureData).get.encode()
    val coapClient = createClientByCategory(Categories.medicalDataCategory)
    val (observePromise, relation) = installExpectedMany(coapClient, 2)
    CitizenMicroservices.citizenService.updateState(CITIZEN_TOKEN, Seq(heartbeatData, bloodPressureData))
    whenReady(observePromise.future) {
      result => result shouldBe Set(hearthBeatRaw, bloodPressureRaw)
    }
    coapClient.shutdown()
    relation.proactiveCancel()
  }
  "citizen resources " should " be robust from multiple update" in {
    val heartBeats = (0 to 500).map(x => Data(x + "", Resource("y"), Categories.heartBeatCategory, 10, 90)).toSet
    val bloodPressures = (0 to 500).map(x => Data(x + "", Resource("y"), Categories.bloodPressureCategory, 10, 90))

    val heartBeatsDecoded = heartBeats
      .map(CitizenMicroservices.parserRegistry.encode)
      .map(_.get)
      .map(_.encode)

    val coapClient = createClientByCategory(Categories.heartBeatCategory)
    val (observePromise, relation) = installExpectedMany(coapClient, heartBeatsDecoded.size)
    val futures = (heartBeats ++ bloodPressures)
      .map(data => CitizenMicroservices.citizenService.updateState(CITIZEN_TOKEN, Seq(data)))
      .map(_.future)
    futures.foreach(future => whenReady(future) { _ => {}})

    whenReady(observePromise.future) {
      result => result shouldBe heartBeatsDecoded
    }
    coapClient.shutdown()
    relation.proactiveCancel()
  }

  "citizen resources " should " be robust from multiple update in multiple client" in {
    val heartBeats = (0 to 50)
      .map(x => Data(x + "", Resource("y"), Categories.heartBeatCategory, 10, 90))
      .toSet
    val bloodPressures = (0 to 50)
      .map(x => Data(x + "", Resource("y"), Categories.bloodPressureCategory, 10, 90))
      .toSet

    val heartBeatsDecoded = heartBeats
      .map(CitizenMicroservices.parserRegistry.encode)
      .map(_.get)
      .map(_.encode)

    val bloodPressuresDecoded = bloodPressures
      .map(CitizenMicroservices.parserRegistry.encode)
      .map(_.get)
      .map(_.encode)

    val allDecoded : Set[String] = heartBeatsDecoded ++ bloodPressuresDecoded

    val hearthBeatClients = (0 to 5).map(_ => createClientByCategory(Categories.heartBeatCategory)).map(installExpectedMany(_, heartBeatsDecoded.size))
    val bloodPressureClients = (0 to 5).map(_ => createClientByCategory(Categories.bloodPressureCategory)).map(installExpectedMany(_, bloodPressuresDecoded.size))
    val medialClients = (0 to 5).map(_ => createClientByCategory(Categories.medicalDataCategory)).map(installExpectedMany(_, allDecoded.size))

    val hearthBeatFutures = hearthBeatClients
      .map(_._1.future)
    val bloodPressureFutures = bloodPressureClients
      .map(_._1.future)
    val medialClientFutures = medialClients
      .map(_._1.future)

    val futures = (heartBeats ++ bloodPressures)
      .map(data => CitizenMicroservices.citizenService.updateState(CITIZEN_TOKEN, Seq(data)))
      .map(_.future)

    futures.foreach(whenReady(_) { _ => {}})
    hearthBeatFutures.foreach(whenReady(_) { result => result shouldBe heartBeatsDecoded})
    bloodPressureFutures.foreach(whenReady(_) { result => result shouldBe bloodPressuresDecoded})
    medialClientFutures.foreach(whenReady(_) { result => result shouldBe allDecoded})
    (hearthBeatClients ++ bloodPressureClients ++ medialClients).foreach(_._2.proactiveCancel())
  }
  "citizen resources " should " NOT being observable from unkwon category" in {
    val aData = Data("10", Resource("bi"), Categories.heartBeatCategory, 10, 10)
    val coapClient = new CoapClient(s"""localhost:${CoapScope.currentPort}/citizen/50/state?data_category=unkwon""")
    val result = coapClient.observeWithTokenAndWait("jwt1", (data : CoapResponse) => {})
    assert(result.isCanceled)
    coapClient.shutdown()
  }

  "citizen resources " should " NOT support state observing without token" in {
    val coapClient = createClientByCategory(Categories.medicalDataCategory)
    val observing = coapClient.observeAndWait((data : CoapResponse) => {})
    assert(observing.isCanceled)
    coapClient.shutdown()
  }

  "citizen resources " should " NOT being notified from other categories" in {
    val coapClient = createClientByCategory(Categories.bloodPressureCategory)
    val (observePromise, relation) = installExpectedOne(coapClient)
    CitizenMicroservices.citizenService.updateState(CITIZEN_TOKEN, Seq(heartbeatData))
    Thread.sleep(1000)
    observePromise.isCompleted shouldBe false
    coapClient.shutdown()
    relation.proactiveCancel()
  }

  "citizen resources with coap" should " NOT support get without observe" in {
    val coapClient = createClientByCategory(Categories.bloodPressureCategory)
    coapClient.getWithToken(CITIZEN_TOKEN.token).getCode shouldBe ResponseCode.METHOD_NOT_ALLOWED
    coapClient.shutdown()
  }

  override def beforeAll(): Unit = {
    CitizenMicroservices.refresh()
    CoapScope.boot()
  }
  override def afterAll(): Unit = {
    CoapScope.teardown()
  }
}

object ObservableCoapCitizenApi {
  val heartbeatData = Data("10", Resource("mi band 3"), Categories.heartBeatCategory, 100, 80)
  val bloodPressureData = Data("11", Resource("mi band 3"), Categories.bloodPressureCategory, 100, 32)
}
