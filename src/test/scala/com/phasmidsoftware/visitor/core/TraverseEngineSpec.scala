package com.phasmidsoftware.visitor.core

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import scala.collection.immutable.Queue

/**
  * Direct tests for [[Traversal.traverse]], the generic engine.
  *
  * The compiler points out that `traverse` could be private, because every call to it from
  * within this library goes through `bfs`, `bestFirst`, `bestFirstMax` or `bestFirstWeighted`.
  * It stays public deliberately: it is the entry point for a caller who brings their own
  * [[Frontier]], and that is the one thing the four wrappers cannot express between them. What
  * follows is the test that justifies the decision — until now `traverse` was exercised only
  * through those wrappers, each of which pins its own frontier and so can never show that the
  * frontier is what chooses the order.
  *
  * The contract asserted here is the one its scaladoc states: '''the choice of frontier F
  * determines the traversal order''', with everything else held constant. Two calls differing
  * in nothing but `F` produce the two textbook orders over the same tree.
  */
class TraverseEngineSpec extends AnyFlatSpec with Matchers:

  // TreeFixture (in TranslatedSpec.scala) is the binary search tree
  //
  //              10
  //            /    \
  //           5      13
  //          / \    /  \
  //         2   6  11   15
  //        / \
  //       1   3
  import TreeFixture.given_GraphNeighbours_Int

  private given Queue[Int] = Queue.empty

  private given Stack[Int] = Nil

  /**
    * One call to `traverse`, parameterised only by the frontier. Everything else — the node
    * type, the visitor, the adjacency, the visited set — is held constant, so any difference in
    * the result is attributable to `F` alone.
    */
  private def traverseWith[F[_]](start: Int, goal: Int => Boolean = (_: Int) => false)(using
                                                                                       fr: Frontier[F],
                                                                                       cu: CostUpdate[Int, F],
                                                                                       initial: F[Int]): List[Int] =
    Traversal.traverse[Int, Int, QueueJournal[Int], F](start, SimpleVisitor.withQueueJournal[Int], goal).result.toList

  behavior of "the frontier choosing the order"

  it should "traverse breadth-first when the frontier is a queue" in :
    traverseWith[Queue](10) shouldBe List(10, 5, 13, 2, 6, 11, 15, 1, 3)

  it should "traverse depth-first when the frontier is a stack" in :
    traverseWith[Stack](10) shouldBe List(10, 5, 2, 1, 3, 6, 13, 11, 15)

  it should "differ in nothing but F, and still give different orders" in :
    // The whole reason traverse is public rather than private.
    traverseWith[Queue](10) should not be traverseWith[Stack](10)

  it should "visit every node exactly once, whichever frontier is used" in :
    val everyNode = List(1, 2, 3, 5, 6, 10, 11, 13, 15)
    traverseWith[Queue](10).sorted shouldBe everyNode
    traverseWith[Stack](10).sorted shouldBe everyNode

  behavior of "traverse underneath the wrappers"

  it should "agree with bfs, which is traverse with a queue" in :
    traverseWith[Queue](10) shouldBe Visits.bfs(10)

  it should "agree with pre-order dfs, a stack reaching the same order by other means" in :
    // Not the same engine: dfs uses Either-tagged frames to place the record, where traverse
    // simply pops. That they coincide on a tree is worth pinning, because they diverge on a
    // graph, where dfs's frames outlive the pop order.
    traverseWith[Stack](10) shouldBe Visits.dfs(10)

  behavior of "the goal predicate"

  it should "record the start node before any goal check, and expand nothing" in :
    traverseWith[Queue](10, _ == 10) shouldBe List(10)

  it should "stop after recording the first node the goal accepts" in :
    // 13 is discovered and offered to the frontier, but never settled, so it is not recorded.
    traverseWith[Queue](10, _ == 5) shouldBe List(10, 5)

  it should "never stop early by default" in :
    traverseWith[Queue](10) should have size 9

  behavior of "discovery"

  it should "call discover for each unvisited neighbour, before it is offered" in :
    val visitor = ParentedVisitor.withQueueJournal[Int, Option[Int]](using EvaluableFrom.parentOf[Int])
    val out = Traversal.traverse[Int, Mapped[Int, Option[Int]], QueueJournal[Mapped[Int, Option[Int]]], Queue](10, visitor)
    out.asInstanceOf[ParentedVisitor[Int, Option[Int], ?]].cameFrom shouldBe
            Map(5 -> 10, 13 -> 10, 2 -> 5, 6 -> 5, 11 -> 13, 15 -> 13, 1 -> 2, 3 -> 2)

/**
  * The two guards `traverse` carries that a tree can never exercise: the pop-time revisit check
  * and termination on a cycle.
  *
  * A tree reaches every node by exactly one path, so nothing is ever offered to the frontier
  * twice and `visited.isVisited(node)` at pop time is never true. These use shapes where it is.
  * Compare [[https://github.com/rchillyard/Visitor/issues/39 issue #39]], which records the same
  * gap for `dfs` and `traverseTree`.
  */
class TraverseEngineGuardSpec extends AnyFlatSpec with Matchers:

  private given Queue[Int] = Queue.empty

  private def order(start: Int)(using GraphNeighbours[Int]): List[Int] =
    Traversal.traverse[Int, Int, QueueJournal[Int], Queue](start, SimpleVisitor.withQueueJournal[Int]).result.toList

  behavior of "the pop-time revisit guard"

  it should "record a node reached by two paths exactly once" in :
    // A diamond: 4 is offered to the frontier twice, by 2 and by 3, because neither has
    // visited it at the moment it expands. It is therefore popped twice, and the second pop
    // is what the guard at the top of the loop exists for.
    given GraphNeighbours[Int] with
      def neighbours(n: Int): Iterator[Int] = n match
        case 1 => Iterator(2, 3)
        case 2 => Iterator(4)
        case 3 => Iterator(4)
        case _ => Iterator.empty

    val result = order(1)
    result shouldBe List(1, 2, 3, 4)
    withClue("no node should appear twice: ") :
      result.distinct shouldBe result

  behavior of "termination"

  it should "terminate on a cycle, visiting each node once" in :
    given GraphNeighbours[Int] with
      def neighbours(n: Int): Iterator[Int] = n match
        case 1 => Iterator(2)
        case 2 => Iterator(3)
        case 3 => Iterator(1)
        case _ => Iterator.empty

    order(1) shouldBe List(1, 2, 3)

  it should "terminate on a self-loop" in :
    given GraphNeighbours[Int] with
      def neighbours(n: Int): Iterator[Int] = if n == 1 then Iterator(1) else Iterator.empty

    order(1) shouldBe List(1)

  it should "record a lone node with no neighbours" in :
    given GraphNeighbours[Int] with
      def neighbours(n: Int): Iterator[Int] = Iterator.empty

    order(42) shouldBe List(42)
