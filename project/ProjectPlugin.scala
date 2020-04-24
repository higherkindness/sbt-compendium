import sbt.Keys._
import sbt.ScriptedPlugin.autoImport._
import sbt._
import sbtorgpolicies.OrgPoliciesPlugin
import sbtorgpolicies.OrgPoliciesPlugin.autoImport._
import sbtorgpolicies.model._
import sbtorgpolicies.runnable.syntax._
import sbtorgpolicies.templates._
import sbtorgpolicies.templates.badges._

object ProjectPlugin extends AutoPlugin {

  import autoImport._

  override def requires: Plugins = OrgPoliciesPlugin

  override def trigger: PluginTrigger = allRequirements

  override def projectSettings: Seq[Def.Setting[_]] = Seq(
    name := "sbt-compendium",
    orgGithubSetting := GitHubSettings(
      organization = "higherkindness",
      project = (name in LocalRootProject).value,
      organizationName = "47 Degrees",
      groupId = "io.higherkindness",
      organizationHomePage = url("http://47deg.com"),
      organizationEmail = "hello@47deg.com"
    ),
    scriptedLaunchOpts := {
      scriptedLaunchOpts.value ++
        Seq(
          "-Xmx1024M",
          "-XX:ReservedCodeCacheSize=256m",
          "-XX:+UseConcMarkSweepGC",
          "-Dplugin.version=" + version.value,
          "-Dscala.version=" + scalaVersion.value
        )
    },
    scriptedBufferLog := false,
    scalaVersion := V.scala,
    crossScalaVersions := Seq(scalaVersion.value),
    startYear := Some(2019),
    resolvers += Resolver.sonatypeRepo("snapshots"),
    orgProjectName := "sbt-compendium",
    orgUpdateDocFilesSetting += baseDirectory.value / "readme",
    orgMaintainersSetting := List(Dev("developer47deg", Some("47 Degrees (twitter: @47deg)"), Some("hello@47deg.com"))),
    orgBadgeListSetting := List(
      TravisBadge.apply,
      CodecovBadge.apply,
      info => MavenCentralBadge.apply(info.copy(libName = "sbt-compendium")),
      ScalaLangBadge.apply,
      LicenseBadge.apply,
      info => GitterBadge.apply(info.copy(owner = "higherkindness", repo = "sbt-compendium")),
      GitHubIssuesBadge.apply
    ),
    orgEnforcedFilesSetting := List(
      LicenseFileType(orgGithubSetting.value, orgLicenseSetting.value, startYear.value),
      ContributingFileType(
        orgProjectName.value,
        // Organization field can be configured with default value if we migrate it to the frees-io organization
        orgGithubSetting.value.copy(organization = "higherkindness", project = "sbt-compendium")
      ),
      AuthorsFileType(name.value, orgGithubSetting.value, orgMaintainersSetting.value, orgContributorsSetting.value),
      NoticeFileType(orgProjectName.value, orgGithubSetting.value, orgLicenseSetting.value, startYear.value),
      VersionSbtFileType,
      ChangelogFileType,
      ReadmeFileType(
        orgProjectName.value,
        orgGithubSetting.value,
        startYear.value,
        orgLicenseSetting.value,
        orgCommitBranchSetting.value,
        sbtPlugin.value,
        name.value,
        version.value,
        scalaBinaryVersion.value,
        sbtBinaryVersion.value,
        orgSupportedScalaJSVersion.value,
        orgBadgeListSetting.value
      ),
      ScalafmtFileType,
      TravisFileType(crossScalaVersions.value, orgScriptCICommandKey, orgAfterCISuccessCommandKey)
    ),
    orgScriptTaskListSetting := List(
      (clean in Global).asRunnableItemFull,
      (compile in Compile).asRunnableItemFull,
      (test in Test).asRunnableItemFull,
    ),
    addCompilerPlugin("org.augustjune" %% "context-applied" % V.contextApplied),
    addCompilerPlugin("org.typelevel"  %% "kind-projector"  % V.kindProjector cross CrossVersion.full)
  )

  object autoImport {

    val V = new {
      val cats            = "2.1.1"
      val contextApplied  = "0.1.4"
      val enumeratum      = "1.5.15"
      val enumeratumCirce = "1.5.23"
      val hammock         = "0.10.0"
      val kindProjector   = "0.11.0"
      val scala           = "2.12.10"
      val specs2          = "4.9.3"
      val avroHugger      = "1.0.0-RC22"
      val pureconfig      = "0.12.3"
      val skeuomorph      = "0.0.22"
      val droste          = "0.8.0"
      val scalameta       = "4.3.10"
    }

    val clientSettings: Seq[Def.Setting[_]] = Seq(
      libraryDependencies ++= Seq(
        %%("cats-core", V.cats),
        %%("pureconfig", V.pureconfig),
        "com.github.pureconfig" %% "pureconfig-cats-effect"  % V.pureconfig,
        "com.pepegar"           %% "hammock-core"            % V.hammock,
        "com.pepegar"           %% "hammock-circe"           % V.hammock,
        "com.pepegar"           %% "hammock-asynchttpclient" % V.hammock,
        "com.beachape"          %% "enumeratum"              % V.enumeratum,
        "com.beachape"          %% "enumeratum-circe"        % V.enumeratumCirce,
        "com.julianpeeters"     %% "avrohugger-core"         % V.avroHugger,
        "io.higherkindness"     %% "skeuomorph"              % V.skeuomorph,
        "io.higherkindness"     %% "droste-core"             % V.droste,
        %%("scalameta", V.scalameta),
        %%("specs2-core", V.specs2)       % Test,
        %%("specs2-scalacheck", V.specs2) % Test
      )
    )
  }
}
