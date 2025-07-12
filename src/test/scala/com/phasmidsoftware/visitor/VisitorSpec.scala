package com.phasmidsoftware.visitor

import org.scalatest.flatspec.AnyFlatSpec

class VisitorSpec extends AnyFlatSpec {

  behavior of "VisitorSpec"

  it should "visit" in {
    val visitor = new Visitor[Int] {
      def visit(msg: Message)(x: Int): Visitor[Int] = unit
    }
  }

}
