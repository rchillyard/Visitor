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
    * Traverses a (homogeneous) graph structure starting from a given node, visiting nodes iteratively
    * and accumulating results into a visitor. The traversal is controlled by the
    * concepts of a frontier, visited nodes, and neighbours of nodes.
    *
    * NOTE: Homogeneous graph traversal — H and V are the same type.
    * The =:= witness lets the compiler accept V where H is expected.
    *
    * @param start   The starting node where the traversal begins.
    * @param visitor An instance of `Visitor` that collects results during traversal.
    * @param nbrs    A typeclass instance providing the neighbours for each node in the graph.
    * @param ev      A typeclass instance defining how to extract results from each node.
    * @param vs      A typeclass instance representing the set of visited nodes to prevent revisiting.
    * @param fr      A typeclass instance representing the frontier structure used for traversal.
    * @param initial The initial empty frontier structure for the traversal.
    * @return A `Visitor` instance containing the accumulated results after traversal completes.
    */
  def traverse[V, R, J <: Appendable[(V, Option[R])], F[_]](
                                                             start: V,
                                                             visitor: Visitor[V, R, J]
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
          val newFrontier = fr.offerAll(rest)(
            nbrs.neighbours(node).filterNot(newVisited.isVisited).toList
          )
          loop(newFrontier, newVisitor, newVisited)

    val seedFrontier = fr.offerAll(initial)(nbrs.neighbours(start).toList)
    loop(seedFrontier, visitor.visit(start), vs.markVisited(start))


  /**
    * Traverses a tree-like structure starting from a given root node, visiting nodes
    * as specified by the provided `Visitor`. The traversal is driven by the concepts
    * of a frontier (e.g., queue or stack), visited nodes, and neighbours of nodes.
    * NOTE that traverseTree doesn't visit start itself (no Evaluable[H, R] in scope), so visitor and vs are passed unseedeed to loop —
    * unlike traverse where we call visitor.visit(start) and vs.markVisited(start) before entering the loop.
    *
    * @param start     The root node where the traversal begins.
    * @param visitor   An instance of `Visitor` that accumulates results during the traversal.
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
                                                                    visitor: Visitor[V, R, J]
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
          val newFrontier = fr.offerAll(rest)(
            graphNbrs.neighbours(node).filterNot(newVisited.isVisited).toList
          )
          loop(newFrontier, newVisitor, newVisited)

    val seedFrontier = fr.offerAll(initial)(rootNbrs.neighbours(start).toList)
    loop(seedFrontier, visitor, vs)

  // ----------------------------------------------------------
  // Convenience entry points
  // ----------------------------------------------------------

  /** Breadth-first search. */
  def bfs[V, R, J <: Appendable[(V, Option[R])]](
                                                  start: V,
                                                  visitor: Visitor[V, R, J]
                                                )(using
                                                  nbrs: GraphNeighbours[V],
                                                  ev: Evaluable[V, R],
                                                  vs: VisitedSet[V]
                                                ): Visitor[V, R, J] =
    given Queue[V] = Queue.empty

    traverse[V, R, J, Queue](start, visitor)

  /**
    * Depth-first search with configurable pre- or post-order recording.
    *
    * The internal stack holds `Either[V, V]` frames:
    *   - `Left(v)`  means "expand v — push its children and a record frame"
    *   - `Right(v)` means "record v in the visitor now"
    *
    * Pre-order:  record before expanding  → push Right(v), then Left(children)
    * so Right(v) is on top and visited first.
    * Post-order: record after expanding   → push children as Left, then Right(v)
    * so children are expanded before v is recorded.
    *
    * @param order DfsOrder.Pre (default) or DfsOrder.Post
    */
  def dfs[V, R, J <: Appendable[(V, Option[R])]](
                                                  start: V,
                                                  visitor: Visitor[V, R, J],
                                                  order: DfsOrder = DfsOrder.Pre
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

        // Record frame: visit the node (already marked visited when expanded)
        case Right(node) :: rest =>
          loop(rest, vis.visit(node), visited)

        // Expand frame: if already visited skip; otherwise mark and push frames
        case Left(node) :: rest =>
          if visited.isVisited(node) then loop(rest, vis, visited)
          else
            val newVisited = visited.markVisited(node)
            val children = nbrs.neighbours(node).filterNot(newVisited.isVisited).toList
            val childFrames = children.map(Left(_)) // no .reverse
            val newStack = order match
              case DfsOrder.Pre =>
                Right(node) :: (childFrames ::: rest)
              case DfsOrder.Post =>
                childFrames ::: (Right(node) :: rest)
            loop(newStack, vis, newVisited)

    loop(List(Left(start)), visitor, vs)

  /** Best-first / priority-queue traversal. Requires Ordering[V] in scope. */
  def bestFirst[V: Ordering, R, J <: Appendable[(V, Option[R])]](
                                                                  start: V,
                                                                  visitor: Visitor[V, R, J]
                                                                )(using
                                                                  nbrs: GraphNeighbours[V],
                                                                  ev: Evaluable[V, R],
                                                                  vs: VisitedSet[V]
                                                                ): Visitor[V, R, J] =
    given PrioQueue[V] = PrioQueue.empty[V] // Ordering[V] is in scope via : Ordering

    traverse[V, R, J, PrioQueue](start, visitor)

  /** Max-priority / best-first traversal. Largest element dequeued first. */
  def bestFirstMax[V: Ordering, R, J <: Appendable[(V, Option[R])]](
                                                                     start: V,
                                                                     visitor: Visitor[V, R, J]
                                                                   )(using
                                                                     nbrs: GraphNeighbours[V],
                                                                     ev: Evaluable[V, R],
                                                                     vs: VisitedSet[V]
                                                                   ): Visitor[V, R, J] =
    given PrioQueue[V] = PrioQueue.emptyMax[V]

    traverse[V, R, J, PrioQueue](start, visitor)
    