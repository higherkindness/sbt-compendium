/*
 * Copyright 2018-2019 47 Degrees, LLC. <http://www.47deg.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sbtcompendium

import cats.effect.Sync
import hammock.InterpTrans
import enumeratum._

final case class Protocol(raw: String)
final case class HttpConfig(host: String, port: Int)
final case class CompendiumConfig(http: HttpConfig)
final case class ClientInfo(fileName: String, extension: String, path: String, text: String)

sealed trait Target extends EnumEntry

object Target extends Enum[Target] {

  val values = findValues

  case object Scala extends Target
}

trait CompendiumClient[F[_]] {

  /** Stores a protocol
   *
   * @param identifier the protocol identifier
   * @param protocol a protocol
   * @return the identifier of the protocol
   */
  def storeProtocol(identifier: String, protocol: Protocol): F[Int]

  /** Retrieve a Protocol by its id
   *
   * @param identifier the protocol identifier
   * @return a protocol
   */
  def recoverProtocol(identifier: String): F[Option[Protocol]]

  /** Generates a client for a target and a protocol by its identifier
   *
   * @param identifier the protocol identifier
   * @return a client for that protocol and target
   */
  def generateClient(identifier: String): F[ClientInfo]
}

object CompendiumClient {

  def apply[F[_]](implicit F: CompendiumClient[F]): CompendiumClient[F] = F

  //@SuppressWarnings("unused")
  implicit def impl[F[_]: Sync: InterpTrans](implicit clientConfig: CompendiumConfig): CompendiumClient[F] = {

    new CompendiumClient[F] {

      override def storeProtocol(identifier: String, protocol: Protocol): F[Int] = Sync[F].pure(clientConfig.http.port)

      override def recoverProtocol(identifier: String): F[Option[Protocol]] = Sync[F].pure(Some(Protocol("identifier")))

      override def generateClient(identifier: String): F[ClientInfo] =
        Sync[F].pure(
          ClientInfo(
            "TestFile",
            "scala",
            "src/main/scala/higherkindness/compendium/storage",
            """
          package higherkindness.compendium.storage

          import java.io.{BufferedWriter, File, FileWriter}

          class TestFile {

            def storeFileTest(path: String, text: String): Unit = {
              val file = new File(path)
              val bw = new BufferedWriter(new FileWriter(file))
              bw.write(text)
              bw.close()
            }
          }
        """.stripMargin
          ))
    }
  }
}
