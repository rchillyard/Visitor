package com.phasmidsoftware.visitor

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers.*

class LoggingSpec extends AnyFlatSpec {

  behavior of "Logging"

  import Logging.*

  it should "!! 1" in {
    setLogging(true)
    val sb = new StringBuilder()
    setLogFunction(s => sb.append(s).append("\n"))

    ("Hello" !! 1) shouldBe 1
    sb.toString shouldBe "Hello: 1\n"
  }

  it should "!! Option(1)" in {
    setLogging(true)
    val sb = new StringBuilder()
    setLogFunction(s => sb.append(s).append("\n"))

    ("Hello" !! Some(1)) shouldBe Some(1)
    sb.toString shouldBe "Hello: Some(1)\n"
  }

  it should "!! Seq(1,2,3)" in {
    setLogging(true)
    val sb = new StringBuilder()
    setLogFunction(s => sb.append(s).append("\n"))

    val numbers = Seq(1, 2, 3)
    ("Hello" !! numbers) shouldBe numbers
    sb.toString shouldBe "Hello: List(1, 2, 3)\n"
  }
}
