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

package sbtcompendium.models.proto

sealed abstract class CompressionTypeGen(val value: String) extends Product with Serializable

case object GzipGen          extends CompressionTypeGen("Gzip")
case object NoCompressionGen extends CompressionTypeGen("Identity")

sealed trait StreamingImplementation

case object Fs2Stream       extends StreamingImplementation
case object MonixObservable extends StreamingImplementation
