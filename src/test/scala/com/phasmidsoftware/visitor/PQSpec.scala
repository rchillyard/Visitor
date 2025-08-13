package com.phasmidsoftware.visitor

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

class PQSpec extends AnyFlatSpec with should.Matchers {

  behavior of "MinPQ"

  it should "isEmpty" in {
    MinPQ[Int]().isEmpty shouldBe true
  }

  it should "take" in {
    a[NoSuchElementException] shouldBe thrownBy(MinPQ[Int]().take)
  }

  it should "append/take 1" in {
    val pq: PQ[Int] = MinPQ[Int]().append(1)
    pq.isEmpty shouldBe false
    val (x, pqDash) = pq.take
    pqDash.isEmpty shouldBe true
    x shouldBe 1
  }

  it should "append/take 2" in {
    val pq: PQ[Int] = MinPQ[Int]().append(1).append(2)
    pq.isEmpty shouldBe false
    val (x1, pqDash) = pq.take
    pqDash.isEmpty shouldBe false
    x1 shouldBe 1
    val (x2, pqDash2) = pq.take
    pqDash2.isEmpty shouldBe true
    x2 shouldBe 2
    pqDash2.isEmpty shouldBe true
  }

  behavior of "MaxPQ"

  it should "isEmpty" in {
    MaxPQ[Int]().isEmpty shouldBe true
  }

  it should "take" in {
    a[NoSuchElementException] shouldBe thrownBy(MaxPQ[Int]().take)
  }

  it should "append/take 1" in {
    val pq: PQ[Int] = MaxPQ[Int]().append(1)
    pq.isEmpty shouldBe false
    val (x, pqDash) = pq.take
    pqDash.isEmpty shouldBe true
    x shouldBe 1
  }

  it should "append/take 2" in {
    val pq: PQ[Int] = MaxPQ[Int]().append(1).append(2)
    pq.isEmpty shouldBe false
    val (x1, pqDash) = pq.take
    pqDash.isEmpty shouldBe false
    x1 shouldBe 2
    val (x2, pqDash2) = pq.take
    pqDash2.isEmpty shouldBe true
    x2 shouldBe 1
    pqDash2.isEmpty shouldBe true
  }

}
