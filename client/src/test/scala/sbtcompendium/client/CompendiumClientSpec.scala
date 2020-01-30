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

import cats.effect.{IO, Sync}
import cats.~>
import hammock.{HttpF, HttpRequest, InterpTrans, Post}
import higherkindness.compendium.models._
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification
import pureconfig.generic.auto._
import hammock._
import io.circe.syntax._
import io.circe.Encoder

object CompendiumClientSpec extends Specification with ScalaCheck {

  sequential

  private[this] val dummyProtocol: Protocol = Protocol("rawProtocol")

  implicit val clientConfig: CompendiumClientConfig =
    pureconfig.ConfigSource.default.at("compendium").loadOrThrow[CompendiumClientConfig]

  private[this] def asEntityJson[T: Encoder](t: T): Entity =
    Entity.StringEntity(t.asJson.toString, ContentType.`application/json`)

  def interp[F[_]: Sync](identifier: String, target: IdlName, version: Option[Int] = None): InterpTrans[F] =
    new InterpTrans[F] {

      val trans: HttpF ~> F = new (HttpF ~> F) {
        private def response(entity: Entity): HttpResponse =
          HttpResponse(Status.OK, Map(), entity)

        def apply[A](req: HttpF[A]): F[A] = req match {
          case Get(HttpRequest(uri, _, _)) if uri.path.equalsIgnoreCase(s"/v0/protocol/$identifier") =>
            F.catchNonFatal {
              response(asEntityJson(dummyProtocol))
            }

          case Get(HttpRequest(uri, _, _))
              if uri.path.equalsIgnoreCase(s"/v0/protocol/$identifier?version=${version.getOrElse(0)}") =>
            F.catchNonFatal {
              response(asEntityJson(dummyProtocol))
            }

          case Get(HttpRequest(uri, _, _)) if uri.path.equalsIgnoreCase(s"/v0/protocol/error") =>
            F.catchNonFatal {
              response(Entity.EmptyEntity).copy(status = Status.InternalServerError)
            }

          case Get(HttpRequest(uri, _, _))
              if uri.path.equalsIgnoreCase(s"/v0/protocol/$identifier/generate?target=${target.toString}") =>
            F.catchNonFatal {
              response(Entity.StringEntity(uri.path)).copy(status = Status.NotImplemented)
            }

          case Get(_) =>
            F.catchNonFatal {
              response(Entity.EmptyEntity).copy(status = Status.NotFound)
            }

          case Post(HttpRequest(uri, _, _)) if uri.path.equalsIgnoreCase(s"/v0/protocol/schemaerror") =>
            F.catchNonFatal {
              response(asEntityJson(ErrorResponse("Schema error"))).copy(status = Status.BadRequest)
            }

          case Post(HttpRequest(uri, _, _)) if uri.path.equalsIgnoreCase(s"/v0/protocol/alreadyexists") =>
            F.catchNonFatal {
              response(Entity.StringEntity(uri.path)).copy(status = Status.OK)
            }

          case Post(HttpRequest(uri, _, _)) if uri.path.equalsIgnoreCase(s"/v0/protocol/internal") =>
            F.catchNonFatal {
              response(Entity.EmptyEntity).copy(status = Status.InternalServerError)
            }

          case Post(HttpRequest(uri, _, _)) =>
            F.catchNonFatal {
              response(Entity.StringEntity(uri.path)).copy(status = Status.Created)
            }

          case _ =>
            F.raiseError(new Exception("Unexpected HTTP Method"))
        }
      }
    }

  "Retrieve protocol" >> {
    "Given a valid identifier returns a protocol" >> {

      implicit val terp: InterpTrans[IO] = interp[IO]("proto1", IdlName.Scala)

      CompendiumClient[IO]().retrieveProtocol("proto1", None).unsafeRunSync() should beSome(dummyProtocol)
    }

    "Given a valid identifier with version returns a protocol" >> {
      val version = Option(1)

      implicit val terp: InterpTrans[IO] = interp[IO]("proto1", IdlName.Scala, version)

      CompendiumClient[IO]().retrieveProtocol("proto1", version).unsafeRunSync() should beSome(dummyProtocol)
    }

    "Given an invalid identifier returns no protocol" >> {

      implicit val terp: InterpTrans[IO] = interp[IO]("proto1", IdlName.Scala)

      CompendiumClient[IO]().retrieveProtocol("proto2", None).unsafeRunSync() should beNone
    }

    "Given an identifier returns a internal server error" >> {

      implicit val terp: InterpTrans[IO] = interp[IO]("proto1", IdlName.Scala)

      CompendiumClient[IO]()
        .retrieveProtocol("error", None)
        .unsafeRunSync() must throwA[higherkindness.compendium.models.UnknownError]
    }
  }

  "Store protocol" >> {
    "Given a valid identifier and a correct protocol returns no error" >> {

      implicit val terp: InterpTrans[IO] = interp[IO]("proto1", IdlName.Scala)

      CompendiumClient[IO]().storeProtocol("proto1", dummyProtocol).unsafeRunSync() must not(throwA[Exception])
    }

    "Given a valid identifier and an incorrect protocol returns a SchemaError" >> {

      implicit val terp: InterpTrans[IO] = interp[IO]("proto1", IdlName.Scala)

      CompendiumClient[IO]()
        .storeProtocol("schemaerror", dummyProtocol)
        .unsafeRunSync() must throwA[higherkindness.compendium.models.SchemaError]
    }

    "Given a valid identifier and a protocol that already exists returns no error" >> {

      implicit val terp: InterpTrans[IO] = interp[IO]("proto1", IdlName.Scala)

      CompendiumClient[IO]()
        .storeProtocol("alreadyexists", dummyProtocol)
        .unsafeRunSync() must not(throwA[Exception])
    }

    "Given a valid identifier and a protocol that returs a InternalServerError returns a UnknownError" >> {

      implicit val terp: InterpTrans[IO] = interp[IO]("proto1", IdlName.Scala)

      CompendiumClient[IO]()
        .storeProtocol("internal", dummyProtocol)
        .unsafeRunSync() must throwA[higherkindness.compendium.models.UnknownError]
    }
  }

  "Generate client" >> {
    "Given a valid identifier and a valid target" >> {
      failure
    }.pendingUntilFixed
  }

}
