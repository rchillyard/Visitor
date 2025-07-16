package com.phasmidsoftware.visitor

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class AppendableWriterSpec extends AnyFlatSpec with Matchers {

  behavior of "AppendableWriter"

  it should "apply 1" in {
    val appendable: AppendableWriter = AppendableWriter.apply()
    appendable.append("Hello\n")
    appendable.append("Hello\n")
    appendable.writer.toString shouldBe "Hello\nHello\n"
  }

}
