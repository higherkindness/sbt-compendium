version := "1.0"

lazy val root = (project in file("."))
  .settings(
    version := "0.1",
    scalaVersion := "2.12.8",
    assemblyJarName in assembly := "test.jar"
  )
