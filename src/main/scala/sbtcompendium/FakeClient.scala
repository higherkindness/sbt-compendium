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
import higherkindness.compendium.CompendiumClient
import higherkindness.compendium.models.{CompendiumConfig, Protocol, Target}

object DummyCompendiumClient {

  def apply(implicit clientConfig: CompendiumConfig): CompendiumClient = new CompendiumClient {

    override def storeProtocol(identifier: String, protocol: Protocol): IO[Int] = IO.pure(clientConfig.http.port)

    override def recoverProtocol(identifier: String): IO[Option[Protocol]] = IO.pure(Some(Protocol("identifier")))

    override def generateClient(target: Target, identifier: String): IO[String] =
      IO(
        s"""
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
      )

  }

}
