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

import cats.syntax.either._
import cats.effect.IO
import sbtcompendium.models._
import java.io.File

object CompendiumUtils {

  def generateCodeFor(
      identifier: ProtocolAndVersion,
      path: String => File,
      f: (IdlName, String, Option[String], Option[SchemaConfig]) => IO[List[String]],
      format: IdlName,
      config: Option[SchemaConfig]
  ): Either[(String, Throwable), List[File]] =
    f(format, identifier.name, identifier.version, config)
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
      .map(_.leftMap((identifier.name, _)))
      .unsafeRunSync()

}
