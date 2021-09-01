package uk.gov.hmrc.fhdds.testsupport


import org.scalatest.OptionValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.{AnyWordSpec, AnyWordSpecLike}
import org.scalatestplus.play.WsScalaTestClient

trait TestHelpers
  extends AnyWordSpec
    with OptionValues
    with WsScalaTestClient
    with AnyWordSpecLike
    with Matchers
    with ScalaFutures {

}
