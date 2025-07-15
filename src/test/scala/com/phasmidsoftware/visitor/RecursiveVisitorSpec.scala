package com.phasmidsoftware.visitor

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.util.{Success, Using}

class RecursiveVisitorSpec extends AnyFlatSpec with Matchers {

  behavior of "RecursiveVisitor"

  // Define a tree similar to a binary heap starting at slot 1
  //           10
  //        5--------13
  //      2---6---11---15
  //     1-3
  private val tree = Seq(-99, 10, 5, 13, 2, 6, 11, 15, 1, 3)

  // Define a function that gets the children of the input value `i`.
  def f(i: Int): Seq[Int] = {
    val twiceIndex = 2 * tree.indexOf(i)
    Seq(twiceIndex, twiceIndex + 1).filter(x => x > 0 && x < tree.length).map(tree)
  }

  it should "recurse pre-order" in {

    // Test a recursive pre-order traversal of the tree, starting at the root.
    Using(RecursiveVisitor[Int](Map(Pre -> QueueJournal.empty[Int]), f)) {
      visitor0 =>
        val visitor1 = visitor0.recurse(10)
        for {journal <- visitor1.journals
             entry <- journal
             } yield entry
    } shouldBe Success(List(10, 5, 2, 1, 3, 6, 13, 11, 15))
  }

  it should "recurse reverse post-order" in {
    // Test a recursive pre-order traversal of the tree, starting at the root.
    Using(RecursiveVisitor[Int](Map(Post -> ListJournal.empty[Int]), f)) {
      visitor =>
        for {journal <- visitor.recurse(10).journals
             entry <- journal
             } yield entry
    } shouldBe Success(List(10, 13, 15, 11, 5, 6, 2, 3, 1))
  }

  it should "recurse in-order" in {
    // Test a recursive pre-order traversal of the tree, starting at the root.
    Using(RecursiveVisitor[Int](Map(In -> QueueJournal.empty[Int]), f)) {
      visitor =>
        for {journal <- visitor.recurse(10).journals
             entry <- journal
             } yield entry
    } shouldBe Success(List(1, 2, 3, 5, 6, 10, 11, 13, 15))
  }
}
