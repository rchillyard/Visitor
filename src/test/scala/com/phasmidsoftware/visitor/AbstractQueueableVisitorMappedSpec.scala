package com.phasmidsoftware.visitor

import com.phasmidsoftware.visitor.BfsPQVisitorMapped.{createMax, createMin}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

import scala.collection.immutable.Queue
import scala.util.{Success, Using}

/**
 * NOTE some of these tests appear to be duplicates.
 *
 * Provides a unit test specification for verifying the functionality of breadth-first search (BFS) with
 * queue and priority queue-based visitors, in combination with journal mappings such as `Pre` and `Post`.
 *
 * This class primarily tests BFS traversal on a predefined tree structure with `Queue` and `PriorityQueue`-based
 * visitor implementations (`BfsQueueVisitorMapped` and `BfsPQVisitorMapped`).
 * These visitors utilize journals to record traversal results for both `Pre` and `Post` mappings, validating correctness and order.
 *
 * The focus includes:
 * - BFS traversal with queue-based journal mappings (`Pre` and `Post`).
 * - BFS traversal using max-heap or min-heap priority queues (`Pre` and `Post`).
 * - Verification of traversal results against expected outcomes.
 * - Validation of journal mappings (`QueueJournal` and `AbstractMapJournal`) used to record traversal operations.
 *
 * Behavior tested:
 * - BFS traversal with a queue-based visitor using `Pre` and `Post` journal mappings.
 * - BFS traversal using a priority queue with max-heap or min-heap ordering for `Pre` and `Post` operations.
 * - Traversal consistency with respect to predefined tree nodes and operations.
 *
 * Predefined tree structure:
 * 10
 * 5--------13
 * 2---6---11---15
 * 1-3
 *
 * Key functionalities include:
 * - Traversal of the tree while capturing node entries in specific order.
 * - Use of journals to record visited nodes during traversal.
 * - Validation of traversal consistency and correctness.
 *
 * Test scenarios include:
 * - BFS traversal consistency with `Queue`-based visitor.
 * - BFS traversal order correctness with max-heap and min-heap priority queues.
 *
 * Note:
 * - Certain tests include TODO marks for further verification of expected results.
 * - Traversal order may vary for tests relying on map-to-sequence conversion.
 */
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

  behavior of "BfsQueueVisitorMapped with Queue Journal"

  it should "bfs pre queue" in {
    val fulfill: Option[Int] => Int => String = _ => y => y.toString
    val goal: Int => Boolean = _ => false
    val visitor = BfsQueueVisitorMapped(Queue.empty[Int], Map(Pre -> QueueJournal.empty[(Int, String)]), fulfill, children, goal)
    // Test a recursive pre-order traversal of the tree, starting at the root (skipping the starting vertex because it's a pre-visit).
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

  it should "bfs post queue" in {
    val fulfill: Option[Int] => Int => String = _ => y => y.toString
    val goal: Int => Boolean = _ => false
    val visitor = BfsQueueVisitorMapped(Queue.empty[Int], Map(Post -> QueueJournal.empty[(Int, String)]), fulfill, children, goal)
    // Test a recursive post-order traversal of the tree, starting at the root.
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

  it should "bfs PQ max Pre" in {
    val visitor: BfsPQVisitorMapped[Int, String] = createMax(Pre, x => x.toString, children, _ => false)
    // NOTE that the order of the result is essentially random (because it's taking a Map as a Seq).
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

  it should "bfs PQ min Pre" in {
    val visitor: BfsPQVisitorMapped[Int, String] = createMin(Pre, x => x.toString, children, _ => false)
    // NOTE that the order of the result is essentially random (because it's taking a Map as a Seq).
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

  it should "bfs PQ max Post" in {
    val visitor: BfsPQVisitorMapped[Int, String] = createMax(Post, x => x.toString, children, _ => false)
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

  it should "bfs PQ min Post" in {
    val visitor: BfsPQVisitorMapped[Int, String] = createMin(Post, x => x.toString, children, _ => false)
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

  behavior of "BfsQueueVisitorMapped with Map Journal"

  it should "bfs PQ max Pre" in {
    val visitor: BfsPQVisitorMapped[Int, String] = createMax(Pre, x => x.toString, children, _ => false)
    // NOTE that the order of the result is essentially random (because it's taking a Map as a Seq).
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

  it should "bfs PQ min Pre" in {
    val visitor: BfsPQVisitorMapped[Int, String] = createMin(Pre, x => x.toString, children, _ => false)
    // NOTE that the order of the result is essentially random (because it's taking a Map as a Seq).
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

  it should "bfs PQ max Post" in {
    val visitor: BfsPQVisitorMapped[Int, String] = createMax(Post, x => x.toString, children, _ => false)
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

  it should "bfs PQ min Post" in {
    val visitor: BfsPQVisitorMapped[Int, String] = createMin(Post, x => x.toString, children, _ => false)
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
