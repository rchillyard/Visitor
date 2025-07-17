package com.phasmidsoftware.visitor

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.collection.immutable.Queue
import scala.util.{Success, Using}

class BfsVisitorSpec extends AnyFlatSpec with Matchers {

  behavior of "BfsVisitor"

  // Define a tree similar to a binary heap starting at slot 1
  //           10
  //        5--------13
  //      2---6---11---15
  //     1-3

  // Define a function that gets the children of the input value `i`.
  def f(i: Int): Seq[Int] = i match {
    case 10 => Seq(5, 13)
    case 5 => Seq(2, 6)
    case 13 => Seq(11, 15)
    case 2 => Seq(1, 3)
    case _ => Seq.empty[Int]
  }

  it should "bfs pre-order" in {

    // Test a recursive pre-order traversal of the tree, starting at the root.
    Using(BfsVisitor[Int](Map(Pre -> QueueJournal.empty[Int]), f)) {
      visitor0 =>
        for {journal <- visitor0.bfs(10).iterableJournals
             entry <- journal
             } yield entry
    } shouldBe Success(Queue(10, 5, 13, 2, 6, 11, 15, 1, 3))
  }

  // NOTE that bfs is not set up to respond to post-order visits.
  ignore should "bfs reverse post-order" in {
    // Test a recursive pre-order traversal of the tree, starting at the root.
    Using(BfsVisitor[Int](Map(Post -> ListJournal.empty[Int]), f)) {
      visitor =>
        for {journal <- visitor.bfs(10).iterableJournals
             entry <- journal
             } yield entry
    } shouldBe Success(List(10, 13, 15, 11, 5, 6, 2, 3, 1))
  }
}
