package com.phasmidsoftware.visitor.core

import com.phasmidsoftware.visitor.misc.{FunctionMapJournal, MapJournal}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

// ============================================================
// Shared tree fixture
//
//           10
//        5--------13
//      2---6---11---15
//     1-3
//
// BFS (level order):          10, 5, 13, 2, 6, 11, 15, 1, 3
// DFS pre-order:              10, 5, 2, 1, 3, 6, 13, 11, 15
// BFS min-priority order:     10, 5, 2, 1, 3, 6, 13, 11, 15
// BFS max-priority order:     10, 13, 15, 11, 5, 6, 2, 3, 1
// ============================================================

object TreeFixture:
  given GraphNeighbours[Int] with
    def neighbours(n: Int): Iterator[Int] = n match
      case 10 => Iterator(5, 13)
      case 5 => Iterator(2, 6)
      case 13 => Iterator(11, 15)
      case 2 => Iterator(1, 3)
      case _ => Iterator.empty

  given Evaluable[Int, Int] with
    def evaluate(v: Int): Option[Int] = Some(v)

// ============================================================
// BinaryHeap tests
// ============================================================

class OldBinaryHeapSpec extends AnyFlatSpec with Matchers:

  "BinaryHeap" should "be empty on construction" in :
    BinaryHeap.empty[Int].isEmpty shouldBe true

  it should "insert and removeMin a single element" in :
    val h = BinaryHeap.empty[Int].insert(42)
    h.isEmpty shouldBe false
    val (x, h2) = h.removeMin
    x shouldBe 42
    h2.isEmpty shouldBe true

  it should "always removeMin in ascending order" in :
    val h = BinaryHeap.empty[Int].insert(3).insert(1).insert(4).insert(1).insert(5)
    val (a, h1) = h.removeMin
    val (b, h2) = h1.removeMin
    val (c, h3) = h2.removeMin
    val (d, h4) = h3.removeMin
    val (e, _) = h4.removeMin
    List(a, b, c, d, e) shouldBe List(1, 1, 3, 4, 5)

  it should "maintain heap invariant when root is not the smallest child" in :
    // This specifically catches the removeMin bug (data.tail.tail vs data.tail.init)
    val h = BinaryHeap.empty[Int].insert(1).insert(5).insert(2).insert(7).insert(6).insert(3)
    val (a, h1) = h.removeMin
    val (b, h2) = h1.removeMin
    val (c, _) = h2.removeMin
    a shouldBe 1
    b shouldBe 2
    c shouldBe 3

  it should "work as a max-heap when given a reversed Ordering" in :

    given Ordering[Int] = Ordering.Int.reverse

    val h = BinaryHeap.empty[Int].insert(3).insert(1).insert(2)
    val (a, h1) = h.removeMin // "min" of reversed = actual max
    val (b, h2) = h1.removeMin
    val (c, _) = h2.removeMin
    List(a, b, c) shouldBe List(3, 2, 1)

// ============================================================
// PrioQueue (min and max) tests
// ============================================================

class OldPrioQueueSpec extends AnyFlatSpec with Matchers:

  // --- Min ---

  "PrioQueue.empty (min)" should "be empty" in :
    PrioQueue.empty[Int].isEmpty shouldBe true

  it should "throw on take from empty queue" in :
    a[IllegalArgumentException] shouldBe thrownBy(PrioQueue.empty[Int].take)

  it should "offer and take a single element" in :
    val pq = PrioQueue.empty[Int].offer(1)
    pq.isEmpty shouldBe false
    val (x, pq2) = pq.take
    x shouldBe 1
    pq2.isEmpty shouldBe true

  it should "dequeue elements in ascending order" in :
    val pq = PrioQueue.empty[Int].offer(3).offer(1).offer(2)
    val (a, pq1) = pq.take
    val (b, pq2) = pq1.take
    val (c, _) = pq2.take
    List(a, b, c) shouldBe List(1, 2, 3)

  it should "handle duplicate elements" in :
    val pq = PrioQueue.empty[Int].offer(2).offer(1).offer(2).offer(1)
    val (a, pq1) = pq.take
    val (b, pq2) = pq1.take
    val (c, pq3) = pq2.take
    val (d, _) = pq3.take
    List(a, b, c, d) shouldBe List(1, 1, 2, 2)

  // --- Max ---

  "PrioQueue.emptyMax (max)" should "be empty" in :
    PrioQueue.emptyMax[Int].isEmpty shouldBe true

  it should "throw on take from empty queue" in :
    a[IllegalArgumentException] shouldBe thrownBy(PrioQueue.emptyMax[Int].take)

  it should "offer and take a single element" in :
    val pq = PrioQueue.emptyMax[Int].offer(1)
    pq.isEmpty shouldBe false
    val (x, pq2) = pq.take
    x shouldBe 1
    pq2.isEmpty shouldBe true

  it should "dequeue elements in descending order" in :
    val pq = PrioQueue.emptyMax[Int].offer(3).offer(1).offer(2)
    val (a, pq1) = pq.take
    val (b, pq2) = pq1.take
    val (c, _) = pq2.take
    List(a, b, c) shouldBe List(3, 2, 1)

// ============================================================
// BFS tests
// ============================================================

class BfsTranslatedSpec extends AnyFlatSpec with Matchers:

  import TreeFixture.given

  "Traversal.bfs" should "traverse in level order" in :
    val visitor = JournaledVisitor.withQueueJournal[Int, Int]
    val result = Traversal.bfs(10, visitor)
    result.result.map(_._1).toList shouldBe List(10, 5, 13, 2, 6, 11, 15, 1, 3)

  it should "record the correct evaluated values" in :
    val visitor = JournaledVisitor.withQueueJournal[Int, Int]
    val result = Traversal.bfs(10, visitor)
    result.result.toList.map(_._2) shouldBe
      List(10, 5, 13, 2, 6, 11, 15, 1, 3).map(Some(_))

// ============================================================
// DFS tests
// ============================================================

class DfsTranslatedSpec extends AnyFlatSpec with Matchers:

  import TreeFixture.given

  "Traversal.dfs" should "traverse in pre-order" in :
    val visitor = JournaledVisitor.withQueueJournal[Int, Int]
    val result = Traversal.dfs(10, visitor)
    result.result.map(_._1).toList shouldBe List(10, 5, 2, 1, 3, 6, 13, 11, 15)

  it should "visit all nodes exactly once" in :
    val visitor = JournaledVisitor.withQueueJournal[Int, Int]
    val result = Traversal.dfs(10, visitor)
    val visited = result.result.map(_._1).toList
    visited.size shouldBe 9
    visited.distinct.size shouldBe 9

// ============================================================
// Best-first (min priority) tests
// ============================================================

class BestFirstTranslatedSpec extends AnyFlatSpec with Matchers:

  import TreeFixture.given

  "Traversal.bestFirst (min)" should "traverse in min-priority order" in :
    val visitor = JournaledVisitor.withQueueJournal[Int, Int]
    val result = Traversal.bestFirst(10, visitor)
    result.result.map(_._1).toList shouldBe List(10, 5, 2, 1, 3, 6, 13, 11, 15)

  "Traversal.bestFirstMax (max)" should "traverse in max-priority order" in :
    val visitor = JournaledVisitor.withQueueJournal[Int, Int]
    val result = Traversal.bestFirstMax(10, visitor)
    result.result.map(_._1).toList shouldBe List(10, 13, 15, 11, 5, 6, 2, 3, 1)

// ============================================================
// MapJournal tests (translated directly — no change needed)
// ============================================================

class MapJournalTranslatedSpec extends AnyFlatSpec with Matchers:

  "MapJournal" should "append and retrieve by key" in :
    val j = MapJournal.empty[String, Int].append("a" -> 1).append("b" -> 2)
    j.get("a") shouldBe Some(1)
    j.get("b") shouldBe Some(2)
    j.get("c") shouldBe None

  it should "return all keys" in :
    val j = MapJournal.empty[String, Int].append("a" -> 1).append("b" -> 2)
    j.keys.toSet shouldBe Set("a", "b")

  it should "return a map" in :
    val j = MapJournal.empty[String, Int].append("1" -> 1).append("2" -> 2)
    j.entries.toMap shouldBe Map("1" -> 1, "2" -> 2)

  "FunctionMapJournal" should "appendByFunction and retrieve" in :
    val f: String => Int = _.toInt
    val j = FunctionMapJournal.empty[String, Int](f)
      .appendByFunction("1").appendByFunction("2")
    j.get("1") shouldBe Some(1)
    j.get("2") shouldBe Some(2)

  it should "return None for missing keys" in :
    val j = FunctionMapJournal.empty[String, String](identity)
    j.get("a") shouldBe None
    j.append("a" -> "a").get("a") shouldBe Some("a")

// ============================================================
// Goal predicate tests
// ============================================================

class GoalPredicateSpec extends AnyFlatSpec with Matchers:

  import TreeFixture.given

  "Traversal.bfs with goal" should "stop after recording the goal node" in :
    val visitor = JournaledVisitor.withQueueJournal[Int, Int]
    // BFS from 10, stop when we reach 6
    val result = Traversal.bfs(10, visitor, goal = _ == 6)
    // Level order: 10, then 5 and 13, then 2 and 6 — stops at 6
    result.result.map(_._1).toList shouldBe List(10, 5, 13, 2, 6)

  it should "include the goal node in the journal" in :
    val visitor = JournaledVisitor.withQueueJournal[Int, Int]
    val result = Traversal.bfs(10, visitor, goal = _ == 13)
    result.result.map(_._1).toList should contain(13)

  it should "not expand children of the goal node" in :
    val visitor = JournaledVisitor.withQueueJournal[Int, Int]
    val result = Traversal.bfs(10, visitor, goal = _ == 13)
    // 11 and 15 are children of 13 — should not appear
    result.result.map(_._1).toList should contain noneOf(11, 15)

  it should "traverse the whole graph when goal is never met" in :
    val visitor = JournaledVisitor.withQueueJournal[Int, Int]
    val result = Traversal.bfs(10, visitor, goal = _ == 99)
    result.result.map(_._1).toSet shouldBe Set(10, 5, 13, 2, 6, 11, 15, 1, 3)

  it should "stop immediately when start node matches goal" in :
    val visitor = JournaledVisitor.withQueueJournal[Int, Int]
    val result = Traversal.bfs(10, visitor, goal = _ == 10)
    result.result.map(_._1).toList shouldBe List(10)

  "Traversal.dfs with goal" should "stop after recording the goal node" in :
    val visitor = JournaledVisitor.withQueueJournal[Int, Int]
    // DFS pre-order from 10, stop when we reach 3
    val result = Traversal.dfs(10, visitor, goal = _ == 3)
    // Pre-order: 10, 5, 2, 1, 3 — stops at 3
    result.result.map(_._1).toList shouldBe List(10, 5, 2, 1, 3)

  it should "not expand children of the goal node" in :
    val visitor = JournaledVisitor.withQueueJournal[Int, Int]
    val result = Traversal.dfs(10, visitor, goal = _ == 5)
    // 5's children are 2 and 6 — should not appear
    result.result.map(_._1).toList should contain noneOf(2, 6)

  "Traversal.bestFirst with goal" should "stop after recording the goal node" in :
    val visitor = JournaledVisitor.withQueueJournal[Int, Int]
    // bestFirst from 10, stop when we reach 5
    val result = Traversal.bestFirst(10, visitor, goal = _ == 5)
    result.result.map(_._1).toList should contain(5)
    // Should not have visited children of 5 (2 and 6)
    result.result.map(_._1).toList should contain noneOf(2, 6)