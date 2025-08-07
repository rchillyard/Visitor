package com.phasmidsoftware.visitor

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

import scala.util.{Success, Using}

class MultiVisitorSpec extends AnyFlatSpec with should.Matchers {

  behavior of "MultiVisitor"

  it should "visit" in {
    Using(MultiVisitor[String](Pre -> QueueJournal.empty[String])) {
      visitor0 =>
        val visitor1 = visitor0.visit(Pre)("Hello").visit(Pre)("Goodbye")
        visitor1.appendable(Pre) match {
          case Some(journal: IterableJournal[_]) => journal.iterator.toList shouldBe List("Hello", "Goodbye")
          case _ => fail("No journal")
        }
    }
  }

  it should "addAppendable" in {
    Using(MultiVisitor[String]()) {
      visitor =>
        val visitor1 = visitor.addAppendable(Pre, QueueJournal.empty[String]).visit(Pre)("Hello").visit(Pre)("Goodbye")
        visitor1.appendable(Pre) match {
          case Some(journal: IterableJournal[_]) => journal.iterator.toList shouldBe List("Hello", "Goodbye")
          case _ => fail("No journal")
        }
    }
  }

  it should "not addAppendable" in {
    Using(MultiVisitor[String]()) {
      visitor =>
        val visitor1 = visitor.visit(Pre)("Hello").visit(Pre)("Goodbye")
        visitor1.appendable(Pre) match {
          case Some(journal: IterableJournal[_]) =>
            journal.iterator.toList shouldBe List("Hello", "Goodbye")
          case _ =>
            fail("No journal")
        }
    }.isSuccess shouldBe false
  }

  it should "iterableJournals" in {
    Using(MultiVisitor[String](Pre -> QueueJournal.empty[String])) {
      visitor0 =>
        val visitor1 = visitor0.addAppendable(Post, ListJournal.empty[String]).visit(Pre)("Hello").visit(Post)("Go away!").visit(Pre)("Goodbye")
        for {journal <- visitor1.iterableJournals
             entry <- journal
             } yield entry
    } shouldBe Success(List("Hello", "Goodbye", "Go away!"))
  }

  it should "mapJournals" in {
    Using(VisitorMapped[String, String](Map(Pre -> MapJournal.empty[String, String]))) {
      visitor =>
        val visitor0: VisitorMapped[String, String] = visitor.addAppendable(Post, ListJournal.empty[(String, String)]).asInstanceOf[VisitorMapped[String, String]]
        val visitor1: VisitorMapped[String, String] = visitor0.visit(Pre)("Hello" -> "Greeting").visit(Post)("Go away!" -> "").visit(Pre)("Goodbye" -> "Valediction")
        for {journal <- visitor1.mapJournals
             } yield journal
    } shouldBe Success(List(MapJournal(Map("Hello" -> "Greeting", "Goodbye" -> "Valediction"))))
  }

}
