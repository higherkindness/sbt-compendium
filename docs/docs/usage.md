
# Getting started

Add to your `project/plugin.sbt` file the following line

```mdoc
    addSbtPlugin("io.higherkindness" %% "sbt-compendium" % "0.0.1")
```

Also to your `build.sbt` file add to your project settings

```mdoc
    .settings(
         compendiumSrcGenProtocolIdentifiers := List(ProtocolAndVersion("supplier",None),ProtocolAndVersion("material",None),ProtocolAndVersion("sale",None)),
         compendiumSrcGenServerHost := "localhost",
         compendiumSrcGenServerPort := 8080,
         sourceGenerators in Compile += Def.task {
           compendiumSrcGenClients.value
         }.taskValue
    )
```
**Note:** These example settings come from
[compendium-example](https://github.com/higherkindness/compendium-example).

The configuration works as follow:

-  `compendiumSrcGenServerHost`: *String*. Url of the compendium server.
   Default value: "localhost"
-  `compendiumSrcGenServerPort`: *Integer*. Port of the compendium
   server. Default value: 47047
-  `compendiumSrcGenFormatSchema`: *IdlName type*. Schema type to
   download. Default value: IdlName.Avro. Currently supported: Avro,
   Proto.
-  `compendiumSrcGenProtocolIdentifiers`: *`Seq[ProtocolAndVersion]*.
   Protocol identifiers to be retrieved from compendium server.
   `ProtocolAndVersion` provides two values: `name` (mandatory) that
   corresponds with the identifier used to store the protocol and
   `version` (optional). Default: Nil

## How to use it

Once you have set up the configuration, in the compilation runtime `sbt-compendium` will
get the protocols from compendium service, validate them and create the
proper scala classes on `target/scala-2.12/src_managed`.

### What if I want to save a class on compendium?

`sbt-compendium` is design to retrieve protocols but not to save it. If
you want to save protocols you'll need to make an http call to

```mdoc
[compendium host and port]/v0/protocol/[identifier]?idlName=[format]
```

with the body

```mdoc
{
  "raw" : [protocol string formatted]
}
```
