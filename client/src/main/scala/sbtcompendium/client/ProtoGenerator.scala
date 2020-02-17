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
import higherkindness.skeuomorph.protobuf.ParseProto.{parseProto, ProtoSource}
import higherkindness.skeuomorph.protobuf.ProtobufF
import higherkindness.skeuomorph.mu.{CompressionType, MuF}
import higherkindness.droste.data.Mu
import higherkindness.droste.data.Mu._
import java.io.{File, PrintWriter}

import sbtcompendium.client.CompendiumClient.FilePrintWriter

import scala.meta._
import scala.meta.Type

object ProtoGenerator {

  val streamCtor: (Type, Type) => Type.Apply = { case (f, a) => t"_root_.fs2.Stream[$f, $a]" }

  val transformToMuProtocol: higherkindness.skeuomorph.protobuf.Protocol[Mu[ProtobufF]] => higherkindness.skeuomorph.mu.Protocol[
    Mu[
      MuF
    ]
  ] =
    higherkindness.skeuomorph.mu.Protocol
      .fromProtobufProto(CompressionType.Identity, false)

  val generateScalaSource: higherkindness.skeuomorph.mu.Protocol[Mu[MuF]] => Either[
    String,
    String
  ] =
    higherkindness.skeuomorph.mu.codegen.protocol(_, streamCtor).map(_.syntax)

  def getCode[F[_]](raw: String)(implicit F: Sync[F]): F[List[String]] = {

    for {
      file <- writeTempFile(raw, extension.proto)
      protocol <- parseProto[F, Mu[ProtobufF]]
        .parse(ProtoSource(file.file.getName, file.file.getAbsolutePath.replace(file.file.getName, "")))
      res <- (transformToMuProtocol andThen generateScalaSource)(protocol) match {
        case Left(error) =>
          F.raiseError(
            ProtoBufGenError(
              s"Failed to generate Scala source from Protobuf file ${file.file.getAbsolutePath}. Error details: $error"
            )
          )
        case Right(fileContent) =>
          F.pure(List(dropImportAndType(fileContent)))
      }
    } yield res
  }

  private def dropImportAndType(str: String) =
    str
      .replace("\nimport _root_.higherkindness.mu.rpc.protocol._", "")
      .replace("@message ", "")
      .replace("@service(Protobuf, Identity)", "")

  private def writeTempFile[F[_]: Sync](msg: String, extension: String, index: String = ""): F[FilePrintWriter] =
    Resource
      .make(F.delay {
        val tmpDir = System.getProperty("java.io.tmpdir")
        val file   = new File(tmpDir + s"/compendium$index$extension")
        file.deleteOnExit()
        FilePrintWriter(file, new PrintWriter(file))
      }) { fpw: FilePrintWriter => F.delay(fpw.pw.close()) }
      .use((fpw: FilePrintWriter) => F.delay(fpw.pw.write(msg)).as(fpw))

}
