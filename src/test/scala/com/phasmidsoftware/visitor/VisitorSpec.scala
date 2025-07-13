package com.phasmidsoftware.visitor

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

class VisitorSpec extends AnyFlatSpec with should.Matchers {

  behavior of "Visitor"

  it should "visit Pre" in {
    val visitor0 = SimpleVisitor[String](QueueJournal.empty[String], Pre)
    val visitor1 = visitor0.visit(Pre)("Hello").visit(Pre)("Goodbye")
    val journal: QueueJournal[String] = visitor1.appendable.asInstanceOf[QueueJournal[String]]
    journal.iterator.toList shouldBe List("Hello", "Goodbye")
  }

  it should "visit Post" in {
    val visitor0 = SimpleVisitor[String](QueueJournal.empty[String], Post)
    val visitor1 = visitor0.visit(Post)("Hello").visit(Pre)("How are You?").visit(Post)("Goodbye")
    val journal: QueueJournal[String] = visitor1.appendable.asInstanceOf[QueueJournal[String]]
    journal.iterator.toList shouldBe List("Hello", "Goodbye")
  }

  it should "close" in {
    val visitor0 = SimpleVisitor[String](QueueJournal.empty[String], Pre)
    visitor0.close()
    a[UnsupportedOperationException] should be thrownBy visitor0.visit(Pre)("Hello")
  }

}
