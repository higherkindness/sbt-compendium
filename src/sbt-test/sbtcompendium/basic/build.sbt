import sbtcompendium.CompendiumPlugin

lazy val root = (project in file("."))
  .enablePlugins(CompendiumPlugin)
  .settings(
    scalaVersion := "2.12.4",
    version := "0.1"
  )