import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt._

object MicroServiceBuild extends Build with MicroService {

  val appName = "fh-registration"

  override lazy val appDependencies: Seq[ModuleID] = compile ++ test()

  val compile = Seq(
    ws,
    "uk.gov.hmrc"             %% "bootstrap-play-25"            % "4.9.0",
    "uk.gov.hmrc"             %% "simple-reactivemongo"         % "7.12.0-play-25",
    
    "com.github.tototoshi"    %% "play-json-naming"             % "1.1.0",
    "com.eclipsesource"       %% "play-json-schema-validator"   % "0.8.9",
    "org.typelevel"           %% "cats"                         % "0.9.0"
  )

  def test(scope: String = "test,it") = Seq(
    "uk.gov.hmrc"             %% "hmrctest"                     % "3.0.0"   % scope,
    "org.scalatest"           %% "scalatest"                    % "2.2.6"   % scope,
    "org.scalatestplus.play"  %% "scalatestplus-play"           % "2.0.0"   % scope,
    "org.mockito"              % "mockito-core"                 % "2.7.22"  % scope,
    "org.pegdown"              % "pegdown"                      % "1.6.0"   % scope,
    "com.typesafe.play"       %% "play-test"                    % PlayVersion.current % scope,
    "uk.gov.hmrc"             %% "reactivemongo-test"           % "4.8.0-play-25"   % scope,
    "com.github.tomakehurst"   % "wiremock"                     % "2.6.0"   % scope

  )

}
