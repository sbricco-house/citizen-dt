package it.unibo.covid.data

import it.unibo.core.data.{GroupCategory, LeafCategory}

//TODO categories could be loaded from a JSON file
//TODO put a TTL
object Categories {
  //medical data
  val bodyTemperatureCategory = LeafCategory("bodyTemperature")
  val bloodOxygenCategory = LeafCategory("spo2")
  val heartbeatCategory = LeafCategory("heartbeat")
  val medicalRecordCategory  = LeafCategory("medicalRecord")
  val medicalDataCategory = GroupCategory("medicalData", Set(bodyTemperatureCategory, bloodOxygenCategory, medicalRecordCategory, heartbeatCategory))
  //personal data
  val nameCategory = LeafCategory("name")
  val surnameCategory = LeafCategory("surname")
  val birthdateCategory = LeafCategory("birthdate")
  val fiscalCodeCategory = LeafCategory("cf")
  val personalDataCategory = GroupCategory("personal", Set(nameCategory, surnameCategory, birthdateCategory, fiscalCodeCategory))
  //position
  val positionCategory = LeafCategory("position")
  val locationCategory = GroupCategory("location", Set(positionCategory))
}
