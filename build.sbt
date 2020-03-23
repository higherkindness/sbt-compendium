pgpPassphrase := Some(getEnvVar("PGP_PASSPHRASE").getOrElse("").toCharArray)

lazy val plugin = project
  .in(file("plugin"))
  .enablePlugins(SbtPlugin)
  .aggregate(client)
  .dependsOn(client)
  .settings(moduleName := "sbt-compendium")

lazy val client = project
  .in(file("client"))
  .settings(clientSettings)
  .settings(moduleName := "sbt-compendium-client")
