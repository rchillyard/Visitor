package com.phasmidsoftware.visitor.core

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

/**
  * The same binary search tree as `TreeFixture`, but described as a [[BinaryNeighbours]] so that
  * in-order has something to work with.
  *
  * {{{
  *              10
  *            /    \
  *           5      13
  *          / \    /  \
  *         2   6  11   15
  *        / \
  *       1   3
  * }}}
  *
  * It is a separate fixture rather than an addition to `TreeFixture` because a
  * `BinaryNeighbours[Int]` is also a `GraphNeighbours[Int]`: putting both in one object would
  * make `import TreeFixture.given` ambiguous for every existing test.
  */
object BinaryTreeFixture:
  given BinaryNeighbours[Int] with
    def left(v: Int): Option[Int] = v match
      case 10 => Some(5)
      case 5 => Some(2)
      case 13 => Some(11)
      case 2 => Some(1)
      case _ => None

    def right(v: Int): Option[Int] = v match
      case 10 => Some(13)
      case 5 => Some(6)
      case 13 => Some(15)
      case 2 => Some(3)
      case _ => None

/**
  * In-order traversal of a binary tree — see
  * [[https://github.com/rchillyard/Visitor/issues/48 issue #48]].
  *
  * The property asserted here is not the arity of each node but '''sortedness''': in-order over
  * a binary search tree yields the ascending sequence. That is the definition of in-order rather
  * than a consequence of it, so a traversal that passes this test over a non-trivial tree is
  * correct by construction. It is supported by two weaker but independent checks — that the
  * three orders are permutations of one another, and that a childless node collapses them all
  * to one.
  */
class InOrderSpec extends AnyFlatSpec with Matchers:

  import BinaryTreeFixture.given

  private val everyNode = List(1, 2, 3, 5, 6, 10, 11, 13, 15)

  /** Node 2 with a single child, 1, on its left. */
  private val leftOnly: BinaryNeighbours[Int] = new BinaryNeighbours[Int]:
    def left(v: Int): Option[Int] = if v == 2 then Some(1) else None

    def right(v: Int): Option[Int] = None

  /** The same two nodes, but with 1 on the right instead. */
  private val rightOnly: BinaryNeighbours[Int] = new BinaryNeighbours[Int]:
    def left(v: Int): Option[Int] = None

    def right(v: Int): Option[Int] = if v == 2 then Some(1) else None

  /** A node with no children at all. */
  private val childless: BinaryNeighbours[Int] = new BinaryNeighbours[Int]:
    def left(v: Int): Option[Int] = None

    def right(v: Int): Option[Int] = None

  behavior of "in-order over a binary search tree"

  it should "yield the nodes in ascending order" in :
    Traverse.inOrder(10) shouldBe everyNode

  it should "agree with sorting, which is what in-order means here" in :
    val order = Traverse.inOrder(10)
    order shouldBe order.sorted

  it should "record a node between its two subtrees" in :
    val position = Traverse.inOrder(10).zipWithIndex.toMap
    val subtrees = Map(
      10 -> (List(5, 2, 1, 3, 6), List(13, 11, 15)),
      5 -> (List(2, 1, 3), List(6)),
      13 -> (List(11), List(15)),
      2 -> (List(1), List(3))
    )
    for (node, (before, after)) <- subtrees do
      for n <- before do
        withClue(s"$n is in $node's left subtree, so should precede it: ") :
          position(n) should be < position(node)
      for n <- after do
        withClue(s"$n is in $node's right subtree, so should follow it: ") :
          position(n) should be > position(node)

  behavior of "in-order alongside pre- and post-order"

  it should "visit every node exactly once" in :
    Traverse.inOrder(10).sorted shouldBe everyNode

  it should "be a permutation of pre-order and of post-order" in :
    val in = Traverse.inOrder(10)
    in.sorted shouldBe Traverse.dfs(10).sorted
    in.sorted shouldBe Traverse.dfs(10, DfsOrder.Post).sorted

  it should "differ from both, this tree being neither a left nor a right spine" in :
    val in = Traverse.inOrder(10)
    in should not be Traverse.dfs(10)
    in should not be Traverse.dfs(10, DfsOrder.Post)

  it should "drive pre-order too, from the one instance" in :
    // BinaryNeighbours supplies `neighbours` from `left` and `right`, so the same given serves
    // the engines that know nothing of left and right.
    Traverse.dfs(10) shouldBe List(10, 5, 2, 1, 3, 6, 13, 11, 15)
    Traverse.bfs(10) shouldBe List(10, 5, 13, 2, 6, 11, 15, 1, 3)

  behavior of "the degenerate cases"

  it should "collapse pre-, in- and post-order for a childless node" in :
    Traverse.inOrder(42)(using childless) shouldBe List(42)
    Traverse.dfs(42)(using childless) shouldBe List(42)
    Traverse.dfs(42, DfsOrder.Post)(using childless) shouldBe List(42)

  it should "distinguish a left-only child from a right-only one" in :
    // The case an arity check cannot reach: both trees have exactly one child, so counting
    // admits both, yet their in-orders differ. This is why BinaryNeighbours exists.
    Traverse.inOrder(2)(using leftOnly) shouldBe List(1, 2)
    Traverse.inOrder(2)(using rightOnly) shouldBe List(2, 1)

  it should "be blind to that difference through neighbours alone" in :
    // The other half of the same point: as adjacencies the two trees are indistinguishable,
    // so no amount of counting or inspecting `neighbours` could have told them apart.
    leftOnly.neighbours(2).toList shouldBe rightOnly.neighbours(2).toList
    Traverse.dfs(2)(using leftOnly) shouldBe Traverse.dfs(2)(using rightOnly)

  behavior of "in-order with a goal and a visited set"

  it should "stop after recording the first node the goal accepts" in :
    // 5 is recorded once its left subtree (1, 2, 3) is done, and before its right child 6.
    Traverse.inOrder(10, _ == 5) shouldBe List(1, 2, 3, 5)

  it should "stop at the very first node when the goal accepts it" in :
    Traverse.inOrder(10, _ == 1) shouldBe List(1)

  it should "hand back the visited set it built" in :
    val traversed = Traversal.inOrderTracking(10, SimpleVisitor.withQueueJournal[Int], VisitedSet.empty[Int])
    traversed.visitor.result.toList shouldBe everyNode
    everyNode.foreach(n => traversed.visited.isVisited(n) shouldBe true)

  it should "visit nothing when the start node is already visited" in :
    val seen = VisitedSet.empty[Int].markVisited(10)
    Traversal.inOrderTracking(10, SimpleVisitor.withQueueJournal[Int], seen).visitor.result.toList shouldBe Nil

  behavior of "in-order and came-from"

  it should "discover each child from its parent, as dfs does" in :
    val visitor = ParentedVisitor.withQueueJournal[Int, Option[Int]](using EvaluableFrom.parentOf[Int])
    val parents = Traversal.inOrder(10, visitor).asInstanceOf[ParentedVisitor[Int, Option[Int], ?]].cameFrom
    parents shouldBe Map(5 -> 10, 13 -> 10, 2 -> 5, 6 -> 5, 11 -> 13, 15 -> 13, 1 -> 2, 3 -> 2)
