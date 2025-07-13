package com.phasmidsoftware.visitor

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

class MapJournalSpec extends AnyFlatSpec with should.Matchers {

  behavior of "MapJournal"

  it should "test Iterator" in {
    val emptyJournal = MapJournal.empty[String, Int]
    emptyJournal.iterator.isEmpty shouldBe true
    val journal = emptyJournal.append(("a", 1)).append(("b", 2))
    val iterator = journal.iterator
    iterator.hasNext shouldBe true
    iterator.next shouldBe("a", 1)
    iterator.hasNext shouldBe true
    iterator.next shouldBe("b", 2)
    iterator.hasNext shouldBe false
  }

  it should "test get" in {
    val emptyJournal = MapJournal.empty[String, Int]
    emptyJournal.get("a") shouldBe None
    val journal = emptyJournal.append(("a", 1))
    journal.get("a") shouldBe Some(1)
  }
}
