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

package sbtcompendium

import cats.effect.IO
import cats.implicits._
import hammock.asynchttpclient.AsyncHttpClientInterpreter
import higherkindness.compendium.models._
import higherkindness.compendium.models.config.HttpConfig
import sbt._

object CompendiumPlugin extends AutoPlugin {

  override def trigger: PluginTrigger = allRequirements

  object autoImport {

    import sbtcompendium.client.{CompendiumClient, CompendiumClientConfig}
    lazy val compendiumGenClients: TaskKey[Seq[File]]   = taskKey[Seq[File]]("Generate all the clients for each protocol")
    lazy val compendiumServerHost: SettingKey[String]   = settingKey[String]("Url of the compendium server")
    lazy val compendiumServerPort: SettingKey[Int]      = settingKey[Int]("Port of the compendium server")
    lazy val compendiumFormatSchema: SettingKey[String] = settingKey[String]("Schema type to download")
    lazy val compendiumProtocolIdentifiers: SettingKey[Seq[String]] =
      settingKey[Seq[String]]("Protocol identifiers to be retrieved from compendium server")

    def client(host: String, port: Int): CompendiumClient[IO] = {
      implicit val interpreter                          = AsyncHttpClientInterpreter.instance[cats.effect.IO]
      implicit val clientConfig: CompendiumClientConfig = CompendiumClientConfig(HttpConfig(host, port))
      CompendiumClient[cats.effect.IO]()
    }

    def fromStringToIdlName(sch: String) = sch match {
      case "proto"   => IdlName.Protobuf
      case "scala"   => IdlName.Scala
      case "mu"      => IdlName.Mu
      case "openapi" => IdlName.OpenApi
      case _         => IdlName.Avro
    }
  }

  import autoImport._

  lazy val defaultSettings = Seq(
    compendiumProtocolIdentifiers := Nil,
    compendiumServerHost := "localhost",
    compendiumServerPort := 47047,
    compendiumFormatSchema := "avro",
    compendiumGenClients := {
      val log = sbt.Keys.streams.value.log

      val generateProtocols = compendiumProtocolIdentifiers.value.toList.map {
        protocolId =>
          def targetFile(id: String) =
            (sbt.Keys.sourceManaged in Compile).value / "compendium" / s"$protocolId$id.scala"
          val generateProtocol = CompendiumUtils.generateCodeFor(
            protocolId,
            targetFile,
            client(compendiumServerHost.value, compendiumServerPort.value).generateClient,
            fromStringToIdlName(compendiumFormatSchema.value)
          )
          log.info(s"Attempting to generate client for [${protocolId}]")
          log.debug("Resulting classes: " + generateProtocol.toOption.get.mkString("\n"))
          generateProtocol
      }

      val (failures, generated) = generateProtocols.separate

      failures.foreach {
        case (id, ProtocolNotFound(_)) => log.error(s"Protocol [${id}] not found in Compendium server")
        case (id, ex)                  => log.error(s"Unable to generate client for [${id}], unknown error [${ex.getMessage}]")
      }

      generated.flatten
    }
  )

  override def projectSettings: Seq[Setting[_]] = defaultSettings
}
