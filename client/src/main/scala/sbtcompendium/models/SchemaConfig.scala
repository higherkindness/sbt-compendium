package sbtcompendium.models

import sbtcompendium.models.proto.{CompressionTypeGen, StreamingImplementation}

trait SchemaConfig

final case class ProtoConfig(compressionTypeGen: CompressionTypeGen, streamingImplementation: StreamingImplementation)
    extends SchemaConfig
