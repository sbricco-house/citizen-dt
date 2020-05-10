name := "citizen-system"

version := "0.1"

scalaVersion := "2.13.2"

val circeVersion = "0.7.0"
lazy val commonSetting = Seq(
  libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest" % "3.1.1" % "test",
    "io.circe"  %% "circe-core"     % circeVersion,
    "io.circe"  %% "circe-generic"  % circeVersion,
    "io.circe"  %% "circe-parser"   % circeVersion
  )
)

lazy val model = (project in file("model"))
  .settings(commonSetting)

lazy val citizen_service = (project in file("citizen"))
  .dependsOn(model)

lazy val permission_service = (project in file("permission"))
  .dependsOn(model)

lazy val authentication_service = (project in file("authentication"))
  .dependsOn(model)