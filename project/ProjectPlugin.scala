import _root_.io.github.davidgregory084.TpolecatPlugin.autoImport._
import com.typesafe.sbt.site.jekyll.JekyllPlugin.autoImport._
import microsites.MicrositeKeys._
import microsites._
import sbt.Keys._
import sbt.ScriptedPlugin.autoImport._
import sbt._
import sbtorgpolicies.OrgPoliciesPlugin
import sbtorgpolicies.OrgPoliciesPlugin.autoImport._
import sbtorgpolicies.model._
import sbtorgpolicies.runnable.syntax._
import sbtorgpolicies.templates._
import sbtorgpolicies.templates.badges._
import sbtrelease.ReleasePlugin.autoImport._
import scala.language.reflectiveCalls
import scoverage.ScoverageKeys._
import tut.TutPlugin.autoImport._


object ProjectPlugin extends AutoPlugin {

  override def requires: Plugins = OrgPoliciesPlugin

  override def trigger: PluginTrigger = allRequirements

  object autoImport {

    val V = new {
        val scala = "2.12.10"
      val betterMonadicFor = "0.3.1"
      val cats = "2.0.0"
      val compendiumClient = "0.0.1-SNAPSHOT"
      val catsScalacheck = "0.2.0"
      val hammock = "0.10.0"
      val kindProjector = "0.10.3"
      val macroParadise = "2.1.1"
      val scalacheck = "1.14.0"
      val enumeratum = "1.5.13"
      val specs2 = "4.8.1"
    }

    val micrositeSettings: Seq[Def.Setting[_]] = Seq(
      micrositeName := "sbt-compendium",
      micrositeDescription := "Schema transformations",
      micrositeBaseUrl := "/sbt-compendium",
      micrositeGithubOwner := "higherkindness",
      micrositeGithubRepo := "sbt-compendium",
      micrositeHighlightTheme := "tomorrow",
      includeFilter in Jekyll := "*.html" | "*.css" | "*.png" | "*.jpg" | "*.gif" | "*.js" | "*.swf" | "*.md",
      micrositePushSiteWith := GitHub4s,
      micrositeExtraMdFiles := Map(
        file("README.md") -> ExtraMdFileConfig(
          "index.md",
          "home",
          Map("title" -> "Home", "section" -> "home", "position" -> "0")
        ),
        file("CHANGELOG.md") -> ExtraMdFileConfig(
          "changelog.md",
          "home",
          Map("title" -> "changelog", "section" -> "changelog", "position" -> "99")
        )
      )

    )

    // General Settings
    val tutSettings: Seq[Def.Setting[_]] = Seq(
      scalacOptions in Tut ~= filterConsoleScalacOptions,
      scalacOptions ~= (_ filterNot Set("-Xfatal-warnings", "-Ywarn-unused-import", "-Xlint").contains),
      scalacOptions in Tut += "-language:postfixOps"
    )

    val compilerPlugins: Seq[Def.Setting[_]] = Seq(
      libraryDependencies ++= Seq(
        compilerPlugin("org.spire-math"  % "kind-projector"      % V.kindProjector cross CrossVersion.binary),
        compilerPlugin("com.olegpy"      %% "better-monadic-for" % V.betterMonadicFor),
        compilerPlugin("org.scalamacros" % "paradise"            % V.macroParadise cross CrossVersion.patch)
      )
    )

    val commonSettings: Seq[Def.Setting[_]] = Seq(
      name := "sbt-compendium",
      orgGithubSetting := GitHubSettings(
        organization = "higherkindness",
        project = (name in LocalRootProject).value,
        organizationName = "47 Degrees",
        groupId = "io.higherkindness",
        organizationHomePage = url("http://47deg.com"),
        organizationEmail = "hello@47deg.com"
      ),
      scriptedLaunchOpts := { scriptedLaunchOpts.value ++
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
      startYear := Some(2018),
      ThisBuild / scalacOptions -= "-Xplugin-require:macroparadise",
      libraryDependencies ++= Seq(
        %%("cats-core", V.cats),
        %%("hammock-core", V.hammock),
        "io.higherkindness" %% "compendium-client" % V.compendiumClient,
        "com.pepegar" %% "hammock-circe" % V.hammock,
        "com.pepegar" %% "hammock-asynchttpclient" % V.hammock,
        %%("specs2-core"      , V.specs2)       % Test,
        %%("specs2-scalacheck", V.specs2) % Test,
        "com.beachape" %% "enumeratum" % V.enumeratum,
        "io.chrisdavenport"     %% "cats-scalacheck" % V.catsScalacheck % Test excludeAll(
          ExclusionRule(organization="org.scalacheck")
        )
      ),
      orgProjectName := "sbt-compendium",
      orgUpdateDocFilesSetting += baseDirectory.value / "readme",
      orgMaintainersSetting := List(Dev("developer47deg", Some("47 Degrees (twitter: @47deg)"), Some("hello@47deg.com"))),
      orgBadgeListSetting := List(
        TravisBadge.apply,
        CodecovBadge.apply, { info =>
          MavenCentralBadge.apply(info.copy(libName = "sbt-compendium"))
        },
        ScalaLangBadge.apply,
        LicenseBadge.apply, { info =>
          GitterBadge.apply(info.copy(owner = "higherkindness", repo = "sbt-compendium"))
        },
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
        "docs/tut".asRunnableItem
      )
    )

  }

}
