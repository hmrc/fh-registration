package uk.gov.hmrc.fhdds.testsupport


import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, OptionValues, WordSpec, WordSpecLike}
import org.scalatestplus.play.WsScalaTestClient

trait TestHelpers
  extends WordSpec
    with OptionValues
    with WsScalaTestClient
    with WordSpecLike
    with Matchers
    with ScalaFutures {

}
