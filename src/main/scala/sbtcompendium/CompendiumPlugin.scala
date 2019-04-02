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

import cats.effect.IO
import hammock.asynchttpclient.AsyncHttpClientInterpreter
import sbt._
import cats.implicits._

object CompendiumPlugin extends AutoPlugin with CompendiumUtils {

  object autoImport extends CompendiumKeys

  import CompendiumPlugin.autoImport._

  lazy val defaultSettings = Seq(
    compProtocolIdentifiersPath := Nil,
    compGenerateClients := {

      val client: CompendiumClient[IO] = {
        implicit val interpreter = new AsyncHttpClientInterpreter[IO]
        implicit val clientConfig: CompendiumConfig = CompendiumConfig(
          HttpConfig(
            "localhost",
            8080
            //compServerHost.value,
            //compServerPort.value
          ))
        CompendiumClient[IO]
      }

      compProtocolIdentifiersPath.value.map(storeProtocol(_, client)).foldLeft(IO.unit)(_ *> _).unsafeRunSync()
    }
  )

  override val projectSettings: Seq[Setting[_]] = defaultSettings
}
