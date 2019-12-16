onLoad in Global := { s =>
  "dependencyUpdates" :: s
}

pgpPassphrase := Some(getEnvVar("PGP_PASSPHRASE").getOrElse("").toCharArray)
pgpPublicRing := file(s"$gpgFolder/pubring.gpg")
pgpSecretRing := file(s"$gpgFolder/secring.gpg")

lazy val root = project
  .in(file("."))
  .enablePlugins(SbtPlugin)
  .settings(commonSettings)
  .settings(moduleName := "sbt-compendium")
  .settings(sbtPlugin := true)

lazy val docs = project
  .in(file("docs"))
  .dependsOn(root)
  .settings(moduleName := "sbt-compendium-docs")
  .settings(commonSettings)
  .settings(noPublishSettings)
  .settings(tutSettings)
  .settings(
    micrositeSettings
  )
  .enablePlugins(MicrositesPlugin)
