package com.phasmidsoftware.visitor.core

/**
  * Convenience entry points for traversals whose answer is simply the order nodes were visited.
  *
  * [[Traversal]] is the engine: it takes a [[Visitor]], threads it through the traversal and
  * hands it back, so the caller chooses what is recorded and reads the journal afterwards. That
  * generality is the point, but it is more than most callers want — if all you need is "which
  * nodes, in what order", this object is the short way to ask.
  *
  * Each method builds a [[SimpleVisitor]], runs the corresponding [[Traversal]] entry point and
  * returns the journal as a `List[V]` in visit order. No [[Evaluable]] is required, and no pair
  * is allocated per visit.
  *
  * These mirror the Java façade's `Traversals`, which offers the same three traversals to Java
  * callers.
  *
  * {{{
  * given Neighbours[String, String] = v => graph.getOrElse(v, Nil).iterator
  * Traverse.bfs("A")  // List(A, B, C, D)
  * }}}
  *
  * For came-from maps, evaluated results or early termination with a richer journal, use
  * [[Traversal]] with a [[JournaledVisitor]] directly.
  */
object Traverse:

  /**
    * Breadth-first traversal from `start`, returning the nodes in visit order.
    *
    * @param start the node to start from
    * @param goal  optional early-termination predicate; the first node it accepts is recorded
    *              and the traversal stops. Defaults to never stopping early.
    * @param nbrs  the adjacency
    * @tparam V the node type
    * @return the visited nodes, in breadth-first order
    */
  def bfs[V](start: V, goal: V => Boolean = (_: V) => false)(using nbrs: GraphNeighbours[V]): List[V] =
    Traversal.bfs(start, SimpleVisitor.withQueueJournal[V], goal).result.toList

  /**
    * Depth-first traversal from `start`, returning the nodes in visit order.
    *
    * @param start the node to start from
    * @param order pre- or post-order recording. Defaults to pre-order.
    * @param goal  optional early-termination predicate. Defaults to never stopping early.
    * @param nbrs  the adjacency
    * @tparam V the node type
    * @return the visited nodes, in depth-first order
    */
  def dfs[V](start: V, order: DfsOrder = DfsOrder.Pre, goal: V => Boolean = (_: V) => false)(using nbrs: GraphNeighbours[V]): List[V] =
    Traversal.dfs(start, SimpleVisitor.withQueueJournal[V], order, goal).result.toList

  /**
    * Depth-first traversal of a tree whose root may have a different type from its descendants.
    *
    * The root itself is not visited; `rootNbrs` yields the nodes the traversal begins from.
    *
    * '''If the root has the same type as its descendants, use [[dfs]] instead: it records the
    * root.''' This entry point is for a root that is not itself a node — a `Tree` whose children
    * are `String`, say — where there is nothing of type `V` to record for it. Given a homogeneous
    * adjacency, `dfs(root, order, goal)` returns exactly this list with the root included (first
    * in pre-order, last in post-order). See
    * [[https://github.com/rchillyard/Visitor/issues/30 issue #30]].
    *
    * @param root      the root structure
    * @param order     pre- or post-order recording. Defaults to pre-order.
    * @param goal      optional early-termination predicate. Defaults to never stopping early.
    * @param rootNbrs  the children of the root
    * @param graphNbrs the children of each node below the root
    * @tparam H the root type
    * @tparam V the node type
    * @return the visited nodes, in depth-first order
    */
  def tree[H, V](root: H, order: DfsOrder = DfsOrder.Pre, goal: V => Boolean = (_: V) => false)(using
                                                                                               rootNbrs: Neighbours[H, V],
                                                                                               graphNbrs: Neighbours[V, V]): List[V] =
    Traversal.traverseTree(root, SimpleVisitor.withQueueJournal[V], order, goal).result.toList
