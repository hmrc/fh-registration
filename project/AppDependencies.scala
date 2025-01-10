import play.sbt.PlayImport.ws
import sbt._

object AppDependencies {

  val playVersion = "play-30"
  val bootstrapVersion = "9.6.0"
  val hmrcMongoVersion = "2.3.0"

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc"          %% s"bootstrap-backend-$playVersion" % bootstrapVersion,
    "uk.gov.hmrc.mongo"    %% s"hmrc-mongo-$playVersion"        % hmrcMongoVersion,
    "org.typelevel"        %% "cats-core"                       % "2.12.0",
    "com.github.tototoshi" %% "play-json-naming"                % "1.5.0",
    compilerPlugin("com.github.ghik" % "silencer-plugin" % "1.7.14" cross CrossVersion.full),
    "com.github.ghik" % "silencer-lib" % "1.7.14" % Provided cross CrossVersion.full
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"            %% s"bootstrap-test-$playVersion"  % bootstrapVersion,
    "uk.gov.hmrc.mongo"      %% s"hmrc-mongo-test-$playVersion" % hmrcMongoVersion,
    "org.mockito"            %% "mockito-scala-scalatest"       % "1.17.37",
    "org.scalatestplus.play" %% "scalatestplus-play"            % "7.0.1",
    "com.typesafe.play"      %% "play-json"                     % "2.10.6"

  ).map(_ % "test, it")

  val all: Seq[ModuleID] = compile ++ test
}