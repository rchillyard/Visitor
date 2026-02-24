package com.phasmidsoftware.visitor.core

import scala.collection.immutable.Queue

// ============================================================
// Traversal engine
// ============================================================

/**
  * Controls whether DFS records a node before or after processing its children.
  *
  *  - Pre:  node is recorded before its children are expanded (default)
  *  - Post: node is recorded after all its descendants (useful for topological sort)
  */
enum DfsOrder:
  case Pre, Post

/**
  * The traversal engine. Knows only about Frontier, Neighbours, Evaluable,
  * VisitedSet, and Visitor. Zero domain knowledge.
  *
  * The choice of frontier F determines the traversal order:
  * F = Queue     → BFS
  * F = Stack     → DFS
  * F = PrioQueue → best-first (requires Ordering[V] in scope)
  */
object Traversal:

  /**
    * Traverses a (homogeneous) graph structure starting from a given node, visiting nodes
    * iteratively and accumulating results into a visitor.
    *
    * The start node is always visited and recorded first, before any goal check or
    * frontier expansion. If `goal(start)` is true, traversal stops immediately after
    * recording the start node — no neighbours are expanded.
    *
    * For all subsequent nodes, traversal stops after recording the first node for which
    * `goal(node)` returns true — that node is recorded in the journal but its neighbours
    * are not expanded.
    *
    * @param start   The starting node where the traversal begins.
    * @param visitor An instance of `Visitor` that collects results during traversal.
    * @param goal    A predicate that, when true for a visited node, halts the traversal
    *                after recording that node. Defaults to never stopping early.
    * @param nbrs    A typeclass instance providing the neighbours for each node in the graph.
    * @param ev      A typeclass instance defining how to extract results from each node.
    * @param vs      A typeclass instance representing the set of visited nodes to prevent revisiting.
    * @param fr      A typeclass instance representing the frontier structure used for traversal.
    * @param initial The initial empty frontier structure for the traversal.
    * @return A `Visitor` instance containing the accumulated results after traversal completes.
    */
  def traverse[V, R, J <: Appendable[(V, Option[R])], F[_]](
                                                             start: V,
                                                             visitor: Visitor[V, R, J],
                                                             goal: V => Boolean = (_: V) => false
                                                           )(using
                                                             nbrs: Neighbours[V, V],
                                                             ev: Evaluable[V, R],
                                                             vs: VisitedSet[V],
                                                             fr: Frontier[F],
                                                             initial: F[V]
                                                           ): Visitor[V, R, J] =

    @annotation.tailrec
    def loop(
              frontier: F[V],
              vis: Visitor[V, R, J],
              visited: VisitedSet[V]
            ): Visitor[V, R, J] =
      if fr.isEmpty(frontier) then vis
      else
        val (node, rest) = fr.take(frontier)
        if visited.isVisited(node) then loop(rest, vis, visited)
        else
          val newVisited = visited.markVisited(node)
          val newVisitor = vis.visit(node)
          if goal(node) then newVisitor
          else
            val newFrontier = fr.offerAll(rest)(
              nbrs.neighbours(node).filterNot(newVisited.isVisited).toList
            )
            loop(newFrontier, newVisitor, newVisited)

    // Visit start first, then check goal before entering the loop.
    // This ensures goal(start) stops traversal immediately with just the start node recorded.
    val startVisitor = visitor.visit(start)
    if goal(start) then startVisitor
    else
      val seedFrontier = fr.offerAll(initial)(nbrs.neighbours(start).toList)
      loop(seedFrontier, startVisitor, vs.markVisited(start))

  /**
    * Traverses a tree-like structure starting from a given root node, visiting nodes
    * as specified by the provided `Visitor`.
    *
    * NOTE: traverseTree does not visit the root `start` itself (there is no
    * `Evaluable[H, R]` in scope for `H`), so `visitor` and `vs` are passed
    * unseeded to the loop — unlike `traverse` where `visitor.visit(start)` and
    * `vs.markVisited(start)` are called before entering the loop.
    *
    * Traversal stops after recording the first `V` node for which `goal(node)`
    * returns true. The goal node is recorded in the journal but its neighbours
    * are not expanded.
    *
    * @param start     The root node where the traversal begins.
    * @param visitor   An instance of `Visitor` that accumulates results during the traversal.
    * @param goal      A predicate that, when true for a visited node, halts the traversal
    *                  after recording that node. Defaults to never stopping early.
    * @param rootNbrs  A typeclass instance providing neighbour nodes for the root structure.
    * @param graphNbrs A typeclass instance providing neighbour nodes for the graph structure.
    * @param ev        A typeclass representing how to evaluate a node and extract its result.
    * @param vs        A typeclass representing a set of visited nodes to prevent revisits.
    * @param fr        A typeclass representing the frontier data structure used for traversal.
    * @param initial   The initial empty frontier structure.
    * @return A `Visitor` instance containing the accumulated results of the traversal.
    */
  def traverseTree[H, V, R, J <: Appendable[(V, Option[R])], F[_]](
                                                                    start: H,
                                                                    visitor: Visitor[V, R, J],
                                                                    goal: V => Boolean = (_: V) => false
                                                                  )(using
                                                                    rootNbrs: Neighbours[H, V],
                                                                    graphNbrs: Neighbours[V, V],
                                                                    ev: Evaluable[V, R],
                                                                    vs: VisitedSet[V],
                                                                    fr: Frontier[F],
                                                                    initial: F[V]
                                                                  ): Visitor[V, R, J] =

    @annotation.tailrec
    def loop(
              frontier: F[V],
              vis: Visitor[V, R, J],
              visited: VisitedSet[V]
            ): Visitor[V, R, J] =
      if fr.isEmpty(frontier) then vis
      else
        val (node, rest) = fr.take(frontier)
        if visited.isVisited(node) then loop(rest, vis, visited)
        else
          val newVisited = visited.markVisited(node)
          val newVisitor = vis.visit(node)
          if goal(node) then newVisitor
          else
            val newFrontier = fr.offerAll(rest)(
              graphNbrs.neighbours(node).filterNot(newVisited.isVisited).toList
            )
            loop(newFrontier, newVisitor, newVisited)

    val seedFrontier = fr.offerAll(initial)(rootNbrs.neighbours(start).toList)
    loop(seedFrontier, visitor, vs)

  // ----------------------------------------------------------
  // Convenience entry points
  // ----------------------------------------------------------

  /**
    * Breadth-first search.
    *
    * The start node is always recorded first. If `goal(start)` is true, traversal
    * stops immediately with just the start node in the journal.
    * For all other nodes, traversal stops after recording the first node satisfying `goal`.
    * The goal node is always recorded before traversal halts.
    *
    * @param goal optional early-termination predicate. Defaults to never stopping early.
    */
  def bfs[V, R, J <: Appendable[(V, Option[R])]](
                                                  start: V,
                                                  visitor: Visitor[V, R, J],
                                                  goal: V => Boolean = (_: V) => false
                                                )(using
                                                  nbrs: GraphNeighbours[V],
                                                  ev: Evaluable[V, R],
                                                  vs: VisitedSet[V]
                                                ): Visitor[V, R, J] =
    given Queue[V] = Queue.empty

    traverse[V, R, J, Queue](start, visitor, goal)

  /**
    * Depth-first search with configurable pre- or post-order recording.
    *
    * The internal stack holds `Either[V, V]` frames:
    *   - `Left(v)`  means "expand v — push its children and a record frame"
    *   - `Right(v)` means "record v in the visitor now"
    *
    * Pre-order:  record before expanding → Right(v) on top, visited first.
    * Post-order: record after expanding  → children expanded before v is recorded.
    *
    * The goal predicate is checked on `Right` (record) frames — i.e. after a node
    * is visited. Traversal halts after recording the first node satisfying `goal`;
    * its remaining siblings and their subtrees are not visited.
    *
    * @param order DfsOrder.Pre (default) or DfsOrder.Post
    * @param goal  optional early-termination predicate. Defaults to never stopping early.
    */
  def dfs[V, R, J <: Appendable[(V, Option[R])]](
                                                  start: V,
                                                  visitor: Visitor[V, R, J],
                                                  order: DfsOrder = DfsOrder.Pre,
                                                  goal: V => Boolean = (_: V) => false
                                                )(using
                                                  nbrs: GraphNeighbours[V],
                                                  ev: Evaluable[V, R],
                                                  vs: VisitedSet[V]
                                                ): Visitor[V, R, J] =

    type Frame = Either[V, V]

    @annotation.tailrec
    def loop(
              stack: List[Frame],
              vis: Visitor[V, R, J],
              visited: VisitedSet[V]
            ): Visitor[V, R, J] =
      stack match
        case Nil => vis

        // Record frame: visit the node, then check goal
        case Right(node) :: rest =>
          val newVisitor = vis.visit(node)
          if goal(node) then newVisitor
          else loop(rest, newVisitor, visited)

        // Expand frame: if already visited skip; otherwise mark and push frames
        case Left(node) :: rest =>
          if visited.isVisited(node) then loop(rest, vis, visited)
          else
            val newVisited = visited.markVisited(node)
            val children = nbrs.neighbours(node).filterNot(newVisited.isVisited).toList
            val childFrames = children.map(Left(_))
            val newStack = order match
              case DfsOrder.Pre => Right(node) :: (childFrames ::: rest)
              case DfsOrder.Post => childFrames ::: (Right(node) :: rest)
            loop(newStack, vis, newVisited)

    loop(List(Left(start)), visitor, vs)

  /**
    * Best-first / min-priority-queue traversal. Smallest element dequeued first.
    * Requires Ordering[V] in scope.
    *
    * The start node is always recorded first. If `goal(start)` is true, traversal
    * stops immediately. For all other nodes, traversal stops after recording the
    * first node satisfying `goal`.
    *
    * @param goal optional early-termination predicate. Defaults to never stopping early.
    */
  def bestFirst[V: Ordering, R, J <: Appendable[(V, Option[R])]](
                                                                  start: V,
                                                                  visitor: Visitor[V, R, J],
                                                                  goal: V => Boolean = (_: V) => false
                                                                )(using
                                                                  nbrs: GraphNeighbours[V],
                                                                  ev: Evaluable[V, R],
                                                                  vs: VisitedSet[V]
                                                                ): Visitor[V, R, J] =
    given PrioQueue[V] = PrioQueue.empty[V]

    traverse[V, R, J, PrioQueue](start, visitor, goal)

  /**
    * Best-first / max-priority-queue traversal. Largest element dequeued first.
    * Requires Ordering[V] in scope.
    *
    * The start node is always recorded first. If `goal(start)` is true, traversal
    * stops immediately. For all other nodes, traversal stops after recording the
    * first node satisfying `goal`.
    *
    * @param goal optional early-termination predicate. Defaults to never stopping early.
    */
  def bestFirstMax[V: Ordering, R, J <: Appendable[(V, Option[R])]](
                                                                     start: V,
                                                                     visitor: Visitor[V, R, J],
                                                                     goal: V => Boolean = (_: V) => false
                                                                   )(using
                                                                     nbrs: GraphNeighbours[V],
                                                                     ev: Evaluable[V, R],
                                                                     vs: VisitedSet[V]
                                                                   ): Visitor[V, R, J] =
    given PrioQueue[V] = PrioQueue.emptyMax[V]

    traverse[V, R, J, PrioQueue](start, visitor, goal)