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
import sbtcompendium.client.{CompendiumClient, CompendiumClientConfig}

final case class ProtocolAndVersion(name: String, version: Option[String])

object CompendiumPlugin extends AutoPlugin {

  override def trigger: PluginTrigger = allRequirements

  object autoImport {

    lazy val compendiumSrcGenClients: TaskKey[Seq[File]] =
      taskKey[Seq[File]]("Generate all the clients for each protocol")
    lazy val compendiumSrcGenServerHost: SettingKey[String]    = settingKey[String]("Url of the compendium server")
    lazy val compendiumSrcGenServerPort: SettingKey[Int]       = settingKey[Int]("Port of the compendium server")
    lazy val compendiumSrcGenFormatSchema: SettingKey[IdlName] = settingKey[IdlName]("Schema type to download")
    lazy val compendiumSrcGenProtocolIdentifiers: SettingKey[Seq[ProtocolAndVersion]] =
      settingKey[Seq[ProtocolAndVersion]]("Protocol identifiers to be retrieved from compendium server")

    def client(host: String, port: Int): CompendiumClient[IO] = {
      implicit val interpreter                          = AsyncHttpClientInterpreter.instance[cats.effect.IO]
      implicit val clientConfig: CompendiumClientConfig = CompendiumClientConfig(HttpConfig(host, port))
      CompendiumClient[cats.effect.IO]()
    }

  }

  import autoImport._

  lazy val defaultSettings = Seq(
    compendiumSrcGenProtocolIdentifiers := Nil,
    compendiumSrcGenServerHost := "localhost",
    compendiumSrcGenServerPort := 47047,
    compendiumSrcGenFormatSchema := IdlName.Avro,
    compendiumSrcGenClients := {

      val log = sbt.Keys.streams.value.log

      lazy val genClient: (IdlName, String, Option[String]) => IO[List[String]] =
        client(compendiumSrcGenServerHost.value, compendiumSrcGenServerPort.value).generateClient

      val generateProtocols = compendiumSrcGenProtocolIdentifiers.value.toList.map {
        protocolId =>
          def targetFile(id: String): File =
            (sbt.Keys.sourceManaged in Compile).value / "compendium" / s"${protocolId.name}$id.scala"
          val generateProtocol = CompendiumUtils.generateCodeFor(
            protocolId,
            targetFile,
            genClient,
            compendiumSrcGenFormatSchema.value
          )

          log.info(
            if (protocolId.version.isEmpty) s"Attempting to generate client for [${protocolId.name}] last version"
            else s"Attempting to generate client for [${protocolId.name}] with version [${protocolId.version}]"
          )
          log.debug(
            "Resulting classes: " + generateProtocol.toOption.map(_.mkString("\n")).getOrElse("No class generated")
          )
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
