package com.phasmidsoftware.visitor.core

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

/**
  * Traversals of the two structures everyone reaches for first: a binary tree and a list.
  *
  * These exercise [[SimpleVisitor]] and [[Traverse]] on shapes where the expected answer is
  * known independently of this library — a binary search tree has a textbook pre- and
  * post-order, and a linked list has exactly one. They also demonstrate the use case
  * `SimpleVisitor` exists for: writing each node out as it is visited.
  */
class TraverseStructuresSpec extends AnyFlatSpec with Matchers:

  // TreeFixture (in TranslatedSpec.scala) is the binary search tree
  //
  //              10
  //            /    \
  //           5      13
  //          / \    /  \
  //         2   6  11   15
  //        / \
  //       1   3
  //
  // Only the adjacency is imported: SimpleVisitor needs no Evaluable.
  import TreeFixture.given_GraphNeighbours_Int

  behavior of "a binary tree"

  it should "traverse pre-order: node, then left subtree, then right" in :
    Traverse.dfs(10) shouldBe List(10, 5, 2, 1, 3, 6, 13, 11, 15)

  it should "traverse post-order: left subtree, then right, then node" in :
    // Previously untested — the binary tree was only ever walked pre-order, bfs or best-first.
    Traverse.dfs(10, DfsOrder.Post) shouldBe List(1, 3, 2, 6, 5, 11, 15, 13, 10)

  it should "always record a node after both its children in post-order" in :
    val order = Traverse.dfs(10, DfsOrder.Post)
    val position = order.zipWithIndex.toMap
    val children = Map(10 -> List(5, 13), 5 -> List(2, 6), 13 -> List(11, 15), 2 -> List(1, 3))
    for (parent, kids) <- children; kid <- kids do
      withClue(s"$kid should precede its parent $parent: ") :
        position(kid) should be < position(parent)

  it should "visit every node exactly once, whatever the order" in :
    Traverse.dfs(10).sorted shouldBe List(1, 2, 3, 5, 6, 10, 11, 13, 15)
    Traverse.dfs(10, DfsOrder.Post).sorted shouldBe List(1, 2, 3, 5, 6, 10, 11, 13, 15)
    Traverse.bfs(10).sorted shouldBe List(1, 2, 3, 5, 6, 10, 11, 13, 15)

  it should "traverse breadth-first in level order" in :
    Traverse.bfs(10) shouldBe List(10, 5, 13, 2, 6, 11, 15, 1, 3)

  behavior of "writing each node out as it is visited"

  it should "print the tree pre-order, one node per line" in :
    // The use case SimpleVisitor exists for. Nothing else in the suite demonstrates it.
    val out = new ByteArrayOutputStream()
    Console.withOut(out):
      Traverse.dfs(10).foreach(println)
    out.toString(StandardCharsets.UTF_8).linesIterator.toList shouldBe
      List("10", "5", "2", "1", "3", "6", "13", "11", "15")

  it should "print the tree post-order" in :
    val out = new ByteArrayOutputStream()
    Console.withOut(out):
      Traverse.dfs(10, DfsOrder.Post).foreach(println)
    out.toString(StandardCharsets.UTF_8).linesIterator.toList shouldBe
      List("1", "3", "2", "6", "5", "11", "15", "13", "10")

/**
  * A linked list is a degenerate tree: every node has exactly one child.
  *
  * Modelling it as `Neighbours[List[Int], List[Int]]` — each node is a list, its single
  * neighbour is its tail — shows that `Neighbours` need not come from a graph at all. It also
  * makes a pleasing check: walking a list post-order reverses it, because a node is recorded
  * only after everything below it.
  */
class TraverseListSpec extends AnyFlatSpec with Matchers:

  private val list: List[Int] = List(1, 2, 3, 4, 5)

  /** A node's only neighbour is its tail, until the tail is empty. */
  private given GraphNeighbours[List[Int]] with
    def neighbours(xs: List[Int]): Iterator[List[Int]] =
      if xs.sizeIs <= 1 then Iterator.empty else Iterator(xs.tail)

  behavior of "a list traversed as a structure"

  it should "yield its elements in order, pre-order" in :
    Traverse.dfs(list).map(_.head) shouldBe List(1, 2, 3, 4, 5)

  it should "yield its elements reversed, post-order" in :
    Traverse.dfs(list, DfsOrder.Post).map(_.head) shouldBe List(5, 4, 3, 2, 1)

  it should "agree with breadth-first, there being only one path" in :
    Traverse.bfs(list).map(_.head) shouldBe List(1, 2, 3, 4, 5)

  it should "visit one node per element" in :
    Traverse.dfs(list) should have size list.size

  it should "stop early at a goal" in :
    Traverse.dfs(list, DfsOrder.Pre, _.head == 3).map(_.head) shouldBe List(1, 2, 3)

  it should "handle a single-element list" in :
    Traverse.dfs(List(42)).map(_.head) shouldBe List(42)

  it should "print the list one element per line" in :
    val out = new ByteArrayOutputStream()
    Console.withOut(out):
      Traverse.dfs(list).foreach(xs => println(xs.head))
    out.toString(StandardCharsets.UTF_8).linesIterator.toList shouldBe
      List("1", "2", "3", "4", "5")
