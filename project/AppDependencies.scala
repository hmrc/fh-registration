import play.sbt.PlayImport.ws
import sbt._

object AppDependencies {

  val bootstrapVersion = "8.4.0"
  val playVersion = "play-30"
  val hmrcMongoVersion = "1.7.0"

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc" %% s"bootstrap-backend-$playVersion" % bootstrapVersion,
    "org.typelevel" %% "cats-core" % "2.9.0",
    "com.github.tototoshi" %% "play-json-naming" % "1.5.0",
    "uk.gov.hmrc.mongo" %% s"hmrc-mongo-$playVersion" % hmrcMongoVersion,

    compilerPlugin("com.github.ghik" % "silencer-plugin" % "1.7.14" cross CrossVersion.full),
    "com.github.ghik" % "silencer-lib" % "1.7.14" % Provided cross CrossVersion.full
  )

  def test(scope: String = "test,it") = Seq(
    "uk.gov.hmrc" %% s"bootstrap-test-$playVersion" % bootstrapVersion % scope,
    "uk.gov.hmrc.mongo" %% s"hmrc-mongo-test-$playVersion" % hmrcMongoVersion % scope,
  )

  def apply(): Seq[ModuleID] = compile ++ test()
}