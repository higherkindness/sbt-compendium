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

import java.io.File

import cats.effect.IO
trait CompendiumUtils {

  def storeProtocol(identifier: String, basePath: ClientInfo => File, client: CompendiumClient[IO]): IO[File] =
    for {
      info <- client.generateClient(identifier)
      file <- IO(basePath(info))
      _    <- IO(sbt.io.IO.write(file, info.text))
    } yield file

}
