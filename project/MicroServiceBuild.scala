import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt._

object MicroServiceBuild extends Build with MicroService {

  val appName = "fh-registration"

  override lazy val appDependencies: Seq[ModuleID] = compile ++ test()

  val compile = Seq(
    ws,
    "uk.gov.hmrc"             %% "bootstrap-play-26"            % "0.42.0",
    "uk.gov.hmrc"             %% "simple-reactivemongo"         % "7.20.0-play-26",
    "com.github.tototoshi"    %% "play-json-naming"             % "1.3.0",
    "com.eclipsesource"       %% "play-json-schema-validator"   % "0.8.9",
    "org.typelevel"           %% "cats"                         % "0.9.0"
  )

  def test(scope: String = "test,it") = Seq(
    "uk.gov.hmrc"             %% "hmrctest"                     % "3.9.0-play-26"   % scope,
    "org.scalatest"           %% "scalatest"                    % "3.0.8"   % scope,
    "org.scalatestplus.play"  %% "scalatestplus-play"           % "3.1.2"   % scope,
    "org.mockito"              % "mockito-core"                 % "3.0.0"  % scope,
    "org.pegdown"              % "pegdown"                      % "1.6.0"   % scope,
    "com.typesafe.play"       %% "play-test"                    % PlayVersion.current % scope,
    "uk.gov.hmrc"             %% "reactivemongo-test"           % "4.15.0-play-26"   % scope,
    "com.github.tomakehurst"   % "wiremock-jre8"                % "2.23.2"  % scope

  )

}
