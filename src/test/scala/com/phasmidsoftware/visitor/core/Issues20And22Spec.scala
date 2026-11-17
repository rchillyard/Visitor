package com.phasmidsoftware.visitor.core

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

/**
  * Tests for [[ParentedVisitor]] / [[EvaluableFrom]] (issue #20) and [[Traversal.dfsTracking]] /
  * [[VisitedSet.from]] (issue #22).
  *
  * The graph is `1 -> 2, 3; 2 -> 4; 3 -> 4, 5`, so 4 is reachable from both 2 and 3 — which is
  * what makes first-discovery-wins observable.
  */
class ParentedVisitorSpec extends AnyFlatSpec with Matchers:

  import TestGraph.given_GraphNeighbours_Int

  behavior of "ParentedVisitor"

  it should "give each node the node it was reached from" in :
    given EvaluableFrom[Int, Option[Int]] = EvaluableFrom.parentOf[Int]
    val out = Traversal.bfs(1, ParentedVisitor.withQueueJournal[Int, Option[Int]])
    out.result.toList shouldBe List(
      1 -> Some(None),      // the start node came from nowhere
      2 -> Some(Some(1)),
      3 -> Some(Some(1)),
      4 -> Some(Some(2)),   // first discovery wins: 2 is expanded before 3
      5 -> Some(Some(3))
    )

  it should "agree with JournaledVisitor's came-from map" in :
    given EvaluableFrom[Int, Option[Int]] = EvaluableFrom.parentOf[Int]
    val parented = Traversal.bfs(1, ParentedVisitor.withQueueJournal[Int, Option[Int]])
    val parents = parented.asInstanceOf[ParentedVisitor[Int, Option[Int], ?]].cameFrom

    given Evaluable[Int, Int] = (v: Int) => Some(v)
    val journaled = Traversal.bfs(1, JournaledVisitor.withQueueJournalAndCameFrom[Int, Int])
    val cameFrom = journaled.asInstanceOf[JournaledVisitor[Int, Int, ?]].cameFrom

    parents shouldBe cameFrom.getOrElse(Map.empty)

  it should "let evaluation depend on the predecessor" in :
    // Record a node only when it was reached from an odd node — a decision an Evaluable,
    // which sees the node alone, simply cannot express.
    given EvaluableFrom[Int, Int] =
      (from: Option[Int], v: Int) => from.filter(_ % 2 == 1).map(_ => v)
    val out = Traversal.bfs(1, ParentedVisitor.withQueueJournal[Int, Int])
    out.result.toList.collect { case (v, Some(_)) => v } shouldBe List(2, 3, 5)

  it should "evaluate the start node with None" in :
    given EvaluableFrom[Int, String] =
      (from: Option[Int], v: Int) => Some(from.fold(s"$v is the root")(p => s"$v from $p"))
    val out = Traversal.bfs(1, ParentedVisitor.withQueueJournal[Int, String])
    out.result.toList.head shouldBe (1 -> Some("1 is the root"))

  it should "adapt an ordinary Evaluable, ignoring the parent" in :
    given EvaluableFrom[Int, Int] = EvaluableFrom.ignoringParent((v: Int) => Some(v * 10))
    val out = Traversal.bfs(1, ParentedVisitor.withQueueJournal[Int, Int])
    out.result.toList.map(_._2) shouldBe List(Some(10), Some(20), Some(30), Some(40), Some(50))

  it should "work with dfs as well as bfs" in :
    given EvaluableFrom[Int, Option[Int]] = EvaluableFrom.parentOf[Int]
    val out = Traversal.dfs(1, ParentedVisitor.withQueueJournal[Int, Option[Int]])
    out.result.toList.map(_._1) shouldBe List(1, 2, 4, 3, 5)

class DfsTrackingSpec extends AnyFlatSpec with Matchers:

  import TestGraph.given_GraphNeighbours_Int

  behavior of "Traversal.dfsTracking"

  it should "hand back the visited set it built" in :
    val out = Traversal.dfsTracking(1, SimpleVisitor.withQueueJournal[Int], VisitedSet.empty[Int])
    out.visitor.result.toList shouldBe List(1, 2, 4, 3, 5)
    for v <- List(1, 2, 3, 4, 5) do out.visited.isVisited(v) shouldBe true
    out.visited.isVisited(99) shouldBe false

  it should "agree with plain dfs on the journal" in :
    val tracked = Traversal.dfsTracking(1, SimpleVisitor.withQueueJournal[Int], VisitedSet.empty[Int])
    val plain = Traversal.dfs(1, SimpleVisitor.withQueueJournal[Int])
    tracked.visitor.result.toList shouldBe plain.result.toList

  it should "let a second component start from the first one's visited set" in :
    // Two disjoint components: 1..5 from TestGraph, plus an isolated pair 10 -> 11.
    given GraphNeighbours[Int] with
      def neighbours(h: Int): Iterator[Int] = h match
        case 10 => Iterator(11)
        case n => TestGraph.adjacency.getOrElse(n, Nil).iterator

    val first = Traversal.dfsTracking(1, SimpleVisitor.withQueueJournal[Int], VisitedSet.empty[Int])
    // Carrying the set forward is a plain argument — no `using` list to spell out.
    val second = Traversal.dfsTracking(10, SimpleVisitor.withQueueJournal[Int], first.visited)

    first.visitor.result.toList shouldBe List(1, 2, 4, 3, 5)
    second.visitor.result.toList shouldBe List(10, 11)
    // The carried-forward set now covers both components, with no rebuilding.
    for v <- List(1, 2, 3, 4, 5, 10, 11) do second.visited.isVisited(v) shouldBe true

  it should "return the visited set even when a goal stops it early" in :
    val out = Traversal.dfsTracking(1, SimpleVisitor.withQueueJournal[Int], VisitedSet.empty[Int], DfsOrder.Pre, _ == 2)
    out.visitor.result.toList shouldBe List(1, 2)
    out.visited.isVisited(1) shouldBe true
    out.visited.isVisited(2) shouldBe true

  behavior of "VisitedSet.from"

  it should "build a visited set from an existing Set in one step" in :
    val vs = VisitedSet.from(Set(1, 2, 3))
    vs.isVisited(1) shouldBe true
    vs.isVisited(4) shouldBe false

  it should "be usable to seed a traversal" in :
    val out = Traversal.dfsTracking(1, SimpleVisitor.withQueueJournal[Int], VisitedSet.from(Set(2, 3))).visitor
    // 2 and 3 are pre-marked, so only 1 is reachable.
    out.result.toList shouldBe List(1)

  it should "give an empty set that matches the default given" in :
    VisitedSet.empty[Int].isVisited(1) shouldBe false
