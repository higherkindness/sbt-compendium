import sbtcompendium.CompendiumPlugin

lazy val root = (project in file("."))
  .enablePlugins(CompendiumPlugin)
  .settings(
    scalaVersion := "2.12.4",
    version := "0.1",
    compProtocolIdentifiersPath := List("basicdomain"),
    compServerHost := "localhost",
    compServerPort := 8080,
    sourceGenerators in Compile += Def.task {
      compGenerateClients.value
    }.taskValue
  )
