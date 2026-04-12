package com.phasmidsoftware.visitor.core

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.collection.immutable.Queue

/**
  * Test graph/tree structures used across multiple tests.
  *
  * Simple directed graph:
  *
  *   1 → 2 → 4
  *   1 → 3 → 4
  *       3 → 5
  *
  * i.e. a diamond with an extra leaf.
  */
object TestGraph:
  val adjacency: Map[Int, List[Int]] = Map(
    1 -> List(2, 3),
    2 -> List(4),
    3 -> List(4, 5),
    4 -> List(),
    5 -> List()
  )

  /** Neighbours for the integer graph. */
  given GraphNeighbours[Int] with
    def neighbours(h: Int): Iterator[Int] =
      adjacency.getOrElse(h, Nil).iterator

  /** Evaluable: the result of visiting a node is the node value itself. */
  given Evaluable[Int, Int] with
    def evaluate(v: Int): Option[Int] = Some(v)

  /** Evaluable: only even nodes produce a result. */
  given Evaluable[Int, String] with
    def evaluate(v: Int): Option[String] =
      if v % 2 == 0 then Some(s"even:$v") else None

/**
  * A simple binary tree for traverseTree tests.
  *
  *        A
  *       / \
  *      B   C
  *     / \
  *    D   E
  */
object TestTree:
  case class Tree(value: String, children: List[Tree] = Nil)

  val root: Tree = Tree("A", List(
    Tree("B", List(Tree("D"), Tree("E"))),
    Tree("C")
  ))

  /** Root neighbours: H = Tree, V = String. */
  given Neighbours[Tree, String] with
    def neighbours(h: Tree): Iterator[String] =
      h.children.map(_.value).iterator

  /** Once we have String nodes, we need String → String neighbours.
    * In this simple test we derive children from a lookup map. */
  val treeMap: Map[String, List[String]] = Map(
    "A" -> List("B", "C"),
    "B" -> List("D", "E"),
    "C" -> Nil,
    "D" -> Nil,
    "E" -> Nil
  )

  given GraphNeighbours[String] with
    def neighbours(h: String): Iterator[String] =
      treeMap.getOrElse(h, Nil).iterator

  given Evaluable[String, String] with
    def evaluate(v: String): Option[String] = Some(v)

// ============================================================
// VisitedSet tests
// ============================================================

class VisitedSetSpec extends AnyFlatSpec with Matchers:

  "SetVisitedSet" should "report unvisited nodes correctly" in :
    val vs = summon[VisitedSet[Int]]
    vs.isVisited(1) shouldBe false

  it should "report visited nodes after marking" in :
    val vs = summon[VisitedSet[Int]].markVisited(1)
    vs.isVisited(1) shouldBe true

  it should "be immutable — original unchanged after markVisited" in :
    val vs0 = summon[VisitedSet[Int]]
    val vs1 = vs0.markVisited(1)
    vs0.isVisited(1) shouldBe false
    vs1.isVisited(1) shouldBe true

  it should "track multiple visited nodes independently" in :
    val vs = summon[VisitedSet[Int]].markVisited(1).markVisited(3)
    vs.isVisited(1) shouldBe true
    vs.isVisited(2) shouldBe false
    vs.isVisited(3) shouldBe true

// ============================================================
// Frontier tests
// ============================================================

class FrontierSpec extends AnyFlatSpec with Matchers:

  "Frontier[Queue] (BFS)" should "offer and take in FIFO order" in :
    val fr = summon[Frontier[Queue]]
    val q0 = fr.empty[Int]
    val q1 = fr.offer(fr.offer(q0)(1))(2)
    val (a, q2) = fr.take(q1)
    val (b, _) = fr.take(q2)
    a shouldBe 1
    b shouldBe 2

  it should "report empty correctly" in :
    val fr = summon[Frontier[Queue]]
    fr.isEmpty(fr.empty[Int]) shouldBe true
    fr.isEmpty(fr.offer(fr.empty[Int])(42)) shouldBe false

  "Frontier[Stack] (DFS)" should "offer and take in LIFO order" in :
    val fr = summon[Frontier[Stack]]
    val s0 = fr.empty[Int]
    val s1 = fr.offer(fr.offer(s0)(1))(2)
    val (a, s2) = fr.take(s1)
    val (b, _) = fr.take(s2)
    a shouldBe 2 // LIFO
    b shouldBe 1

  it should "report empty correctly" in :
    val fr = summon[Frontier[Stack]]
    fr.isEmpty(fr.empty[Int]) shouldBe true
    fr.isEmpty(fr.offer(fr.empty[Int])(42)) shouldBe false

  "Frontier[PrioQueue] (best-first)" should "dequeue elements in ascending order (min-first)" in :
    val fr = summon[Frontier[PrioQueue]]
    val pq0 = PrioQueue.empty[Int]
    val pq1 = fr.offer(fr.offer(fr.offer(pq0)(3))(1))(2)
    val (a, pq2) = fr.take(pq1)
    val (b, pq3) = fr.take(pq2)
    val (c, _) = fr.take(pq3)
    a shouldBe 1
    b shouldBe 2
    c shouldBe 3

// ============================================================
// Journal tests
// ============================================================

class JournalSpec extends AnyFlatSpec with Matchers:

  "ListJournal" should "append elements in prepend order" in :
    val j = ListJournal.empty[Int].append(1).append(2).append(3)
    j.toList shouldBe List(3, 2, 1)

  it should "be empty initially" in :
    ListJournal.empty[Int].toList shouldBe Nil

  "QueueJournal" should "append elements in FIFO order" in :
    val j = QueueJournal.empty[Int].append(1).append(2).append(3)
    j.toList shouldBe List(1, 2, 3)

// ============================================================
// JournaledVisitor tests
// ============================================================

class JournaledVisitorSpec extends AnyFlatSpec with Matchers:

  import TestGraph.given

  "JournaledVisitor" should "visit a single node and record it" in :
    val v = JournaledVisitor.withListJournal[Int, Int]
    val v2 = v.visit(42)
    v2.result.toList shouldBe List((42, Some(42)))

  it should "visit multiple nodes accumulating results" in :
    val v = JournaledVisitor.withListJournal[Int, Int]
      .visit(1).visit(2).visit(3)
    // ListJournal prepends, so most recent first
    v.result.toList shouldBe List((3, Some(3)), (2, Some(2)), (1, Some(1)))

  it should "record None for nodes that produce no result" in :
    val v = JournaledVisitor.withListJournal[Int, String]
    val v2 = v.visit(1).visit(2).visit(3)
    val results = v2.result.toList.toMap
    results(1) shouldBe None
    results(2) shouldBe Some("even:2")
    results(3) shouldBe None

// ============================================================
// Traversal — BFS tests
// ============================================================

class BfsSpec extends AnyFlatSpec with Matchers:

  import TestGraph.given

  "Traversal.bfs" should "visit all reachable nodes from node 1" in :
    val visitor = JournaledVisitor.withQueueJournal[Int, Int]
    val result = Traversal.bfs(1, visitor)
    val visited = result.result.map(_._1).toSet
    visited shouldBe Set(1, 2, 3, 4, 5)

  it should "visit nodes in BFS order (level by level)" in :
    val visitor = JournaledVisitor.withQueueJournal[Int, Int]
    val result = Traversal.bfs(1, visitor)
    val order = result.result.map(_._1).toList
    // 1 first, then 2 and 3 (level 2), then 4 and 5 (level 3)
    order.head shouldBe 1
    order.take(3).toSet shouldBe Set(1, 2, 3)
    order.toSet shouldBe Set(1, 2, 3, 4, 5)

  it should "not revisit nodes in a graph with a diamond" in :
    val visitor = JournaledVisitor.withQueueJournal[Int, Int]
    val result = Traversal.bfs(1, visitor)
    // Node 4 is reachable from both 2 and 3 — should appear only once
    result.result.map(_._1).toList.count(_ == 4) shouldBe 1

  it should "work from a leaf node with no neighbours" in :
    val visitor = JournaledVisitor.withQueueJournal[Int, Int]
    val result = Traversal.bfs(5, visitor)
    result.result.map(_._1).toList shouldBe List(5)

// ============================================================
// Traversal — DFS tests
// ============================================================

class DfsSpec extends AnyFlatSpec with Matchers:

  import TestGraph.given
  import com.phasmidsoftware.visitor.core.DfsOrder.Post

  "Traversal.dfs" should "visit all reachable nodes from node 1" in :
    val visitor = JournaledVisitor.withListJournal[Int, Int]
    val result = Traversal.dfs(1, visitor)
    result.result.map(_._1).toSet shouldBe Set(1, 2, 3, 4, 5)

  it should "visit node 1 first" in :
    val visitor = JournaledVisitor.withListJournal[Int, Int]
    val result = Traversal.dfs(1, visitor)
    // ListJournal prepends, so last visited is at head
    result.result.map(_._1).toList.last shouldBe 1

  it should "not revisit nodes" in :
    val visitor = JournaledVisitor.withListJournal[Int, Int]
    val result = Traversal.dfs(1, visitor)
    val visited = result.result.map(_._1).toList
    visited.distinct shouldBe visited

  it should "work from a leaf node" in :
    val visitor = JournaledVisitor.withListJournal[Int, Int]
    val result = Traversal.dfs(5, visitor)
    result.result.map(_._1).toList shouldBe List(5)

  it should "traverse in post-order (queue journal)" in :
    import TreeFixture.given
    val visitor = JournaledVisitor.withQueueJournal[Int, Int]
    val result = Traversal.dfs(10, visitor, Post)
    result.result.map(_._1).toList shouldBe List(1, 3, 2, 6, 5, 11, 15, 13, 10)

  it should "traverse in post-order (list journal)" in :
    import TreeFixture.given
    val visitor = JournaledVisitor.withListJournal[Int, Int]
    val result = Traversal.dfs(10, visitor, Post)
    result.result.map(_._1).toList shouldBe List(10, 13, 15, 11, 5, 6, 2, 3, 1)

// ============================================================
// Traversal — bestFirst tests
// ============================================================

class BestFirstSpec extends AnyFlatSpec with Matchers:

  import TestGraph.given

  "Traversal.bestFirst" should "visit all reachable nodes from node 1" in :
    val visitor = JournaledVisitor.withListJournal[Int, Int]
    val result = Traversal.bestFirst(1, visitor)
    result.result.map(_._1).toSet shouldBe Set(1, 2, 3, 4, 5)

  it should "visit nodes in ascending order (min-first by default Ordering[Int])" in :
    val visitor = JournaledVisitor.withListJournal[Int, Int]
    val result = Traversal.bestFirst(1, visitor)
    // ListJournal prepends, so reverse to get visit order
    val order = result.result.map(_._1).toList.reverse
    // First node is always start (1), then frontier pops in ascending order
    order.head shouldBe 1
    // Each subsequent node should be >= previous (min-heap property)
    order.sliding(2).foreach:
      case List(a, b) => a should be <= b
      case _ => ()

  it should "not revisit nodes" in :
    val visitor = JournaledVisitor.withListJournal[Int, Int]
    val result = Traversal.bestFirst(1, visitor)
    val visited = result.result.map(_._1).toList
    visited.distinct shouldBe visited

// ============================================================
// Traversal — traverseTree tests
//
// Tree fixture (from TestTree):
//
//        A
//       / \
//      B   C
//     / \
//    D   E
//
// BFS / pre-order:   B, C, D, E   (A is root H, not visited)
// DFS pre-order:     B, D, E, C
// DFS post-order:    D, E, B, C
// ============================================================

class TraverseTreeSpec extends AnyFlatSpec with Matchers:

  import TestTree.given

  // Helper to reduce boilerplate — traverseTree no longer needs F[_] type param
  private def runTree(
                       order: DfsOrder = DfsOrder.Pre,
                       goal:  String => Boolean = _ => false
                     ) =
    Traversal.traverseTree[TestTree.Tree, String, String, QueueJournal[(String, Option[String])]](
      TestTree.root,
      JournaledVisitor.withQueueJournal[String, String],
      order,
      goal
    )

  "Traversal.traverseTree" should "visit all nodes in the tree (pre-order)" in :
    runTree().result.map(_._1).toSet shouldBe Set("B", "C", "D", "E")

  it should "not visit the root H node" in :
    runTree().result.map(_._1).toList should not contain "A"

  it should "not revisit any node" in :
    val visited = runTree().result.map(_._1).toList
    visited.distinct shouldBe visited

  it should "visit nodes in DFS pre-order" in :
    runTree(DfsOrder.Pre).result.map(_._1).toList shouldBe List("B", "D", "E", "C")

  it should "visit nodes in DFS post-order" in :
    runTree(DfsOrder.Post).result.map(_._1).toList shouldBe List("D", "E", "B", "C")

  it should "record correct evaluated values" in :
    runTree().result.toList.map(_._2) shouldBe
      List("B", "D", "E", "C").map(s => Some(s))

  "Traversal.traverseTree with goal" should "stop after recording the goal node" in :
    // Pre-order: B, D — stops at D
    runTree(goal = _ == "D").result.map(_._1).toList shouldBe List("B", "D")

  it should "include the goal node in the journal" in :
    runTree(goal = _ == "C").result.map(_._1).toList should contain("C")

  it should "not expand children of the goal node" in :
    // B's children are D and E — stopping at B means D and E should not appear
    runTree(goal = _ == "B").result.map(_._1).toList should contain noneOf("D", "E")

  it should "traverse everything when goal is never met" in :
    runTree(goal = _ == "Z").result.map(_._1).toSet shouldBe Set("B", "C", "D", "E")

  it should "stop at first node when it matches goal" in :
    runTree(goal = _ == "B").result.map(_._1).toList shouldBe List("B")

// ============================================================
// CameFromJournal tests
// ============================================================

class CameFromJournalSpec extends AnyFlatSpec with Matchers:

  "CameFromJournal" should "be empty initially" in :
    val j = CameFromJournal.empty[Int]
    j.asMap shouldBe Map.empty

  it should "record a came-from relationship on append" in :
    val j = CameFromJournal.empty[Int].append(2 -> 1)
    j.cameFrom(2) shouldBe Some(1)

  it should "record multiple came-from relationships" in :
    val j = CameFromJournal.empty[Int]
      .append(2 -> 1)
      .append(3 -> 1)
      .append(4 -> 2)
    j.cameFrom(2) shouldBe Some(1)
    j.cameFrom(3) shouldBe Some(1)
    j.cameFrom(4) shouldBe Some(2)

  it should "return None for vertices not in the map" in :
    val j = CameFromJournal.empty[Int].append(2 -> 1)
    j.cameFrom(99) shouldBe None

  it should "be iterable over (discovered, cameFrom) pairs" in :
    val j = CameFromJournal.empty[Int].append(2 -> 1).append(3 -> 1)
    j.toSet shouldBe Set(2 -> 1, 3 -> 1)

  it should "expose its contents as a plain Map" in :
    val j = CameFromJournal.empty[Int].append(2 -> 1).append(4 -> 2)
    j.asMap shouldBe Map(2 -> 1, 4 -> 2)

// ============================================================
// JournaledVisitor — discover tests
// ============================================================

class JournaledVisitorDiscoverSpec extends AnyFlatSpec with Matchers:

  "JournaledVisitor without came-from journal" should "return None for cameFrom" in :
    val v = JournaledVisitor.withQueueJournal[Int, Int]
    v.cameFrom shouldBe None

  it should "be a no-op when discover is called" in :
    val v = JournaledVisitor.withQueueJournal[Int, Int]
    val v2 = v.discover(2, 1)
    v2.cameFrom shouldBe None

  "JournaledVisitor with came-from journal" should "return Some(empty map) initially" in :
    val v = JournaledVisitor.withQueueJournalAndCameFrom[Int, Int]
    v.cameFrom shouldBe Some(Map.empty)

  it should "record discover calls in the came-from map" in :
    val v = JournaledVisitor.withQueueJournalAndCameFrom[Int, Int]
    val v2 = v.discover(2, 1).discover(3, 1).discover(4, 2)
    v2.cameFrom shouldBe Some(Map(2 -> 1, 3 -> 1, 4 -> 2))

  it should "not affect the visit journal when discover is called" in :
    val v = JournaledVisitor.withQueueJournalAndCameFrom[Int, Int]
    val v2 = v.discover(2, 1)
    v2.result.toList shouldBe Nil

// ============================================================
// BFS came-from tests
// ============================================================

class BfsCameFromSpec extends AnyFlatSpec with Matchers:

  import TestGraph.given

  "Traversal.bfs with CameFromJournal" should "not include start vertex in came-from map" in :
    val visitor = JournaledVisitor.withQueueJournalAndCameFrom[Int, Int]
    val result = Traversal.bfs(1, visitor).asInstanceOf[JournaledVisitor[Int, Int, ?]]
    result.cameFrom.get should not contain key(1)

  it should "record all non-start vertices in came-from map" in :
    val visitor = JournaledVisitor.withQueueJournalAndCameFrom[Int, Int]
    val result = Traversal.bfs(1, visitor).asInstanceOf[JournaledVisitor[Int, Int, ?]]
    result.cameFrom.get.keySet shouldBe Set(2, 3, 4, 5)

  it should "record correct came-from for direct neighbours of start" in :
    val visitor = JournaledVisitor.withQueueJournalAndCameFrom[Int, Int]
    val result = Traversal.bfs(1, visitor).asInstanceOf[JournaledVisitor[Int, Int, ?]]
    val cf = result.cameFrom.get
    cf(2) shouldBe 1
    cf(3) shouldBe 1

  it should "record node 4 as discovered from either 2 or 3 (first BFS neighbour)" in :
    val visitor = JournaledVisitor.withQueueJournalAndCameFrom[Int, Int]
    val result = Traversal.bfs(1, visitor).asInstanceOf[JournaledVisitor[Int, Int, ?]]
    result.cameFrom.get(4) shouldBe 2

  it should "support path reconstruction from leaf to start" in :
    val visitor = JournaledVisitor.withQueueJournalAndCameFrom[Int, Int]
    val result = Traversal.bfs(1, visitor).asInstanceOf[JournaledVisitor[Int, Int, ?]]
    val cf = result.cameFrom.get

    def pathTo(target: Int): List[Int] =
      def walk(v: Int, acc: List[Int]): List[Int] =
        cf.get(v) match
          case None => v :: acc // start vertex
          case Some(from) => walk(from, v :: acc)

      walk(target, Nil)

    pathTo(4) shouldBe List(1, 2, 4)
    pathTo(5) shouldBe List(1, 3, 5)
    pathTo(2) shouldBe List(1, 2)

// ============================================================
// DFS came-from tests
// ============================================================

class DfsCameFromSpec extends AnyFlatSpec with Matchers:

  import TestGraph.given

  "Traversal.dfs with CameFromJournal" should "not include start vertex in came-from map" in :
    val visitor = JournaledVisitor.withListJournalAndCameFrom[Int, Int]
    val result = Traversal.dfs(1, visitor).asInstanceOf[JournaledVisitor[Int, Int, ?]]
    result.cameFrom.get should not contain key(1)

  it should "record all non-start vertices in came-from map" in :
    val visitor = JournaledVisitor.withListJournalAndCameFrom[Int, Int]
    val result = Traversal.dfs(1, visitor).asInstanceOf[JournaledVisitor[Int, Int, ?]]
    result.cameFrom.get.keySet shouldBe Set(2, 3, 4, 5)

  it should "record correct came-from for direct neighbours of start" in :
    val visitor = JournaledVisitor.withListJournalAndCameFrom[Int, Int]
    val result = Traversal.dfs(1, visitor).asInstanceOf[JournaledVisitor[Int, Int, ?]]
    val cf = result.cameFrom.get
    cf(2) shouldBe 1
    cf(3) shouldBe 1

  it should "support path reconstruction from leaf to start" in :
    val visitor = JournaledVisitor.withListJournalAndCameFrom[Int, Int]
    val result = Traversal.dfs(1, visitor).asInstanceOf[JournaledVisitor[Int, Int, ?]]
    val cf = result.cameFrom.get

    def pathTo(target: Int): List[Int] =
      def walk(v: Int, acc: List[Int]): List[Int] =
        cf.get(v) match
          case None => v :: acc
          case Some(from) => walk(from, v :: acc)

      walk(target, Nil)

    pathTo(2) shouldBe List(1, 2)
    pathTo(3) shouldBe List(1, 3)
    // DFS path to 4 goes through whichever branch was explored first
    pathTo(4).head shouldBe 1
    pathTo(4).last shouldBe 4

  it should "agree with BFS on connectivity (same key set)" in :
    val bfsVisitor = JournaledVisitor.withQueueJournalAndCameFrom[Int, Int]
    val dfsVisitor = JournaledVisitor.withListJournalAndCameFrom[Int, Int]
    val bfsCf = Traversal.bfs(1, bfsVisitor).asInstanceOf[JournaledVisitor[Int, Int, ?]].cameFrom.get
    val dfsCf = Traversal.dfs(1, dfsVisitor).asInstanceOf[JournaledVisitor[Int, Int, ?]].cameFrom.get
    bfsCf.keySet shouldBe dfsCf.keySet