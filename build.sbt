name := "citizen-system"

version := "0.1"

scalaVersion := "2.13.2"

lazy val commonSetting = Seq(
  libraryDependencies += "org.scalatest" %% "scalatest" % "3.1.1" % "test"
)

lazy val model = (project in file("model"))
  .settings(commonSetting)

lazy val citizen_service = (project in file("citizen"))
  .dependsOn(model)

lazy val permission_service = (project in file("permission"))
  .dependsOn(model)

lazy val authentication_service = (project in file("authentication"))
  .dependsOn(model)