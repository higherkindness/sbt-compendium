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

import cats.effect.{Resource, Sync}
import cats.implicits._
import higherkindness.skeuomorph.openapi.{print, schema, JsonSchemaF}
import java.io.{File, PrintWriter}
import java.nio.file.{Path, Paths}

import higherkindness.skeuomorph.Parser
import higherkindness.skeuomorph.openapi.client.http4s.print.impl
import higherkindness.skeuomorph.openapi.client.print.interfaceDefinition
import higherkindness.skeuomorph.openapi.print.{model, PackageName}
import sbtcompendium.client.CompendiumClient.FilePrintWriter

import scala.collection.JavaConverters._
import higherkindness.skeuomorph.openapi._
import schema.OpenApi
import ParseOpenApi._
import print._
import client.print._
import client.http4s.circe._
import client.http4s.print._
import higherkindness.skeuomorph.openapi.JsonSchemaF.Fixed

object OpenApiGenerator {

  implicit val http4sSpecifics = client.http4s.print.v20.v20Http4sSpecifics

  def getCode[F[_]](raw: String)(implicit F: Sync[F]): F[List[String]] =
    writeTempFile(raw, ".json")
      .flatMap(_ =>
        parseOpenApi(raw)
          .map { openApi =>
            val pkg = packageName(Paths.get("compendium"))
            List(
              List(
                s"package ${pkg.value}",
                model[JsonSchemaF.Fixed].print(openApi),
                interfaceDefinition.print(openApi)
              ).filter(_.nonEmpty).mkString("\n\n"),
              impl.print(pkg -> openApi)
            ).filter(_.nonEmpty)
          }
      )

  private def packageName(path: Path): PackageName =
    PackageName(path.iterator.asScala.map(_.toString).mkString("."))

  private def parseOpenApi[F[_]: Sync](raw: String): F[OpenApi[Fixed]] =
      writeTempFile(raw, extension.json).flatMap(tmpFile =>
                  Parser[F, JsonSource, OpenApi[JsonSchemaF.Fixed]].parse(JsonSource(tmpFile.file))
        
  //TODO: support yaml
   /* raw match {
      case json if json.startsWith("{") => {
        writeTempFile(raw, extension.json).flatMap(tmpFile =>
          Parser[F, JsonSource, OpenApi[JsonSchemaF.Fixed]].parse(JsonSource(tmpFile.file))
        )
      }
      case _ => {
        writeTempFile(raw, extension.yaml).flatMap(tmpFile =>
          Parser[F, YamlSource, OpenApi[JsonSchemaF.Fixed]].parse(YamlSource(tmpFile.file))
        )
      }
    }*/

  private def writeTempFile[F[_]: Sync](msg: String, extension: String): F[FilePrintWriter] =
    Resource
      .make(F.delay {
        val tmpDir = System.getProperty("java.io.tmpdir")
        val file   = new File(tmpDir + s"/compendium$extension")
        file.deleteOnExit()
        FilePrintWriter(file, new PrintWriter(file))
      }) { fpw: FilePrintWriter => F.delay(fpw.pw.close()) }
      .use((fpw: FilePrintWriter) => F.delay(fpw.pw.write(msg)).as(fpw))
}
