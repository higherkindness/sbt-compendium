/*
 * Copyright 2019-2020 47 Degrees, LLC. <http://www.47deg.com>
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

package sbtcompendium.client

import avrohugger.Generator
import avrohugger.format.Standard
import cats.effect.Sync
import cats.free.Free
import cats.implicits._
import hammock._
import hammock.circe.implicits._
import higherkindness.compendium.models._

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
   * @param version    optional protocol version number
   * @return a protocol
   */
  def retrieveProtocol(identifier: String, version: Option[Int]): F[Option[Protocol]]

  /** Generates a client for a target and a protocol by its identifier
   *
   * @param target target for the protocol
   * @param identifier the protocol identifier
   * @return a client for that protocol and target
   */
  def generateClient(target: IdlName, identifier: String): F[List[String]]
}

object CompendiumClient {

  def apply[F[_]: Sync]()(
      implicit interp: InterpTrans[F],
      clientConfig: CompendiumClientConfig
  ): CompendiumClient[F] =
    new CompendiumClient[F] {

      val baseUrl: String          = s"http://${clientConfig.http.host}:${clientConfig.http.port}"
      val versionParamName: String = "version"

      override def storeProtocol(identifier: String, protocol: Protocol): F[Int] = {
        val request: Free[HttpF, HttpResponse] =
          Hammock.request(Method.POST, uri"$baseUrl/v0/protocol/$identifier", Map(), Some(protocol))

        for {
          status <- request.map(_.status).exec[F]
          _ <- status match {
            case Status.Created => F.unit
            case Status.OK      => F.unit
            case Status.BadRequest =>
              asError(request, SchemaError)
            case Status.InternalServerError =>
              Sync[F].raiseError(UnknownError(s"Error in compendium server"))
            case _ =>
              Sync[F].raiseError(UnknownError(s"Unknown error with status code $status"))
          }
        } yield status.code
      }

      override def retrieveProtocol(
          identifier: String,
          version: Option[Int]
      ): F[Option[Protocol]] = {
        val versionParam = version.fold("")(v => s"?$versionParamName=${v.show}")
        val uri          = uri"$baseUrl/v0/protocol/$identifier$versionParam"

        val request: Free[HttpF, HttpResponse] = Hammock.request(Method.GET, uri, Map())

        for {
          status <- request.map(_.status).exec[F]
          out <- status match {
            case Status.OK       => request.as[Protocol].map(Option(_)).exec[F]
            case Status.NotFound => F.pure(None)
            case Status.InternalServerError =>
              F.raiseError(UnknownError(s"Error in compendium server"))
            case _ =>
              F.raiseError(UnknownError(s"Unknown error with status code $status"))
          }
        } yield out
      }

      override def generateClient(target: IdlName, identifier: String): F[List[String]] =
        target match {
          case IdlName.Avro =>
            retrieveProtocol(identifier, None)
              .map(_.map(r => handleAvro(r.raw)).getOrElse(List.empty))
          // case IdlName.Protobuf =>
          //   retrieveProtocol(identifier, None)
          //     .map(_.map(r => handleAvro(r.raw).mkString("\n")).getOrElse(""))
          case _ =>
            F.raiseError(UnknownError(s"Unknown error with status code 501. Schema format not implemented yet"))
        }

      private def handleAvro(raw: String): List[String] =
        Generator(Standard).stringToStrings(raw)

      private def asError(request: Free[HttpF, HttpResponse], error: String => Exception): F[Unit] =
        request
          .as[ErrorResponse]
          .exec[F]
          .flatMap(rsp => F.raiseError(error(rsp.message)))
    }
}
