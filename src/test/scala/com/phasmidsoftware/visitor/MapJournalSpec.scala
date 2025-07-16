package com.phasmidsoftware.visitor

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import org.scalatest.matchers.should.Matchers.shouldBe

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

  it should "test map" in {
    val emptyJournal = MapJournal.empty[String, Int]
    val journal: MapJournal[String, Int] = emptyJournal.append("1" -> 1).append("2" -> 2)
    val map: Map[String, Int] = journal.map
    map shouldBe Map("1" -> 1, "2" -> 2)
  }

  it should "test get" in {
    val emptyJournal = MapJournal.empty[String, Int]
    emptyJournal.get("a") shouldBe None
    val journal = emptyJournal.append(("a", 1))
    journal.get("a") shouldBe Some(1)
  }

  behavior of "FunctionMapMappedJournal"

  it should "test Iterator" in {
    val f: String => Int = s => s.toInt
    val emptyJournal = FunctionMapMappedJournal.empty[String, Int](f)
    emptyJournal.iterator.isEmpty shouldBe true
    val journal = emptyJournal.appendByFunction("1").appendByFunction("2")
    val iterator = journal.iterator
    iterator.hasNext shouldBe true
    iterator.next shouldBe("1", 1)
    iterator.hasNext shouldBe true
    iterator.next shouldBe("2", 2)
    iterator.hasNext shouldBe false
  }

  it should "test get" in {
    val emptyJournal = FunctionMapMappedJournal.empty[String, String](identity)
    emptyJournal.get("a") shouldBe None
    val journal = emptyJournal.append(("a", "a"))
    journal.get("a") shouldBe Some("a")
  }
}
