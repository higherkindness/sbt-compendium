import cats.effect.IO
import cats.implicits._
import sbtcompendium.models._
import sbtcompendium._

scalaVersion := "2.12.10"

version := "0.1"

compendiumSrcGenProtocolIdentifiers := List(ProtocolAndVersion("MyProtocol", None))
compendiumSrcGenServerHost := "localhost"
compendiumSrcGenServerPort := 8080

sourceGenerators in Compile += Def.task {
  compendiumSrcGenClients.value
}.taskValue

def generateClient(
    target: IdlName,
    identifier: String,
    v: Option[String],
    schemaConfig: Option[SchemaConfig]
): IO[List[String]] =
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
    def targetFile(id: String) = (sbt.Keys.sourceManaged in Compile).value / "compendium" / s"${protocolId.name}.scala"
    val generateProtocol       = CompendiumUtils.generateCodeFor(protocolId, targetFile, generateClient, IdlName.Avro, None)

    generateProtocol
  }

  val (_, generated) = generateProtocols.separate

  generated.flatten
}
