package com.phasmidsoftware.visitor

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

import scala.collection.immutable.Queue

class QueueJournalSpec extends AnyFlatSpec with should.Matchers {

  behavior of "QueueJournal"

  it should "test Iterator" in {
    val emptyJournal = QueueJournal.empty[String]
    emptyJournal.iterator.isEmpty shouldBe true
    val journal = emptyJournal.append("a").append("b")
    val iterator = journal.iterator
    iterator.hasNext shouldBe true
    iterator.next shouldBe "a"
    iterator.hasNext shouldBe true
    iterator.next shouldBe "b"
    iterator.hasNext shouldBe false
  }

  it should "test queue" in {
    val emptyJournal = QueueJournal.empty[String]
    val journal = emptyJournal.append("a").append("b")
    val queue = journal.queue
    queue shouldBe Queue("a", "b")
  }
}
