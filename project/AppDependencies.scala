import play.sbt.PlayImport.ws
import sbt._

object AppDependencies {

  val playVersion = "play-30"
  val bootstrapVersion = "10.4.0"
  val hmrcMongoVersion = "2.10.0"

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc"          %% s"bootstrap-backend-$playVersion" % bootstrapVersion,
    "uk.gov.hmrc.mongo"    %% s"hmrc-mongo-$playVersion"        % hmrcMongoVersion,
    "org.typelevel"        %% "cats-core"                       % "2.12.0",
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"            %% s"bootstrap-test-$playVersion"  % bootstrapVersion,
    "uk.gov.hmrc.mongo"      %% s"hmrc-mongo-test-$playVersion" % hmrcMongoVersion,
    "org.scalatestplus.play" %% "scalatestplus-play"            % "7.0.1",
  ).map(_ % "test, it")

  val all: Seq[ModuleID] = compile ++ test
}