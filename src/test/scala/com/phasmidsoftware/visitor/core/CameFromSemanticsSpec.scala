package com.phasmidsoftware.visitor.core

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import scala.annotation.tailrec

/**
  * What does `cameFrom` hold, and is it the tree the traversal actually walked? Issue #55.
  *
  * ==Why this spec exists==
  *
  * Through 1.9.x, `cameFrom` recorded the first vertex to reach each one, for both traversals.
  * That is right for breadth-first and wrong for depth-first, and '''no test said otherwise''' —
  * the suite was green throughout, because every answer was a valid came-from map, just not the
  * DFS tree. It took a consumer running DFS on a shape where the two differ to notice
  * (rchillyard/Gryphon#144).
  *
  * So the point of this spec is not to re-test the traversals. It is to pin the one thing nothing
  * pinned: '''which''' of several defensible maps each traversal returns.
  *
  * ==The fixture, and why it is the right one==
  *
  * The transitive tournament on 0..5 — an edge `i -> j` for every `i < j`. It is the smallest
  * shape where discovery and exploration disagree maximally, because the source is adjacent to
  * every other vertex, so every vertex really is first seen from the source.
  *
  *  - A '''BFS''' tree on it is the star: every vertex is one edge from the source.
  *  - A '''DFS''' tree on it is a Hamiltonian path: the search descends and never needs to
  *    backtrack, since every vertex ahead is reachable from wherever it is.
  *
  * A star and a path are about as far apart as two spanning trees on six vertices can be, which
  * is what makes the fixture decisive: a journal that returns the wrong one cannot look nearly
  * right.
  */
class CameFromSemanticsSpec extends AnyFlatSpec with Matchers:

  private given Evaluable[Int, Int] with
    def evaluate(v: Int): Option[Int] = Some(v)

  /** The transitive tournament on 0..5: an edge from every i to every j > i. */
  private given tournament: GraphNeighbours[Int] = (v: Int) => (v + 1 to 5).iterator

  private def dfsCameFrom: Map[Int, Int] =
    Traversal.dfs(0, JournaledVisitor.withListJournalAndCameFrom[Int, Int], DfsOrder.Pre)
            .asInstanceOf[JournaledVisitor[Int, Int, ListJournal[Mapped[Int, Int]]]]
            .cameFrom.getOrElse(fail("came-from tracking was requested"))

  private def bfsCameFrom: Map[Int, Int] =
    Traversal.bfs(0, JournaledVisitor.withQueueJournalAndCameFrom[Int, Int])
            .asInstanceOf[JournaledVisitor[Int, Int, QueueJournal[Mapped[Int, Int]]]]
            .cameFrom.getOrElse(fail("came-from tracking was requested"))

  /** Follows the came-from chain back from `v`, which must terminate at the root. */
  private def chainToRoot(map: Map[Int, Int], v: Int): List[Int] =
    @tailrec def go(at: Int, acc: List[Int], seen: Set[Int]): List[Int] =
      if seen.contains(at) then fail(s"came-from map has a cycle at $at")
      else map.get(at) match
        case None => at :: acc
        case Some(p) => go(p, at :: acc, seen + at)

    go(v, Nil, Set.empty)

  behavior of "depth-first came-from"

  // The regression test. On the old implementation this map was {1->0, 2->0, 3->0, 4->0, 5->0}.
  it should "be the path the search actually walked, not a star" in {
    dfsCameFrom shouldBe Map(1 -> 0, 2 -> 1, 3 -> 2, 4 -> 3, 5 -> 4)
  }

  // Said structurally rather than by literal, so it still means something if the traversal's
  // neighbour order ever changes: a path is a tree in which no vertex parents two others.
  it should "give each vertex a distinct parent, since a DFS tree here is a path" in {
    val parents = dfsCameFrom.values.toList
    parents.distinct.size shouldBe parents.size
  }

  it should "record only real edges, each from a vertex that precedes its child" in {
    dfsCameFrom.foreach((child, parent) => parent should be < child) // i -> j only for i < j
    dfsCameFrom.keySet shouldBe Set(1, 2, 3, 4, 5) // the source has no parent
  }

  behavior of "breadth-first came-from"

  // Unchanged, and the half that was always right. Kept because this spec is about which map you
  // get, and "BFS still gives the other one" is exactly the thing to protect.
  it should "be the star, which is the shortest-path tree here" in {
    bfsCameFrom shouldBe Map(1 -> 0, 2 -> 0, 3 -> 0, 4 -> 0, 5 -> 0)
  }

  // The property that makes the BFS map worth having: following it back takes the fewest edges.
  // Every vertex is one edge from the source on a tournament, so every chain is length two.
  it should "yield a shortest path for every reached vertex" in {
    for v <- 1 to 5 do chainToRoot(bfsCameFrom, v) shouldBe List(0, v)
  }

  behavior of "the two together"

  // The heart of #55: one mechanism served both and was right for one of them. If these two ever
  // agree on this fixture again, one of them has reverted.
  it should "disagree, because discovery and exploration are different questions" in {
    dfsCameFrom should not be bfsCameFrom
  }

  it should "both reach every vertex, and both be rooted at the source" in {
    for map <- Seq(dfsCameFrom, bfsCameFrom); v <- 1 to 5 do
      chainToRoot(map, v).head shouldBe 0
      chainToRoot(map, v).last shouldBe v
  }

  behavior of "depth-first came-from when the search backtracks"

  // The tournament above never backtracks: every vertex ahead is reachable from wherever the
  // search is, so it walks straight down. That makes it a clean demonstration and an INCOMPLETE
  // test, because the interesting question for last-wins is what happens when the search
  // retreats and resumes from an earlier vertex.
  //
  //     0 -> 1, 2, 3      and      1 -> 3
  //
  // The search expands 0, which reaches 1, 2 and 3. It then expands 1, which reaches 3 again --
  // so 3 has now been reached twice, and the second writer is the one whose frame sits nearest
  // the top of the stack. It descends to 3, finds nothing, BACKTRACKS to 0, and takes 2.
  //
  // The tree is therefore {1 -> 0, 3 -> 1, 2 -> 0}: 2's parent is the source, because nothing
  // reached 2 after the source did, while 3's parent is 1. Last-wins has to get BOTH right, and
  // a naive "always overwrite" that ignored visitedness would not -- which is the case this
  // fixture exists to cover.
  it should "name the vertex each node was expanded from, not merely the most recent writer" in {
    given GraphNeighbours[Int] = (v: Int) =>
      (v match
        case 0 => List(1, 2, 3)
        case 1 => List(3)
        case _ => Nil
      ).iterator

    val cameFrom = Traversal.dfs(0, JournaledVisitor.withListJournalAndCameFrom[Int, Int], DfsOrder.Pre)
            .asInstanceOf[JournaledVisitor[Int, Int, ListJournal[Mapped[Int, Int]]]]
            .cameFrom.getOrElse(fail("came-from tracking was requested"))

    cameFrom shouldBe Map(1 -> 0, 3 -> 1, 2 -> 0)

    // Note what this fixture also establishes: a DFS tree is NOT generally a path. Here vertex 0
    // parents two children. The tournament's path is a property of that graph and that neighbour
    // order, not of depth-first search -- worth stating, because a consumer test asserting "no
    // vertex parents two others" would be asserting something false in general.
    cameFrom.values.count(_ == 0) shouldBe 2
  }
