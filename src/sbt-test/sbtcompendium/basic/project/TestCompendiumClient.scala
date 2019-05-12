import higherkindness.compendium.CompendiumClient
import higherkindness.compendium.models.{Protocol, Target}
import cats.effect.IO

object TestCompendiumClient {

  def apply(): CompendiumClient = new CompendiumClient {

    override def storeProtocol(identifier: String, protocol: Protocol): IO[Int] = IO.never

    override def recoverProtocol(identifier: String): IO[Option[Protocol]] = IO.never

    override def generateClient(target: Target, identifier: String): IO[String] =
      IO(
        s"""
          package higherkindness.compendium.storage

          object TestFile extends App {
            println("Hey")
          }
        """.stripMargin
      )
  }
}