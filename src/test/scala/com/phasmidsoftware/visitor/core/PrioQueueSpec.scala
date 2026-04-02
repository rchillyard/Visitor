package com.phasmidsoftware.visitor.core

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

/**
  * Tests for [[PrioQueue]] — the priority queue ADT layer.
  *
  * PrioQueue delegates structural work to [[BinaryHeap]] and adds the
  * policy that duplicates are permitted. Tests focus on the ADT contract
  * (offer/take ordering, duplicates, min vs max) rather than heap mechanics,
  * which are covered in [[BinaryHeapSpec]].
  */
class PrioQueueSpec extends AnyFlatSpec with Matchers:

  // -----------------------------------------------------------------------
  // Construction
  // -----------------------------------------------------------------------

  "PrioQueue.empty" should "be empty" in:
    PrioQueue.empty[Int].isEmpty shouldBe true
    PrioQueue.empty[Int].size shouldBe 0

  it should "not be empty after offer" in:
    val pq = PrioQueue.empty[Int].offer(1)
    pq.isEmpty shouldBe false
    pq.size shouldBe 1

  // -----------------------------------------------------------------------
  // Min-priority (default)
  // -----------------------------------------------------------------------

  "PrioQueue (min)" should "dequeue elements in ascending order" in:
    val pq = PrioQueue.empty[Int].offer(3).offer(1).offer(2)
    val (a, pq1) = pq.take
    val (b, pq2) = pq1.take
    val (c, _)   = pq2.take
    List(a, b, c) shouldBe List(1, 2, 3)

  it should "expose the minimum via head without removing it" in:
    val pq = PrioQueue.empty[Int].offer(3).offer(1).offer(2)
    pq.head shouldBe 1
    pq.size shouldBe 3

  it should "dequeue a single element correctly" in:
    val pq = PrioQueue.empty[Int].offer(7)
    val (x, pq2) = pq.take
    x shouldBe 7
    pq2.isEmpty shouldBe true

  it should "dequeue in ascending order regardless of insertion order" in:
    val pq = PrioQueue.empty[Int].offer(5).offer(2).offer(8).offer(1).offer(9).offer(3)
    val result = Iterator.unfold(pq) { q =>
      if q.isEmpty then None else Some(q.take)
    }.toList
    result shouldBe List(1, 2, 3, 5, 8, 9)

  // -----------------------------------------------------------------------
  // Duplicates
  // -----------------------------------------------------------------------

  it should "permit duplicate elements" in:
    val pq = PrioQueue.empty[Int].offer(2).offer(1).offer(2).offer(1)
    pq.size shouldBe 4

  it should "dequeue duplicates in non-decreasing order" in:
    val pq = PrioQueue.empty[Int].offer(2).offer(1).offer(2).offer(1)
    val (a, pq1) = pq.take
    val (b, pq2) = pq1.take
    val (c, pq3) = pq2.take
    val (d, _)   = pq3.take
    List(a, b, c, d) shouldBe List(1, 1, 2, 2)

  it should "handle all identical elements" in:
    val pq = PrioQueue.empty[Int].offer(3).offer(3).offer(3)
    val (a, pq1) = pq.take
    val (b, pq2) = pq1.take
    val (c, _)   = pq2.take
    List(a, b, c) shouldBe List(3, 3, 3)

  // -----------------------------------------------------------------------
  // Max-priority
  // -----------------------------------------------------------------------

  "PrioQueue.emptyMax" should "dequeue elements in descending order" in:
    val pq = PrioQueue.emptyMax[Int].offer(1).offer(3).offer(2)
    val (a, pq1) = pq.take
    val (b, pq2) = pq1.take
    val (c, _)   = pq2.take
    List(a, b, c) shouldBe List(3, 2, 1)

  it should "expose the maximum via head without removing it" in:
    val pq = PrioQueue.emptyMax[Int].offer(1).offer(3).offer(2)
    pq.head shouldBe 3

  it should "dequeue duplicates in non-increasing order" in:
    val pq = PrioQueue.emptyMax[Int].offer(2).offer(1).offer(2).offer(1)
    val (a, pq1) = pq.take
    val (b, pq2) = pq1.take
    val (c, pq3) = pq2.take
    val (d, _)   = pq3.take
    List(a, b, c, d) shouldBe List(2, 2, 1, 1)

  // -----------------------------------------------------------------------
  // Frontier[PrioQueue] integration
  // -----------------------------------------------------------------------

  "Frontier[PrioQueue]" should "offer and take in ascending priority order" in:
    val fr = summon[Frontier[PrioQueue]]
    val pq = PrioQueue.empty[Int]
    val pq1 = fr.offer(fr.offer(fr.offer(pq)(3))(1))(2)
    val (a, pq2) = fr.take(pq1)
    val (b, pq3) = fr.take(pq2)
    val (c, _)   = fr.take(pq3)
    List(a, b, c) shouldBe List(1, 2, 3)

  it should "report isEmpty correctly" in:
    val fr  = summon[Frontier[PrioQueue]]
    val pq0 = PrioQueue.empty[Int]
    fr.isEmpty(pq0) shouldBe true
    val pq1 = fr.offer(pq0)(1)
    fr.isEmpty(pq1) shouldBe false
    val (_, pq2) = fr.take(pq1)
    fr.isEmpty(pq2) shouldBe true

  it should "permit duplicates via offerAll" in:
    val fr  = summon[Frontier[PrioQueue]]
    val pq  = PrioQueue.empty[Int]
    val pq1 = fr.offerAll(pq)(List(2, 1, 2, 1))
    pq1.size shouldBe 4