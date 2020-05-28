package it.unibo.service.citizen

import org.eclipse.californium.core.CoapServer

object CoapBootstrap {
  var server : CoapServer = _
  def boot() : Unit = {
    server = CoapObservableApi(CitizenMicroservices.citizenService, CitizenMicroservices.parserRegistry)
    server.start()
  }

  def teardown() : Unit = {
    server.destroy()
  }
}
