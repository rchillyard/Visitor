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

  it should "journals" in {
    Using(MultiVisitor[String](Pre -> QueueJournal.empty[String])) {
      visitor0 =>
        val visitor1 = visitor0.addAppendable(Post, ListJournal.empty[String]).visit(Pre)("Hello").visit(Post)("Go away!").visit(Pre)("Goodbye")
        for {journal <- visitor1.journals
             entry <- journal
             } yield entry
    } shouldBe Success(List("Hello", "Goodbye", "Go away!"))
  }

  it should "mapJournals" in {
    Using(MultiVisitor[(String, String)](Pre -> MapJournal.empty[String, String])) {
      visitor0 =>
        val visitor1 = visitor0.addAppendable(Post, ListJournal.empty[(String, String)]).visit(Pre)("Hello" -> "Greeting").visit(Post)("Go away!" -> "").visit(Pre)("Goodbye" -> "Valediction")
        for {journal <- visitor1.mapJournals
             entry <- journal
             } yield entry
    } shouldBe Success(List("Hello" -> "Greeting", "Goodbye" -> "Valediction"))
  }

}
