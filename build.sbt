import microsites._
import sbtorgpolicies.OrgPoliciesPlugin.autoImport._
import sbtorgpolicies.model._
import sbtorgpolicies.runnable.SetSetting
import sbtorgpolicies.runnable.syntax._
import sbtorgpolicies.templates._
import sbtorgpolicies.templates.badges._
import scoverage.ScoverageKeys

val V = new {
  val betterMonadicFor = "0.2.4"
  val cats             = "1.5.0"
  val catsScalacheck   = "0.1.0"
  val circe            = "0.10.1"
  val hammock          = "0.8.7"
  val kindProjector    = "0.10.0"
  val macroParadise    = "2.1.1"
  val scalacheck       = "1.13.5"
  val specs2           = "4.1.0" // DO NOT BUMP. We need all dependent libraries to bump version of scalacheck to 1.14, otherwise we face a bincompat issue between scalacheck 1.14 & scalacheck 1.13.5
}

lazy val root = project
  .in(file("."))
  .settings(commonSettings)
  .settings(moduleName := "sbt-compendium")
  .settings(sbtPlugin := true)

lazy val docs = project
  .in(file("docs"))
  .dependsOn(root)
  .settings(moduleName := "sbt-compendium-docs")
  .settings(commonSettings)
  .settings(sbtMicrositesSettings)
  .settings(noPublishSettings)
  .settings(tutSettings)
  .settings(
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
  .enablePlugins(MicrositesPlugin)

// check for library updates whenever the project is [re]load
onLoad in Global := { s =>
  "dependencyUpdates" :: s
}

pgpPassphrase := Some(getEnvVar("PGP_PASSPHRASE").getOrElse("").toCharArray)
pgpPublicRing := file(s"$gpgFolder/pubring.gpg")
pgpSecretRing := file(s"$gpgFolder/secring.gpg")

// General Settings
lazy val commonSettings = Seq(
  name := "sbt-compendium",
  orgGithubSetting := GitHubSettings(
    organization = "higherkindness",
    project = (name in LocalRootProject).value,
    organizationName = "47 Degrees",
    groupId = "io.higherkindness",
    organizationHomePage = url("http://47deg.com"),
    organizationEmail = "hello@47deg.com"
  ),
  scalaVersion := "2.12.8",
  crossScalaVersions := Seq(scalaVersion.value),
  startYear := Some(2018),
  ThisBuild / scalacOptions -= "-Xplugin-require:macroparadise",
  libraryDependencies ++= Seq(
    %%("cats-core", V.cats),
    %%("circe-core", V.circe),
    %%("hammock-core", V.hammock),
    "com.pepegar" %% "hammock-circe" % V.hammock,
    "com.pepegar" %% "hammock-asynchttpclient" % V.hammock,
    %%("specs2-core"      , V.specs2)       % Test,
    %%("specs2-scalacheck", V.specs2) % Test,
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
) ++ compilerPlugins

lazy val tutSettings = Seq(
  scalacOptions in Tut ~= filterConsoleScalacOptions,
  scalacOptions ~= (_ filterNot Set("-Xfatal-warnings", "-Ywarn-unused-import", "-Xlint").contains),
  scalacOptions in Tut += "-language:postfixOps"
)

lazy val compilerPlugins = Seq(
  libraryDependencies ++= Seq(
    compilerPlugin("org.typelevel"  % "kind-projector"      % V.kindProjector cross CrossVersion.binary),
    compilerPlugin("com.olegpy"      %% "better-monadic-for" % V.betterMonadicFor),
    compilerPlugin("org.scalamacros" % "paradise"            % V.macroParadise cross CrossVersion.patch)
  )
)
