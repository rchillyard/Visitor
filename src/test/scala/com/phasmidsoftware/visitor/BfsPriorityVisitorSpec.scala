//package com.phasmidsoftware.visitor
//
//import org.scalatest.flatspec.AnyFlatSpec
//import org.scalatest.matchers.should.Matchers
//
//import scala.util.{Success, Using}
//
//class BfsPriorityVisitorSpec extends AnyFlatSpec with Matchers {
//
//  behavior of "BfsPriorityVisitor"
//
//  // Define a tree similar to a binary heap starting at slot 1
//  //           10
//  //        5--------13
//  //      2---6---11---15
//  //     1-3
//  private val tree = Seq(-99, 10, 5, 13, 2, 6, 11, 15, 1, 3)
//
//  // Define a function that gets the children of the input value `i`.
//  def f(i: Int): Seq[Int] = {
//    val twiceIndex = 2 * tree.indexOf(i)
//    Seq(twiceIndex, twiceIndex + 1).filter(x => x > 0 && x < tree.length).map(tree)
//  }
//
//  it should "iterableJournals" in {
//    BfsPriorityVisitor
//  }
//
//  it should "appendables" in {
//
//  }
//
//  it should "close" in {
//
//  }
//
//  it should "bfs" in {
//
//  }
//
//  it should "bfsg" in {
//
//    it should "dfs pre-order" in {
//      val visitor = BfsPriorityVisitor[X](pq: PQ[X], f: X => Seq[X], goal: X => Boolean)
//      // Test a recursive pre-order traversal of the tree, starting at the root.
//      Using(DfsVisitor[Int](Map(Pre -> QueueJournal.empty[Int]), f)) {
//        visitor =>
//          for {journal <- visitor.dfs(10).iterableJournals
//               entry <- journal
//               } yield entry
//      } shouldBe Success(List(10, 5, 2, 1, 3, 6, 13, 11, 15))
//    }
//
//  }
//
//  it should "create" in {
//
//  }
//
//  it should "createQueue" in {
//
//  }
//
//}
