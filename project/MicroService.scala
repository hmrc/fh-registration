import sbt.Keys._
import sbt.Tests.{Group, SubProcess}
import sbt._
import play.routes.compiler.StaticRoutesGenerator
import play.sbt.PlayImport.PlayKeys
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._


trait MicroService {

  import uk.gov.hmrc._
  import DefaultBuildSettings._
  import uk.gov.hmrc.{SbtBuildInfo, ShellPrompt, SbtAutoBuildPlugin}
  import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
  import uk.gov.hmrc.versioning.SbtGitVersioning
  import play.sbt.routes.RoutesKeys.routesGenerator
  import sbtscalaxb.Plugin.ScalaxbKeys
  import ScalaxbKeys._


  import TestPhases._

  val appName: String

  lazy val appDependencies : Seq[ModuleID] = ???
  lazy val plugins : Seq[Plugins] = Seq.empty
  lazy val playSettings : Seq[Setting[_]] = Seq.empty

  lazy val scalaXml = "org.scala-lang.modules" %% "scala-xml" % "1.0.2"
  lazy val scalaParser = "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.1"
  lazy val dispatchV = "0.11.2"
  lazy val dispatch = "net.databinder.dispatch" %% "dispatch-core" % dispatchV

  lazy val microservice = Project(appName, file("."))
    .enablePlugins(Seq(play.sbt.PlayScala,SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin) ++ plugins : _*)
    .settings(unmanagedResourceDirectories in Compile += baseDirectory.value / "resources")
    .settings(PlayKeys.playDefaultPort := 1119)
    .settings(playSettings : _*)
    .settings(scalaSettings: _*)
    .settings(publishingSettings: _*)
    .settings(defaultSettings(): _*)
    .settings(
      libraryDependencies ++= appDependencies,
      retrieveManaged := true,
      evictionWarningOptions in update := EvictionWarningOptions.default.withWarnScalaVersionEviction(false),
      routesGenerator := StaticRoutesGenerator
    )
    .configs(IntegrationTest)
    .settings(inConfig(IntegrationTest)(Defaults.itSettings): _*)
    .settings(sbtscalaxb.Plugin.scalaxbSettings: _*)
    .settings(
      sourceGenerators in Compile += (scalaxb in Compile).taskValue,
      dispatchVersion in(Compile, scalaxb) := dispatchV,
//      protocolFileName in(Compile, scalaxb) := "generated/fhdds/xmlprotocol.scala",
      protocolPackageName in(Compile, scalaxb) := Some("generated.fhdds"),
      async in(Compile, scalaxb) := false,
      packageNames in (Compile, scalaxb) := Map(
        uri("http://iforms.hmrc.gov.uk/fhdds/sole") -> "generated.sole",
        uri("http://iforms.hmrc.gov.uk/fhdds/limited") -> "generated.limited",
        uri("http://iforms.hmrc.gov.uk/fhdds/partnership") -> "generated.partnership"
      ),
//      packageName in(Compile, scalaxb) := "generated",
      xsdSource in(Compile, scalaxb) := file("resources/schemas/"),
      ignoreUnknown in(Compile, scalaxb) := true) // to ignore unknown XML elements, as well as order in which they are arrived
    .settings(
      Keys.fork in IntegrationTest := false,
      resourceDirectory in IntegrationTest := baseDirectory.value / "it/resources",
      unmanagedSourceDirectories in IntegrationTest <<= (baseDirectory in IntegrationTest)(base => Seq(base / "it")),
      addTestReportOption(IntegrationTest, "int-test-reports"),
      testGrouping in IntegrationTest := oneForkedJvmPerTest((definedTests in IntegrationTest).value),
      parallelExecution in IntegrationTest := false)
      .settings(resolvers ++= Seq(
        Resolver.bintrayRepo("hmrc", "releases"),
        "emueller-bintray" at "http://dl.bintray.com/emueller/maven",
        Resolver.jcenterRepo
      ))
}

private object TestPhases {

  def oneForkedJvmPerTest(tests: Seq[TestDefinition]) =
    tests map {
      test => new Group(test.name, Seq(test), SubProcess(ForkOptions(runJVMOptions = Seq("-Dtest.name=" + test.name))))
    }
}
