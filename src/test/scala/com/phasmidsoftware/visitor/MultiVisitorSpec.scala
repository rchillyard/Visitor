package com.phasmidsoftware.visitor

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

class MultiVisitorSpec extends AnyFlatSpec with should.Matchers {

  behavior of "MultiVisitor"

  it should "visit" in {
    val visitor0 = MultiVisitor[String](Pre -> QueueJournal.empty[String])
    val visitor1 = visitor0.visit(Pre)("Hello").visit(Pre)("Goodbye")
    val maybeJournal = visitor1.appendable(Pre)
    maybeJournal.isDefined shouldBe true
    maybeJournal match {
      case Some(journal: Journal[_]) => journal.iterator.toList shouldBe List("Hello", "Goodbye")
    }
  }

}
