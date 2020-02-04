import cats.effect.IO
import cats.implicits._
import higherkindness.compendium.models.IdlName
import sbtcompendium.CompendiumUtils

scalaVersion := "2.12.10"

version := "0.1"

compendiumSrcGenProtocolIdentifiers := List("MyProtocol")
compendiumSrcGenServerHost := "localhost"
compendiumSrcGenServerPort := 8080

sourceGenerators in Compile += Def.task {
  compendiumSrcGenClients.value
}.taskValue

def generateClient(target: IdlName, identifier: String): IO[List[String]] =
  IO(
    List("""
    package higherkindness.compendium.storage
    object TestFile extends App {
      println("Hey")
    }
    """.stripMargin)
  )

compendiumSrcGenClients := {

  val generateProtocols = compendiumSrcGenProtocolIdentifiers.value.toList.map { protocolId =>
    def targetFile(id: String) = (sbt.Keys.sourceManaged in Compile).value / "compendium" / s"$protocolId.scala"
    val generateProtocol       = CompendiumUtils.generateCodeFor(protocolId, targetFile, generateClient, IdlName.Avro)

    generateProtocol
  }

  val (_, generated) = generateProtocols.separate

  generated.flatten
}
