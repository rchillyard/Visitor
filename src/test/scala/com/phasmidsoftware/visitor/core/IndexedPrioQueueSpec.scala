package com.phasmidsoftware.visitor.core

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

/**
  * Tests for [[IndexedPrioQueue]] — the indexed priority queue ADT layer.
  *
  * IndexedPrioQueue extends the [[PrioQueue]] contract with:
  *   - deduplication on `offer` (one entry per key)
  *   - `decreaseKey` — O(n log n) re-prioritisation of an existing entry
  *   - `contains` — O(1) membership test
  *
  * These are the operations required by [[CostUpdate]] in Dijkstra/Prim traversals.
  */
class IndexedPrioQueueSpec extends AnyFlatSpec with Matchers:

  // -----------------------------------------------------------------------
  // Construction
  // -----------------------------------------------------------------------

  "IndexedPrioQueue.empty" should "be empty" in:
    IndexedPrioQueue.empty[Int].isEmpty shouldBe true
    IndexedPrioQueue.empty[Int].size shouldBe 0

  it should "not be empty after offer" in:
    val pq = IndexedPrioQueue.empty[Int].offer(1)
    pq.isEmpty shouldBe false
    pq.size shouldBe 1

  // -----------------------------------------------------------------------
  // Offer / take ordering
  // -----------------------------------------------------------------------

  "IndexedPrioQueue (min)" should "dequeue elements in ascending order" in:
    val pq = IndexedPrioQueue.empty[Int].offer(3).offer(1).offer(2)
    val (a, pq1) = pq.take
    val (b, pq2) = pq1.take
    val (c, _)   = pq2.take
    List(a, b, c) shouldBe List(1, 2, 3)

  it should "expose the minimum via head without removing it" in:
    val pq = IndexedPrioQueue.empty[Int].offer(3).offer(1).offer(2)
    pq.head shouldBe 1
    pq.size shouldBe 3

  it should "dequeue a single element correctly" in:
    val pq = IndexedPrioQueue.empty[Int].offer(7)
    val (x, pq2) = pq.take
    x shouldBe 7
    pq2.isEmpty shouldBe true

  it should "dequeue in ascending order regardless of insertion order" in:
    val pq = IndexedPrioQueue.empty[Int].offer(5).offer(2).offer(8).offer(1).offer(9).offer(3)
    val result = Iterator.unfold(pq) { q =>
      if q.isEmpty then None else Some(q.take)
    }.toList
    result shouldBe List(1, 2, 3, 5, 8, 9)

  // -----------------------------------------------------------------------
  // Deduplication — one entry per key
  // -----------------------------------------------------------------------

  it should "treat duplicate offer as a no-op" in:
    val pq  = IndexedPrioQueue.empty[Int].offer(1).offer(2)
    val pq2 = pq.offer(1)  // duplicate — should be ignored
    pq2.size shouldBe 2

  it should "contain the element after offer, not after take" in:
    val pq = IndexedPrioQueue.empty[Int].offer(1).offer(2)
    pq.contains(1) shouldBe true
    pq.contains(2) shouldBe true
    pq.contains(3) shouldBe false
    val (_, pq2) = pq.take  // removes 1
    pq2.contains(1) shouldBe false
    pq2.contains(2) shouldBe true

  it should "not grow when the same element is offered repeatedly" in:
    val pq = (1 to 5).foldLeft(IndexedPrioQueue.empty[Int].offer(42)) { (q, _) => q.offer(42) }
    pq.size shouldBe 1

  // -----------------------------------------------------------------------
  // decreaseKey
  // -----------------------------------------------------------------------

  it should "decreaseKey repositions an existing entry so it dequeues earlier" in:
    // Start: [3, 5, 7] — decrease 7 to 1
    val pq  = IndexedPrioQueue.empty[Int].offer(3).offer(5).offer(7)
    val pq2 = pq.decreaseKey(7, 1)
    val (a, pq3) = pq2.take
    val (b, pq4) = pq3.take
    val (c, _)   = pq4.take
    List(a, b, c) shouldBe List(1, 3, 5)

  it should "decreaseKey on the current minimum has no visible effect on order" in:
    val pq  = IndexedPrioQueue.empty[Int].offer(1).offer(3).offer(5)
    val pq2 = pq.decreaseKey(1, 0)
    val (a, pq3) = pq2.take
    val (b, pq4) = pq3.take
    val (c, _)   = pq4.take
    List(a, b, c) shouldBe List(0, 3, 5)

  it should "decreaseKey on an absent element is a no-op" in:
    val pq  = IndexedPrioQueue.empty[Int].offer(1).offer(2)
    val pq2 = pq.decreaseKey(99, 0)  // 99 not present
    pq2.size shouldBe 2
    pq2.contains(99) shouldBe false
    pq2.contains(0)  shouldBe false

  it should "decreaseKey with a non-improving priority is a no-op" in:
    val pq  = IndexedPrioQueue.empty[Int].offer(1).offer(3)
    val pq2 = pq.decreaseKey(1, 5)  // 5 > 1 — not an improvement
    pq2.size shouldBe 2
    val (a, _) = pq2.take
    a shouldBe 1  // original value still at head

  it should "decreaseKey preserves all other elements" in:
    val pq  = IndexedPrioQueue.empty[Int].offer(10).offer(20).offer(30).offer(40)
    val pq2 = pq.decreaseKey(40, 5)
    val result = Iterator.unfold(pq2) { q =>
      if q.isEmpty then None else Some(q.take)
    }.toList
    result shouldBe List(5, 10, 20, 30)

  it should "support multiple decreaseKey calls on different elements" in:
    val pq  = IndexedPrioQueue.empty[Int].offer(10).offer(20).offer(30)
    val pq2 = pq.decreaseKey(30, 5).decreaseKey(20, 1)
    val result = Iterator.unfold(pq2) { q =>
      if q.isEmpty then None else Some(q.take)
    }.toList
    result shouldBe List(1, 5, 10)

  // -----------------------------------------------------------------------
  // Max-priority variant
  // -----------------------------------------------------------------------

  "IndexedPrioQueue.emptyMax" should "dequeue elements in descending order" in:
    val pq = IndexedPrioQueue.emptyMax[Int].offer(1).offer(3).offer(2)
    val (a, pq1) = pq.take
    val (b, pq2) = pq1.take
    val (c, _)   = pq2.take
    List(a, b, c) shouldBe List(3, 2, 1)

  it should "treat duplicate offer as a no-op" in:
    val pq  = IndexedPrioQueue.emptyMax[Int].offer(1).offer(2)
    val pq2 = pq.offer(2)
    pq2.size shouldBe 2

  // -----------------------------------------------------------------------
  // Frontier[IndexedPrioQueue] integration
  // -----------------------------------------------------------------------

  "Frontier[IndexedPrioQueue]" should "offer and take in ascending priority order" in:
    val fr  = summon[Frontier[IndexedPrioQueue]]
    val pq  = IndexedPrioQueue.empty[Int]
    val pq1 = fr.offer(fr.offer(fr.offer(pq)(3))(1))(2)
    val (a, pq2) = fr.take(pq1)
    val (b, pq3) = fr.take(pq2)
    val (c, _)   = fr.take(pq3)
    List(a, b, c) shouldBe List(1, 2, 3)

  it should "report isEmpty correctly" in:
    val fr  = summon[Frontier[IndexedPrioQueue]]
    val pq0 = IndexedPrioQueue.empty[Int]
    fr.isEmpty(pq0) shouldBe true
    val pq1 = fr.offer(pq0)(1)
    fr.isEmpty(pq1) shouldBe false
    val (_, pq2) = fr.take(pq1)
    fr.isEmpty(pq2) shouldBe true

  it should "deduplicate via offerAll" in:
    val fr  = summon[Frontier[IndexedPrioQueue]]
    val pq  = IndexedPrioQueue.empty[Int]
    val pq1 = fr.offerAll(pq)(List(1, 2, 1, 3))  // 1 appears twice
    pq1.size shouldBe 3

  // -----------------------------------------------------------------------
  // TupleVisitedSet — for (E, V) weighted frontiers
  // -----------------------------------------------------------------------

  "TupleVisitedSet" should "track visited-ness on the vertex component only" in:
    val vs: VisitedSet[(Int, String)] = summon[VisitedSet[(Int, String)]]
    vs.isVisited((1, "a")) shouldBe false
    val vs2 = vs.markVisited((1, "a"))
    vs2.isVisited((1, "a")) shouldBe true
    // Same vertex, different cost — still counts as visited
    vs2.isVisited((99, "a")) shouldBe true
    // Different vertex — not visited
    vs2.isVisited((1, "b")) shouldBe false