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

import sbt._
import hammock.asynchttpclient.AsyncHttpClientInterpreter
import higherkindness.compendium.CompendiumClient
import higherkindness.compendium.models.config.{CompendiumClientConfig, HttpConfig}
import cats.implicits._
import higherkindness.compendium.models.ProtocolNotFound

object CompendiumPlugin extends AutoPlugin with CompendiumUtils {

  object autoImport extends CompendiumKeys

  import CompendiumPlugin.autoImport._

  private val compendiumClient: SettingKey[CompendiumClient] =
    settingKey[CompendiumClient]("default implementation for the compendium client")

  lazy val defaultSettings = Seq(
    compendiumProtocolIdentifiers := Nil,
    compendiumServerHost := "localhost",
    compendiumServerPort := 47047,
    compendiumClient := {
      implicit val interpreter = AsyncHttpClientInterpreter.instance[cats.effect.IO]
      implicit val clientConfig: CompendiumClientConfig = CompendiumClientConfig(
        HttpConfig(
          compendiumServerHost.value,
          compendiumServerPort.value
        ))
      CompendiumClient()
    },
    compendiumGenClients := {
      val log = sbt.Keys.streams.value.log

      val generateProtocols = compendiumProtocolIdentifiers.value.toList.map { protocolId =>
        val targetFile       = (sbt.Keys.sourceManaged in Compile).value / "compendium" / s"$protocolId.scala"
        val generateProtocol = generateCodeFor(protocolId, targetFile, compendiumClient.value)

        log.info(s"Attempting to generate client for [${protocolId}]")
        generateProtocol.attempt.map(_.leftMap((protocolId, _))).unsafeRunSync()
      }

      val (failures, generated) = generateProtocols.separate

      failures.foreach {
        case (id, ProtocolNotFound(_)) => log.error(s"Protocol [${id}] not found in Compendium server")
        case (id, ex)                  => log.error(s"Unable to generate client for [${id}], unknown error [${ex.getMessage}]")
      }

      generated
    }
  )

  override val projectSettings: Seq[Setting[_]] = defaultSettings
}
