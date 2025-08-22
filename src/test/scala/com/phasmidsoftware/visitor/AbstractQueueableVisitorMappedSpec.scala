package com.phasmidsoftware.visitor

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

import scala.collection.immutable.Queue
import scala.util.{Success, Using}

class AbstractQueueableVisitorMappedSpec extends AnyFlatSpec with should.Matchers {

  // Define a tree similar to a binary heap starting at slot 1
  //           10
  //        5--------13
  //      2---6---11---15
  //     1-3
  private val tree = Seq(-99, 10, 5, 13, 2, 6, 11, 15, 1, 3)

  // Define a function that gets the children of the input value `i`.
  def children(i: Int): Seq[Int] = {
    val twiceIndex = 2 * tree.indexOf(i)
    Seq(twiceIndex, twiceIndex + 1).filter(x => x > 0 && x < tree.length).map(tree)
  }

  behavior of "BfsQueueVisitorMapped"

  it should "bfs" in {
    val fulfill: Option[Int] => Int => String = _ => y => y.toString
    val goal: Int => Boolean = _ => false
    val visitor = BfsQueueVisitorMapped(Queue.empty[Int], Map(Post -> QueueJournal.empty[(Int, String)]), fulfill, children, goal)
    // Test a recursive pre-order traversal of the tree, starting at the root.
    val expected = Success(List(10 -> "10", 5 -> "5", 13 -> "13", 2 -> "2", 6 -> "6", 11 -> "11", 15 -> "15", 1 -> "1", 3 -> "3"))
    val actual = Using(visitor) {
      visitor =>
        val (v, _) = visitor.bfs(10)
        for {journal <- v.iterableJournals
             entry <- journal
             } yield entry
    }
    actual shouldBe expected
  }

  it should "create" in {
  }

  behavior of "BfsMinPQVisitorMapped"

  it should "bfs" in {
    val visitor: BfsPQVisitorMapped[Int, String] = BfsPQVisitorMapped.createMax(x => x.toString, children, _ => false)
    // TODO check that this is the correct expected result--it seems wrong.
    val expected = Success(List((5, "5"), (10, "10"), (1, "1"), (6, "6"), (13, "13"), (2, "2"), (3, "3"), (11, "11"), (15, "15")))
    val actual = Using(visitor) {
      visitor =>
        val (v, _) = visitor.bfs(10)
        for {journal: AbstractMapJournal[Int, String] <- v.mapJournals
             entry <- journal.entries
             } yield entry
    }
    actual shouldBe expected
  }

  behavior of "BfsPQVisitorMapped"

  it should "bfs" in {
    val visitor: BfsPQVisitorMapped[Int, String] = BfsPQVisitorMapped.createMax(x => x.toString, children, _ => false)
    // TODO check that this is the correct expected result--it seems wrong.
    val expected = Success(List((5, "5"), (10, "10"), (1, "1"), (6, "6"), (13, "13"), (2, "2"), (3, "3"), (11, "11"), (15, "15")))
    val actual = Using(visitor) {
      visitor =>
        val (v, _) = visitor.bfs(10)
        for {journal: AbstractMapJournal[Int, String] <- v.mapJournals
             entry <- journal.entries
             } yield entry
    }
    actual shouldBe expected
  }


}
