import cats.effect.IO
import cats.implicits._
import higherkindness.compendium.models.IdlName
import sbtcompendium.CompendiumUtils
import cats.syntax._

scalaVersion := "2.12.10"

version := "0.1"

compendiumProtocolIdentifiers := List("MyProtocol")
compendiumServerHost := "localhost"
compendiumServerPort := 8080

sourceGenerators in Compile += Def.task {
  compendiumGenClients.value
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

compendiumGenClients := {

  val generateProtocols = compendiumProtocolIdentifiers.value.toList.map { protocolId =>
    def targetFile(id: String) = (sbt.Keys.sourceManaged in Compile).value / "compendium" / s"$protocolId.scala"
    val generateProtocol       = CompendiumUtils.generateCodeFor(protocolId, targetFile, generateClient, IdlName.Avro)

    generateProtocol
  }

  val (_, generated) = generateProtocols.separate

  generated.flatten
}
