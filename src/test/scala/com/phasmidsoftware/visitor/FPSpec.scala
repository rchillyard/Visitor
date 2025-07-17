package com.phasmidsoftware.visitor

import com.phasmidsoftware.visitor.FP.whenever
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

class FPSpec extends AnyFlatSpec with should.Matchers {

  behavior of "FP"

  it should "whenever" in {
    whenever(true)(Some(1)) shouldBe Some(1)
    whenever(true)(None) shouldBe None
    whenever(false)(Some(1)) shouldBe None
  }

}
