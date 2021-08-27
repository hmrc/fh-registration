import play.sbt.PlayImport._
import play.sbt.PlayImport.PlayKeys
import sbt.Keys._
import sbt.Tests.{Group, SubProcess}
import sbt._
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._
import com.lucidchart.sbt.scalafmt.ScalafmtCorePlugin.autoImport._
import uk.gov.hmrc._
import DefaultBuildSettings._
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

val appName = "fh-registration"

val playVersion = "play-28"

lazy val appDependencies: Seq[ModuleID] = compile ++ test()

val compile = Seq(
  ws,
  "uk.gov.hmrc"             %% s"bootstrap-backend-$playVersion"    % "5.12.0",
  "uk.gov.hmrc"             %% "simple-reactivemongo"               % s"8.0.0-$playVersion",
  "com.github.tototoshi"    %% "play-json-naming"                   % "1.5.0",
  "org.typelevel"           %% "cats-core"                          % "2.6.1",
  compilerPlugin("com.github.ghik" % "silencer-plugin"  % "1.7.4" cross CrossVersion.full),
  "com.github.ghik"         % "silencer-lib"                        % "1.7.4" % Provided cross CrossVersion.full
)

def test(scope: String = "test,it") = Seq(
  "org.scalatest"           %% "scalatest"                    % "3.2.9"   % scope,
  "org.scalatestplus.play"  %% "scalatestplus-play"           % "5.1.0"   % scope,
  "com.vladsch.flexmark"     % "flexmark-all"                 % "0.35.10" % scope,
  "org.scalatestplus"       %% "mockito-3-4"                % "3.2.9.0" % scope,
  "uk.gov.hmrc"             %% "reactivemongo-test"           % s"5.0.0-$playVersion"   % scope,
  "com.github.tomakehurst"   %  "wiremock-standalone"         % "2.27.1"  % scope,
)

lazy val plugins : Seq[Plugins] = Seq.empty
lazy val playSettings : Seq[Setting[_]] = Seq.empty

lazy val scoverageSettings = {
  import scoverage.ScoverageKeys
  Seq(
    // Semicolon-separated list of regexs matching classes to exclude
    ScoverageKeys.coverageExcludedPackages := """uk\.gov\.hmrc\.BuildInfo;.*\.Routes;.*\.RoutesPrefix;.*\.Reverse[^.]*""",
    ScoverageKeys.coverageMinimumStmtTotal := 80.00,
    ScoverageKeys.coverageExcludedFiles := """.*EnrolmentStoreProxyConnector.*;.*UserSearchConnector.*;.*AdminController.*""",
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true,
    Test / parallelExecution := false
  )
}

lazy val microservice = Project(appName, file("."))
  .enablePlugins(Seq(play.sbt.PlayScala, SbtDistributablesPlugin) ++ plugins : _*)
  .settings(majorVersion := 0)
  .settings(Compile / unmanagedResourceDirectories += baseDirectory.value / "resources")
  .settings(PlayKeys.playDefaultPort := 1119)
  .settings(playSettings : _*)
  .settings(scoverageSettings: _*)
  .settings(scalaSettings: _*)
  .settings(publishingSettings: _*)
  .settings(scalaVersion := "2.12.13")
  .settings(defaultSettings(): _*)
  .settings(
    libraryDependencies ++= appDependencies,
    retrieveManaged := true,
    update / evictionWarningOptions := EvictionWarningOptions.default.withWarnScalaVersionEviction(false),
    Compile / scalafmtOnCompile := true,
    Test / scalafmtOnCompile := true
  )
  .configs(IntegrationTest)
  .settings(inConfig(IntegrationTest)(Defaults.itSettings): _*)
  .settings(
    IntegrationTest / Keys.fork := false,
    IntegrationTest / resourceDirectory := baseDirectory.value / "it/resources",
    IntegrationTest / unmanagedSourceDirectories := (IntegrationTest / baseDirectory)(base => Seq(base / "it")).value,
    addTestReportOption(IntegrationTest, "int-test-reports"),
    IntegrationTest / testGrouping := oneForkedJvmPerTest((IntegrationTest / definedTests).value),
    IntegrationTest / parallelExecution := false,
    IntegrationTest / scalafmtOnCompile := true)
  .settings(resolvers += "third-party-maven-releases" at "https://artefacts.tax.service.gov.uk/artifactory/third-party-maven-releases/")
  .settings(scalacOptions += "-P:silencer:pathFilters=routes")
  .settings(Global / lintUnusedKeysOnLoad := false)

def oneForkedJvmPerTest(tests: Seq[TestDefinition]) = {
  tests.map { test =>
    new Group(test.name, Seq(test), SubProcess(ForkOptions().withRunJVMOptions(Vector(s"-Dtest.name=${test.name}"))))
  }
}