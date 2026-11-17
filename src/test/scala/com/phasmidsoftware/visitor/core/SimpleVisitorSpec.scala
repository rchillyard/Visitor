package com.phasmidsoftware.visitor.core

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

/**
  * Tests for [[SimpleVisitor]] and the [[Traverse]] convenience entry points.
  *
  * Uses the same integer graph as `VisitorSpec`:
  * {{{
  *   1 -> 2, 3;  2 -> 4;  3 -> 4, 5;  4 -> ();  5 -> ()
  * }}}
  * Note that only `GraphNeighbours[Int]` is imported below — deliberately not any
  * `Evaluable`, since the whole point of `SimpleVisitor` is that it needs none.
  */
class SimpleVisitorSpec extends AnyFlatSpec with Matchers:

  import TestGraph.given_GraphNeighbours_Int

  behavior of "SimpleVisitor"

  it should "record nodes themselves, with no Evaluable in scope" in :
    val result = Traversal.bfs(1, SimpleVisitor.withQueueJournal[Int])
    result.result.toList shouldBe List(1, 2, 3, 4, 5)

  it should "be usable by dfs, pre-order" in :
    val result = Traversal.dfs(1, SimpleVisitor.withQueueJournal[Int], DfsOrder.Pre)
    result.result.toList shouldBe List(1, 2, 4, 3, 5)

  it should "be usable by dfs, post-order" in :
    val result = Traversal.dfs(1, SimpleVisitor.withQueueJournal[Int], DfsOrder.Post)
    result.result.toList shouldBe List(4, 2, 5, 3, 1)

  it should "honour a goal predicate" in :
    val result = Traversal.bfs(1, SimpleVisitor.withQueueJournal[Int], _ == 3)
    result.result.toList shouldBe List(1, 2, 3)

  it should "prepend when given a ListJournal, so the journal reads in reverse" in :
    val result = Traversal.bfs(1, SimpleVisitor.withListJournal[Int])
    result.result.toList shouldBe List(5, 4, 3, 2, 1)

  it should "visit a lone node with no neighbours" in :
    val result = Traversal.bfs(99, SimpleVisitor.withQueueJournal[Int])
    result.result.toList shouldBe List(99)

  it should "leave discover as a no-op, recording no came-from" in :
    // SimpleVisitor does not override discover; the default returns `this` unchanged.
    val v = SimpleVisitor.withQueueJournal[Int]
    v.discover(2, 1) shouldBe v

  it should "be a Visitor, so the engine accepts it wherever a JournaledVisitor goes" in :
    val v: Visitor[Int, Int, QueueJournal[Int]] = SimpleVisitor.withQueueJournal[Int]
    v.visit(7).result.toList shouldBe List(7)

  behavior of "Traverse"

  it should "give bfs order directly as a List" in :
    Traverse.bfs(1) shouldBe List(1, 2, 3, 4, 5)

  it should "give dfs pre-order directly as a List" in :
    Traverse.dfs(1) shouldBe List(1, 2, 4, 3, 5)

  it should "give dfs post-order directly as a List" in :
    Traverse.dfs(1, DfsOrder.Post) shouldBe List(4, 2, 5, 3, 1)

  it should "honour a goal in bfs" in :
    Traverse.bfs(1, _ == 3) shouldBe List(1, 2, 3)

  it should "agree with the engine driven by hand" in :
    val byHand = Traversal.bfs(1, SimpleVisitor.withQueueJournal[Int]).result.toList
    Traverse.bfs(1) shouldBe byHand

  behavior of "Traverse.tree"

  // TestTree is A -> (B -> (D, E), C), with H = Tree and V = String. traverseTree starts from
  // the root's children, so "A" itself is never recorded.
  import TestTree.{given_Neighbours_Tree_String, given_GraphNeighbours_String}

  it should "not visit the root, and read in pre-order" in :
    Traverse.tree[TestTree.Tree, String](TestTree.root) shouldBe List("B", "D", "E", "C")

  it should "read in post-order" in :
    Traverse.tree[TestTree.Tree, String](TestTree.root, DfsOrder.Post) shouldBe List("D", "E", "B", "C")

  it should "need no Evaluable, unlike the JournaledVisitor path" in :
    // VisitorSpec's equivalent requires a `given Evaluable[String, String]`; this does not.
    Traverse.tree[TestTree.Tree, String](TestTree.root) should not be empty
