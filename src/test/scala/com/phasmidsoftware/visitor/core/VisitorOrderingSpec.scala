package com.phasmidsoftware.visitor.core

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import scala.collection.mutable.ListBuffer

/**
  * Pins the engine's callback ordering contract: `discover(v, parent)` happens before
  * `visit(v)`, for every node except the start.
  *
  * [[ParentedVisitor]] depends on this entirely — it records predecessors as they are
  * discovered and reads them back when the node is visited. If the engine ever visited a node
  * before discovering it, that visitor would silently evaluate with `None` instead of the real
  * predecessor, and issue #20's whole approach would collapse. Nothing else in the suite
  * guards it.
  */
class VisitorOrderingSpec extends AnyFlatSpec with Matchers:

  import TestGraph.given_GraphNeighbours_Int

  /** A visitor that records the sequence of engine callbacks it receives. */
  private class Probe(val log: ListBuffer[String], protected val journal: QueueJournal[Int])
    extends Visitor[Int, Int, QueueJournal[Int]]:

    def visit(v: Int): Probe =
      log += s"visit($v)"
      new Probe(log, journal.append(v))

    override def discover(v: Int, cameFrom: Int): Probe =
      log += s"discover($v,$cameFrom)"
      this

  private def checkOrdering(run: Probe => Visitor[Int, Int, QueueJournal[Int]], clue: String): Unit =
    val log = ListBuffer.empty[String]
    val visited = run(new Probe(log, QueueJournal.empty[Int])).result.toList
    visited should not be empty
    for v <- visited if v != 1 do
      val discoveredAt = log.indexWhere(_.startsWith(s"discover($v,"))
      val visitedAt = log.indexOf(s"visit($v)")
      withClue(s"$clue: node $v discovered at $discoveredAt, visited at $visitedAt: ") :
        discoveredAt should be >= 0
        discoveredAt should be < visitedAt

  behavior of "the traversal engine's callback ordering"

  it should "discover a node before visiting it, in bfs" in :
    checkOrdering(p => Traversal.bfs(1, p), "bfs")

  it should "discover a node before visiting it, in dfs pre-order" in :
    checkOrdering(p => Traversal.dfs(1, p, DfsOrder.Pre), "dfs pre")

  it should "discover a node before visiting it, in dfs post-order" in :
    checkOrdering(p => Traversal.dfs(1, p, DfsOrder.Post), "dfs post")

  it should "never discover the start node" in :
    val log = ListBuffer.empty[String]
    Traversal.bfs(1, new Probe(log, QueueJournal.empty[Int])): Unit
    log.filter(_.startsWith("discover(1,")) shouldBe empty

  it should "discover a node once per incoming edge, not once overall" in :
    // 4 is reachable from both 2 and 3, so it is discovered twice. First discovery wins
    // downstream, but the engine does report both.
    val log = ListBuffer.empty[String]
    Traversal.bfs(1, new Probe(log, QueueJournal.empty[Int])): Unit
    log.count(_.startsWith("discover(4,")) shouldBe 2
    log.indexOf("discover(4,2)") should be < log.indexOf("discover(4,3)")
