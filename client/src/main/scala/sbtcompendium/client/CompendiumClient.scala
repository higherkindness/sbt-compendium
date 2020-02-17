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

import java.io.{File, PrintWriter}

import avrohugger.Generator
import avrohugger.format.Standard
import cats.effect.{IO, Sync}
import cats.free.Free
import cats.implicits._
import hammock._
import hammock.circe.implicits._
import sbtcompendium.models._

import scala.util.Try

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
  def generateClient(target: IdlName, identifier: String, v: Option[String]): F[List[String]]
}

object CompendiumClient {

  final case class FilePrintWriter(file: File, pw: PrintWriter)

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
              F.raiseError(UnknownError(s"Error in compendium server"))
            case _ =>
              F.raiseError(UnknownError(s"Unknown error with status code $status"))
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

      override def generateClient(target: IdlName, identifier: String, v: Option[String]): F[List[String]] =
        target match {
          case IdlName.Avro =>
            for {
              protocol <- retrieveProtocol(identifier, safeInt(v))
              code     <- handleAvro(protocol)
            } yield code
          case IdlName.Protobuf =>
            for {
              protocol <- retrieveProtocol(identifier, safeInt(v))
              code     <- handleProto(protocol)
            } yield code
          case IdlName.OpenApi =>
            for {
              protocol <- retrieveProtocol(identifier, safeInt(v))
              code     <- handleOpenApi(protocol)
            } yield code
          case _ =>
            UnknownError(s"Unknown error with status code 501. Schema format not implemented yet")
              .raiseError[F, List[String]]
        }

      private def handleAvro(op: Option[Protocol]): F[List[String]] =
        op.map(p => F.delay(Generator(Standard).stringToStrings(p.raw))).getOrElse(F.pure(Nil))

      private def handleProto(op: Option[Protocol]): F[List[String]] =
        F.delay(op.map(p => ProtoGenerator.getCode[IO](p.raw).unsafeRunSync).getOrElse(Nil))

      private def handleOpenApi(op: Option[Protocol]): F[List[String]] =
        F.delay(op.map(p => OpenApiGenerator.getCode[IO](p.raw).unsafeRunSync).getOrElse(Nil))

      private def safeInt(s: Option[String]): Option[Int] = s.flatMap(str => Try(str.toInt).toOption)

      private def asError(request: Free[HttpF, HttpResponse], error: String => Exception): F[Unit] =
        request
          .as[ErrorResponse]
          .exec[F]
          .flatMap(rsp => F.raiseError(error(rsp.message)))
    }
}
