name := "citizen-system"

version := "0.1"

val scalaBase = "2.12.1"

val circeVersion = "0.7.0"
val vertxVersion = "3.9.0"
val monixVersion = "3.2.1"

lazy val coreSetting = Seq(
  scalaVersion := scalaBase,
  resolvers += Resolver.sonatypeRepo("releases"),
  addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)
)

lazy val commonSetting = Seq(
  parallelExecution in ThisBuild := false,
  libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest" % "3.1.1" % "test",
    "io.monix" %% "monix-reactive" % monixVersion,
    "io.vertx" %% "vertx-lang-scala" % vertxVersion,
    "io.vertx" %% "vertx-web-scala" % vertxVersion,
    "io.vertx" %% "vertx-web-client-scala" % vertxVersion,
    "org.eclipse.californium" % "californium-core" % "2.2.3"
  )
) ++ coreSetting

lazy val macros = (project in file("macros"))
  .settings(coreSetting,
   libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaBase
  )

lazy val testSetting = Seq(
  libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest" % "3.1.1" % "test"
  )
)

lazy val authenticationSettings = Seq(
  libraryDependencies ++= Seq(
    "io.vertx" %% "vertx-auth-jwt-scala" % vertxVersion
  )
)


lazy val model = (project in file("model"))
  .dependsOn(macros)
  .settings(commonSetting)

lazy val citizen_service = (project in file("citizen"))
  .dependsOn(macros, model, permission_service, authentication_service)
  .settings(testSetting)

lazy val permission_service = (project in file("permission"))
  .dependsOn(macros, model)

lazy val authentication_service = (project in file("authentication"))
  .dependsOn(macros, model)
  .settings(authenticationSettings, testSetting)