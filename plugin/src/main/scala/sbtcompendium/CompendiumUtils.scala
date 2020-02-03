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

import java.io.File

import cats.syntax.either._
import cats.effect.IO
import higherkindness.compendium.models._

object CompendiumUtils {

  def generateCodeFor(
      identifier: String,
      path: String => File,
      f: (IdlName, String) => IO[List[String]],
      format: IdlName
  ): Either[(String, Throwable), List[File]] =
    f(format, identifier)
      .map(
        _.zipWithIndex
          .map {
            case (str, id) =>
              val p = path("_class" + id.toString)
              sbt.io.IO.write(p, str)
              p
          }
      )
      .attempt
      .map(_.leftMap((identifier, _)))
      .unsafeRunSync()
}
