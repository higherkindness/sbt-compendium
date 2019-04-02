import sbt._
import sbtorgpolicies.OrgPoliciesPlugin

object ProjectPlugin extends AutoPlugin {

  override def requires: Plugins = OrgPoliciesPlugin

  override def trigger: PluginTrigger = allRequirements

  object autoImport {

    val V = new {
      val betterMonadicFor = "0.2.4"
      val cats = "1.5.0"
      val catsScalacheck = "0.1.0"
      val hammock = "0.8.7"
      val kindProjector = "0.9.9"
      val macroParadise = "2.1.1"
      val scalacheck = "1.13.5"
      val enumeratum = "1.5.13"
      val specs2 = "4.1.0" // DO NOT BUMP. We need all dependent libraries to bump version of scalacheck to 1.14, otherwise we face a bincompat issue between scalacheck 1.14 & scalacheck 1.13.5
    }

  }
}
