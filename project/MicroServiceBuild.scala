import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt._

object MicroServiceBuild extends Build with MicroService {

  val appName = "fh-registration"

  override lazy val appDependencies: Seq[ModuleID] = compile ++ test()

  val compile = Seq(
    ws,
    "uk.gov.hmrc"             %% "bootstrap-play-25"            % "1.5.0",
    "uk.gov.hmrc"             %% "play-reactivemongo"           % "6.1.0",
    "uk.gov.hmrc"             %% "simple-reactivemongo"         % "6.1.0",
    
    "com.github.tototoshi"    %% "play-json-naming"             % "1.1.0",
    "com.eclipsesource"       %% "play-json-schema-validator"   % "0.8.9",
    "org.typelevel"           %% "cats"                         % "0.9.0"
  )

  def test(scope: String = "test,it") = Seq(
    "uk.gov.hmrc"             %% "hmrctest"                     % "3.0.0"   % scope,
    "org.scalatest"           %% "scalatest"                    % "2.2.6"   % scope,
    "org.scalatestplus.play"  %% "scalatestplus-play"           % "2.0.0"   % scope,
    "org.scoverage"           %  "scalac-scoverage-runtime_2.11"% "1.3.1"   % scope,
    "org.mockito"              % "mockito-core"                 % "2.7.22"  % scope,
    "org.pegdown"              % "pegdown"                      % "1.6.0"   % scope,
    "com.typesafe.play"       %% "play-test"                    % PlayVersion.current % scope,
    "uk.gov.hmrc"             %% "reactivemongo-test"           % "3.1.0"   % scope,
    "com.github.tomakehurst"   % "wiremock"                     % "2.6.0"   % scope

  )

}
