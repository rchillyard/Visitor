package com.phasmidsoftware.visitor

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

import scala.util.Using

class MultiVisitorSpec extends AnyFlatSpec with should.Matchers {

  behavior of "MultiVisitor"

  it should "visit" in {
    Using(MultiVisitor[String](Pre -> QueueJournal.empty[String])) {
      visitor0 =>
        val visitor1 = visitor0.visit(Pre)("Hello").visit(Pre)("Goodbye")
        visitor1.appendable(Pre) match {
          case Some(journal: Journal[_]) => journal.iterator.toList shouldBe List("Hello", "Goodbye")
          case _ => fail("No journal")
        }
    }
  }

  it should "addAppendable" in {
    Using(MultiVisitor[String]()) {
      visitor =>
        val visitor1 = visitor.addAppendable(Pre, QueueJournal.empty[String]).visit(Pre)("Hello").visit(Pre)("Goodbye")
        visitor1.appendable(Pre) match {
          case Some(journal: Journal[_]) => journal.iterator.toList shouldBe List("Hello", "Goodbye")
          case _ => fail("No journal")
        }
    }
  }

  it should "not addAppendable" in {
    Using(MultiVisitor[String]()) {
      visitor =>
        val visitor1 = visitor.visit(Pre)("Hello").visit(Pre)("Goodbye")
        visitor1.appendable(Pre) match {
          case Some(journal: Journal[_]) =>
            journal.iterator.toList shouldBe List("Hello", "Goodbye")
          case _ =>
            fail("No journal")
        }
    }.isSuccess shouldBe false
  }

}
