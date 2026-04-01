package com.phasmidsoftware.visitor.core

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

/**
  * Tests for [[BinaryHeap]] — the pure data structure layer.
  *
  * BinaryHeap is private[core] so these tests are in the same package.
  * Tests focus on structural correctness: heap invariant, insert, removeMin,
  * and duplicate handling. Policy concerns (deduplication, decreaseKey) are
  * tested in [[IndexedPrioQueueSpec]].
  */
class BinaryHeapSpec extends AnyFlatSpec with Matchers:

  // -----------------------------------------------------------------------
  // Construction
  // -----------------------------------------------------------------------

  "BinaryHeap" should "be empty on construction" in:
    val h = BinaryHeap.empty[Int]
    h.isEmpty shouldBe true
    h.size shouldBe 0

  it should "not be empty after a single insert" in:
    val h = BinaryHeap.empty[Int].insert(1)
    h.isEmpty shouldBe false
    h.size shouldBe 1

  it should "expose the minimum element via head without removing it" in:
    val h = BinaryHeap.empty[Int].insert(3).insert(1).insert(2)
    h.head shouldBe 1
    h.size shouldBe 3  // head is non-destructive

  it should "throw on head of empty heap" in:
    an[IllegalArgumentException] should be thrownBy BinaryHeap.empty[Int].head

  it should "throw on removeMin of empty heap" in:
    an[IllegalArgumentException] should be thrownBy BinaryHeap.empty[Int].removeMin

  // -----------------------------------------------------------------------
  // Insert and removeMin — ordering
  // -----------------------------------------------------------------------

  it should "removeMin a single element correctly" in:
    val h = BinaryHeap.empty[Int].insert(42)
    val (x, h2) = h.removeMin
    x shouldBe 42
    h2.isEmpty shouldBe true

  it should "always dequeue in ascending order regardless of insertion order" in:
    val h = BinaryHeap.empty[Int].insert(5).insert(3).insert(1).insert(4).insert(2)
    val (a, h1) = h.removeMin
    val (b, h2) = h1.removeMin
    val (c, h3) = h2.removeMin
    val (d, h4) = h3.removeMin
    val (e, _)  = h4.removeMin
    List(a, b, c, d, e) shouldBe List(1, 2, 3, 4, 5)

  it should "dequeue in ascending order when inserted in ascending order" in:
    val h = BinaryHeap.empty[Int].insert(1).insert(2).insert(3)
    val (a, h1) = h.removeMin
    val (b, h2) = h1.removeMin
    val (c, _)  = h2.removeMin
    List(a, b, c) shouldBe List(1, 2, 3)

  it should "dequeue in ascending order when inserted in descending order" in:
    val h = BinaryHeap.empty[Int].insert(3).insert(2).insert(1)
    val (a, h1) = h.removeMin
    val (b, h2) = h1.removeMin
    val (c, _)  = h2.removeMin
    List(a, b, c) shouldBe List(1, 2, 3)

  it should "maintain heap invariant after each removeMin on a larger heap" in:
    val elems = List(7, 2, 9, 1, 5, 3, 8, 4, 6)
    val h     = elems.foldLeft(BinaryHeap.empty[Int])(_.insert(_))
    val result = Iterator.unfold(h) { heap =>
      if heap.isEmpty then None
      else Some(heap.removeMin)
    }.toList
    result shouldBe elems.sorted

  // -----------------------------------------------------------------------
  // Duplicates
  // -----------------------------------------------------------------------

  it should "permit duplicate elements" in:
    val h = BinaryHeap.empty[Int].insert(2).insert(1).insert(2).insert(1)
    h.size shouldBe 4

  it should "dequeue duplicates in non-decreasing order" in:
    val h = BinaryHeap.empty[Int].insert(2).insert(1).insert(2).insert(1)
    val (a, h1) = h.removeMin
    val (b, h2) = h1.removeMin
    val (c, h3) = h2.removeMin
    val (d, _)  = h3.removeMin
    List(a, b, c, d) shouldBe List(1, 1, 2, 2)

  it should "handle a heap of all identical elements" in:
    val h = BinaryHeap.empty[Int].insert(5).insert(5).insert(5)
    val (a, h1) = h.removeMin
    val (b, h2) = h1.removeMin
    val (c, _)  = h2.removeMin
    List(a, b, c) shouldBe List(5, 5, 5)

  // -----------------------------------------------------------------------
  // Ordering variants
  // -----------------------------------------------------------------------

  it should "work as a max-heap when given a reversed Ordering" in:
    given Ordering[Int] = Ordering.Int.reverse
    val h = BinaryHeap.empty[Int].insert(1).insert(3).insert(2)
    val (a, h1) = h.removeMin  // removeMin on reversed ordering = removeMax
    val (b, h2) = h1.removeMin
    val (c, _)  = h2.removeMin
    List(a, b, c) shouldBe List(3, 2, 1)

  it should "work correctly with a String ordering" in:
    val h = BinaryHeap.empty[String].insert("banana").insert("apple").insert("cherry")
    val (a, h1) = h.removeMin
    val (b, h2) = h1.removeMin
    val (c, _)  = h2.removeMin
    List(a, b, c) shouldBe List("apple", "banana", "cherry")