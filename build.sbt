pgpPassphrase := Some(getEnvVar("PGP_PASSPHRASE").getOrElse("").toCharArray)
pgpPublicRing := file(s"$gpgFolder/pubring.asc")
pgpSecretRing := file(s"$gpgFolder/secring.asc")

lazy val root = project
  .in(file("."))
  .dependsOn(client, plugin, docs)
  .aggregate(client, plugin, docs)
  .settings(noPublishSettings: _*)
  .settings(moduleName := "sbt-compendium-root")

lazy val client = project
  .in(file("client"))
  .settings(clientSettings)
  .settings(moduleName := "sbt-compendium-client")

lazy val plugin = project
  .in(file("plugin"))
  .enablePlugins(SbtPlugin)
  .aggregate(client)
  .dependsOn(client)
  .settings(moduleName := "sbt-compendium")

lazy val docs = project
  .in(file("docs"))
  .dependsOn(plugin, client)
  .settings(moduleName := "sbt-compendium-docs")
  .settings(noPublishSettings)
  .settings(tutSettings)
  .settings(
    micrositeSettings
  )
  .enablePlugins(MicrositesPlugin)
