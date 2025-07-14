package com.phasmidsoftware.visitor

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

class ListJournalSpec extends AnyFlatSpec with should.Matchers {

  behavior of "ListJournal"

  it should "test Iterator" in {
    val emptyJournal = ListJournal.empty[String]
    emptyJournal.iterator.isEmpty shouldBe true
    val journal = emptyJournal.append("a").append("b")
    val iterator = journal.iterator
    iterator.hasNext shouldBe true
    iterator.next shouldBe "b"
    iterator.hasNext shouldBe true
    iterator.next shouldBe "a"
    iterator.hasNext shouldBe false
  }

  it should "test list" in {
    val emptyJournal = ListJournal.empty[String]
    emptyJournal.iterator.isEmpty shouldBe true
    val journal = emptyJournal.append("a").append("b")
    val xs = journal.list
    xs shouldBe List("b", "a")
  }
}
