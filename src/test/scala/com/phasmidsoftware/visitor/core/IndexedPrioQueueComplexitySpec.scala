package com.phasmidsoftware.visitor.core

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

/**
  * Does `IndexedPrioQueue` actually meet the complexity its scaladoc claims? Issue #32.
  *
  * ==Why counting, and not timing==
  *
  * The obvious test is a stopwatch, and it is the wrong one: wall-clock assertions are flaky on
  * shared CI hardware and fail for reasons that have nothing to do with this code. These tests
  * count `Ordering.compare` calls instead, through an instrumented ordering. That is exact,
  * deterministic, and independent of the machine — the same numbers on a laptop and in a container.
  *
  * ==Why it is worth testing at all==
  *
  * Because the scaladoc was wrong for several releases and nothing noticed. It advertised
  * "O(log n) `decreaseKey`"; the implementation removed the element by patching the array, rebuilt
  * the entire heap by folding `insert` over every remaining element, and then rebuilt the index
  * from the finished array — O(n log n), with `offer` and `take` each paying an O(n) index rebuild
  * besides.
  *
  * Nothing failed, because every answer was correct. It surfaced only when Gryphon was first run
  * on a 1000-vertex graph and Prim took 2.3 minutes where Kruskal took 67 ms
  * (rchillyard/Gryphon#164). A complexity claim that no test checks is a comment, and comments do
  * not fail the build.
  *
  * ==Reading the assertions==
  *
  * Each test compares the work done at two sizes a factor of sixteen apart. Logarithmic growth
  * over that range is a factor of about 1.4; linear growth would be 16. The bounds are set loosely
  * between the two, so they cannot flake, and cannot pass if anyone reintroduces a heap rebuild.
  *
  * Checked against the old implementation before being trusted: `decreaseKey` there measures
  * 15.4, against a bound of 4. A guard that cannot fail is not a guard.
  *
  * ==What these tests do NOT cover, which is worth knowing==
  *
  * Counting comparisons sees work done through the `Ordering` and nothing else. It therefore
  * catches a rebuilt HEAP — folding `insert` over every element compares at every level — but it
  * is blind to a rebuilt INDEX, because `data.zipWithIndex.toMap` never consults the ordering.
  *
  * That is not hypothetical: on the pre-1.9.1 implementation these five tests fail exactly once,
  * on `decreaseKey`. The `offer` and `take` tests pass there, even though both were O(n), because
  * all of that cost was the index rebuild. So read them as asserting that the heap side stays
  * logarithmic, and not as a complete guarantee about either method.
  *
  * Closing that gap would mean counting Map operations, which means wrapping the index — a lot of
  * machinery for a case the `decreaseKey` test already fails on. Recorded rather than fixed, so
  * that nobody later reads a green run here as proof that `offer` is cheap.
  */
class IndexedPrioQueueComplexitySpec extends AnyFlatSpec with Matchers:

  /** An Ordering that counts how often it is consulted. */
  private class Counting[T](underlying: Ordering[T]) extends Ordering[T]:
    var count: Int = 0

    def compare(x: T, y: T): Int =
      count += 1
      underlying.compare(x, y)

  /** Fills a queue with `n` entries, then counts the comparisons ONE further operation costs. */
  private def comparisonsFor(n: Int)(op: (IndexedPrioQueue[Int], Counting[Int]) => Any): Int =
    val counting = new Counting(Ordering[Int])
    val q = (0 until n).foldLeft(IndexedPrioQueue.empty[Int](using counting))((acc, i) => acc.offer(i * 2))
    counting.count = 0 // discount the fill; measure only the operation under test
    val _ = op(q, counting)
    counting.count

  behavior of "decreaseKey"

  // The operation the whole issue is about. Lowering one key must not consult the ordering a
  // number of times proportional to the queue size.
  it should "cost logarithmically many comparisons, not linearly many" in {
    val small = comparisonsFor(128)((q, _) => q.decreaseKey(254, 1))
    val large = comparisonsFor(2048)((q, _) => q.decreaseKey(4094, 1))

    // Sixteen times the queue. Logarithmic is about 1.4x; linear would be 16x.
    large.toDouble / math.max(small, 1) should be < 4.0

    // And an absolute bound, so this still means something if both grow together: lowering a key
    // to the minimum sifts it to the root, which is one comparison per level.
    large should be < 64
  }

  it should "cost nothing beyond the lookup when the new key is not an improvement" in {
    // The early return. One comparison decides it, whatever the size.
    comparisonsFor(2048)((q, _) => q.decreaseKey(100, 1000)) should be <= 2
  }

  behavior of "offer and take"

  // NOTE: this does not catch an index rebuild -- see the class scaladoc. It asserts that the
  // heap side of `offer` stays logarithmic, which is what the sift does.
  it should "offer in logarithmically many comparisons" in {
    val small = comparisonsFor(128)((q, _) => q.offer(-1))
    val large = comparisonsFor(2048)((q, _) => q.offer(-1))
    large.toDouble / math.max(small, 1) should be < 4.0
    large should be < 64
  }

  it should "take in logarithmically many comparisons" in {
    val small = comparisonsFor(128)((q, _) => q.take)
    val large = comparisonsFor(2048)((q, _) => q.take)
    large.toDouble / math.max(small, 1) should be < 4.0
    large should be < 64
  }

  behavior of "the index"

  // The incremental repair is only safe if it leaves the index agreeing with the heap for EVERY
  // element, not just the ones that moved. A stale entry would make `contains` lie and
  // `decreaseKey` write to the wrong position -- which is exactly the bug the first draft of the
  // fix had, caught because a swap changes two positions and only one was being recorded.
  it should "agree with the heap after a mixed workload" in {
    val r = new scala.util.Random(42)
    var q = IndexedPrioQueue.empty[Int]
    var present = Set.empty[Int]
    for i <- 0 until 400 do
      q = q.offer(i * 10); present += i * 10
    for _ <- 0 until 400 do
      r.nextInt(3) match
        case 0 if q.size > 1 =>
          val (t, q2) = q.take
          q = q2; present -= t
        case 1 =>
          val v = r.nextInt(10000)
          if !present.contains(v) then
            q = q.offer(v); present += v
        case _ =>
          val existing = present.toSeq(r.nextInt(present.size))
          if existing > 0 then
            val lowered = existing - 1
            if !present.contains(lowered) then
              q = q.decreaseKey(existing, lowered)
              present = present - existing + lowered

      // Every element the queue claims to hold must be findable, and the heap must hold exactly
      // the same set.
      q.size shouldBe present.size
      present.foreach(v => q.contains(v) shouldBe true)

    // Draining must yield them in order: the ultimate check that the heap was never corrupted.
    val drained = Iterator.unfold(q)(qq => Option.when(!qq.isEmpty)(qq.take)).toList
    drained shouldBe drained.sorted
    drained.toSet shouldBe present
  }
