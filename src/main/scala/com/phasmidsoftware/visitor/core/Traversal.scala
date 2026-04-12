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
  * VisitedSet, Visitor, and CostUpdate. Zero domain knowledge.
  *
  * The choice of frontier F determines the traversal order:
  * F = Queue     → BFS
  * F = Stack     → DFS
  * F = PrioQueue → best-first (requires Ordering[V] in scope)
  *
  * Two visitor events are fired during traversal:
  *   - `visit(v)`             — when a vertex is settled (dequeued/popped)
  *   - `discover(v, cameFrom)` — when a vertex is first seen as a neighbour
  *
  * NOTE that there are three independent traversal engines which should be maintained
  * in parallel as much as possible: `traverse`, `traverseTree`, and `dfs`.
  */
object Traversal:

  /**
    * Traverses a (homogeneous) graph structure starting from a given node, visiting nodes
    * iteratively and accumulating results into a visitor.
    *
    * After each node's neighbours are offered to the frontier, `cu.update` is called
    * to apply any priority improvements (e.g. `decreaseKey` in Dijkstra/Prim).
    * For DFS and BFS the default no-op [[CostUpdate]] is resolved automatically.
    *
    * The start node is always visited and recorded first, before any goal check or
    * frontier expansion. If `goal(start)` is true, traversal stops immediately after
    * recording the start node — no neighbours are expanded.
    *
    * For all subsequent nodes, traversal stops after recording the first node for which
    * `goal(node)` returns true — that node is recorded in the journal but its neighbours
    * are not expanded.
    *
    * `discover(neighbour, node)` is called for each unvisited neighbour before it is
    * offered to the frontier, recording the came-from relationship at discovery time.
    *
    * @param start   The starting node where the traversal begins.
    * @param visitor An instance of `Visitor` that collects results during traversal.
    * @param goal    A predicate that, when true for a visited node, halts the traversal
    *                after recording that node. Defaults to never stopping early.
    * @param nbrs    Typeclass providing the neighbours for each node.
    * @param ev      Typeclass defining how to extract results from each node.
    * @param vs      Typeclass representing the set of visited nodes to prevent revisiting.
    * @param fr      Typeclass representing the frontier structure used for traversal.
    * @param cu      Typeclass for post-settle priority updates (no-op for DFS/BFS).
    * @param initial The initial empty frontier structure for the traversal.
    * @param tracer  Typeclass for tracing the primary parametric type (`V`).
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
                                                             cu: CostUpdate[V, F],
                                                             initial: F[V],
                                                             tracer: Tracer[V] = Tracer.silent
  ): Visitor[V, R, J] =

    tracer.trace(0, s"traverse: start=$start")

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
          tracer.trace(1, s"visiting: $node")
          val newVisitor = vis.visit(node)
          if goal(node) then newVisitor
          else
            val neighbourList = nbrs.neighbours(node).filterNot(newVisited.isVisited).toList
            tracer.trace(2, s"neighbours: $neighbourList")
            // discover each unvisited neighbour before offering to frontier
            val discoveredVisitor = neighbourList.foldLeft(newVisitor)((v, n) => v.discover(n, node))
            val offered = fr.offerAll(rest)(neighbourList)
            val newFrontier = cu.update(offered, node)
            loop(newFrontier, discoveredVisitor, newVisited)

    val startVisitor = visitor.visit(start)
    if goal(start) then startVisitor
    else
      val startNeighbours = nbrs.neighbours(start).toList
      // discover start's neighbours before offering
      val discoveredVisitor = startNeighbours.foldLeft(startVisitor)((v, n) => v.discover(n, start))
      val seedOffered = fr.offerAll(initial)(startNeighbours)
      val seedFrontier = cu.update(seedOffered, start)
      loop(seedFrontier, discoveredVisitor, vs.markVisited(start))

  /**
    * Traverses a tree-like structure starting from a given root node, visiting nodes
    * as specified by the provided `Visitor`.
    *
    * NOTE: traverseTree does not visit the root `start` itself (there is no
    * `Evaluable[H, R]` in scope for `H`), so the visitor is unseeded before
    * the loop begins — unlike `traverse` where `visitor.visit(start)` is called first.
    *
    * The `order` parameter controls pre- or post-order recording, using the same
    * `Either`-tagged stack mechanism as `dfs`:
    *   - `Left(v)`  — expand node: mark visited, push children and a record frame
    *   - `Right(v)` — record node: call `visitor.visit(v)`
    *
    * The seed step uses `rootNbrs` (H → V) to get the initial children from the root.
    * All subsequent steps use `graphNbrs` (V → V).
    *
    * Traversal stops after recording the first `V` node for which `goal(node)`
    * returns true. The goal node is recorded in the journal but its neighbours
    * are not expanded.
    *
    * NOTE: `discover` is not called in `traverseTree` — the tree structure implies
    * the came-from relationship is already known from the tree shape.
    *
    * @param start     The root node where the traversal begins.
    * @param visitor   An instance of `Visitor` that accumulates results during the traversal.
    * @param order     DfsOrder.Pre (default) or DfsOrder.Post.
    * @param goal      A predicate that, when true for a visited node, halts the traversal
    *                  after recording that node. Defaults to never stopping early.
    * @param rootNbrs  A typeclass instance providing neighbour nodes for the root structure.
    * @param graphNbrs A typeclass instance providing neighbour nodes for the graph structure.
    * @param ev        A typeclass representing how to evaluate a node and extract its result.
    * @param vs        A typeclass representing a set of visited nodes to prevent revisits.
    * @return A `Visitor` instance containing the accumulated results of the traversal.
    */
  def traverseTree[H, V, R, J <: Appendable[(V, Option[R])]](
                                                              start: H,
                                                              visitor: Visitor[V, R, J],
                                                              order: DfsOrder = DfsOrder.Pre,
                                                              goal: V => Boolean = (_: V) => false
                                                            )(using
                                                              rootNbrs: Neighbours[H, V],
                                                              graphNbrs: Neighbours[V, V],
                                                              ev: Evaluable[V, R],
                                                              vs: VisitedSet[V],
                                                              tracer: Tracer[V] = Tracer.silent
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

        case Right(node) :: rest =>
          tracer.trace(1, s"record: $node")
          val newVisitor = vis.visit(node)
          if goal(node) then newVisitor
          else loop(rest, newVisitor, visited)

        case Left(node) :: rest =>
          if visited.isVisited(node) then loop(rest, vis, visited)
          else
            val newVisited = visited.markVisited(node)
            val children = graphNbrs.neighbours(node).filterNot(newVisited.isVisited).toList
            tracer.trace(1, s"expand: $node  children=$children")
            val childFrames = children.map(Left(_))
            val newStack = order match
              case DfsOrder.Pre => Right(node) :: (childFrames ::: rest)
              case DfsOrder.Post => childFrames ::: (Right(node) :: rest)
            loop(newStack, vis, newVisited)

    val rootChildren = rootNbrs.neighbours(start).toList
    val rootFrames = rootChildren.map(Left(_))
    loop(rootFrames, visitor, vs)

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
    * `discover(child, node)` is called for each unvisited child at expand time
    * (the `Left(node)` branch), before the child frames are pushed to the stack.
    * This records the came-from relationship at the point of discovery.
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
                                                  vs: VisitedSet[V],
                                                  tracer: Tracer[V] = Tracer.silent
  ): Visitor[V, R, J] =

    tracer.trace(0, s"dfs ($order): start=$start")

    type Frame = Either[V, V]

    @annotation.tailrec
    def loop(
              stack: List[Frame],
              vis: Visitor[V, R, J],
              visited: VisitedSet[V]
            ): Visitor[V, R, J] =
      stack match
        case Nil => vis

        case Right(node) :: rest =>
          tracer.trace(1, s"record: $node")
          val newVisitor = vis.visit(node)
          if goal(node) then newVisitor
          else loop(rest, newVisitor, visited)

        case Left(node) :: rest =>
          if visited.isVisited(node) then loop(rest, vis, visited)
          else
            val newVisited = visited.markVisited(node)
            val children = nbrs.neighbours(node).filterNot(newVisited.isVisited).toList
            tracer.trace(1, s"expand: $node  children=$children")
            // discover each child before pushing to stack
            val discoveredVis = children.foldLeft(vis)((v, child) => v.discover(child, node))
            val childFrames = children.map(Left(_))
            val newStack = order match
              case DfsOrder.Pre => Right(node) :: (childFrames ::: rest)
              case DfsOrder.Post => childFrames ::: (Right(node) :: rest)
            loop(newStack, discoveredVis, newVisited)

    // discover start's children before beginning
    val startChildren = nbrs.neighbours(start).toList
    val discoveredVisitor = startChildren.foldLeft(visitor)((v, child) => v.discover(child, start))
    loop(List(Left(start)), discoveredVisitor, vs)

  /**
    * Breadth-first search traversal.
    *
    * `discover(neighbour, node)` is called for each unvisited neighbour before
    * it is offered to the frontier, recording the came-from relationship at
    * discovery time.
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
                                                  vs: VisitedSet[V],
                                                  tracer: Tracer[V] = Tracer.silent
                                                ): Visitor[V, R, J] =
    given Queue[V] = Queue.empty

    traverse[V, R, J, Queue](start, visitor, goal)

  /**
    * Best-first / min-priority-queue traversal. Smallest element dequeued first.
    * Requires Ordering[V] in scope.
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
                                                                  vs: VisitedSet[V],
                                                                  tracer: Tracer[V] = Tracer.silent
  ): Visitor[V, R, J] =
    given PrioQueue[V] = PrioQueue.empty[V]
    traverse[V, R, J, PrioQueue](start, visitor, goal)

  /**
    * Best-first / max-priority-queue traversal. Largest element dequeued first.
    * Requires Ordering[V] in scope.
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
                                                                     vs: VisitedSet[V],
                                                                     tracer: Tracer[V] = Tracer.silent
  ): Visitor[V, R, J] =
    given PrioQueue[V] = PrioQueue.emptyMax[V]
    traverse[V, R, J, PrioQueue](start, visitor, goal)

  /**
    * Best-first traversal with an explicit `CostUpdate` — for Dijkstra and Prim.
    *
    * The frontier element type `W` is typically `(E, V)` where `E` is the cost
    * type and `V` is the vertex type. The caller supplies:
    *   - `start`: the seed element, e.g. `(zero, startVertex)`
    *   - a `given IndexedPrioQueue[W] = IndexedPrioQueue.empty[W]` (always empty;
    *     `traverse` seeds the frontier from `start`'s neighbours)
    *   - a `given CostUpdate[W, IndexedPrioQueue]` that calls `decreaseKey` after
    *     each settle
    *   - `Neighbours[W, W]` expanding `(cost, vertex)` to `(newCost, neighbour)` pairs
    *
    * NOTE: `discover` is not called in `bestFirstWeighted` — the came-from
    * relationship for weighted traversals is already tracked via the `pred` map
    * inside `WeightedTraversal.CostUpdate`.
    *
    * @param goal optional early-termination predicate.
    */
  def bestFirstWeighted[W: Ordering, R, J <: Appendable[(W, Option[R])]](
                                                                          start: W,
                                                                          visitor: Visitor[W, R, J],
                                                                          goal: W => Boolean = (_: W) => false
                                                                        )(using
                                                                          nbrs: GraphNeighbours[W],
                                                                          ev: Evaluable[W, R],
                                                                          vs: VisitedSet[W],
                                                                          cu: CostUpdate[W, IndexedPrioQueue],
                                                                          initial: IndexedPrioQueue[W],
                                                                          tracer: Tracer[W] = Tracer.silent
  ): Visitor[W, R, J] =
    traverse[W, R, J, IndexedPrioQueue](start, visitor, goal)