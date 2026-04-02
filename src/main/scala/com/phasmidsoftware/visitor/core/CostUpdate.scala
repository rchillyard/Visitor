package com.phasmidsoftware.visitor.core

// ============================================================
// CostUpdate typeclass
// ============================================================

/**
  * Typeclass: after a node is settled (dequeued and marked visited) and its
  * neighbours have been offered to the frontier, optionally update the
  * priorities of frontier entries that have improved.
  *
  * This typeclass exists to support `decreaseKey` in Dijkstra- and Prim-style
  * traversals. It keeps all domain knowledge (cost maps, edge weights, the
  * notion of "improvement") out of [[Traversal]] itself.
  *
  * For DFS and BFS the default no-op given is resolved automatically.
  * For Dijkstra and Prim, the `GraphTraversal` implementation supplies a
  * concrete `given CostUpdate[W, IndexedPrioQueue]` (where `W = (E, V)`) that
  * closes over a secondary vertex→cost map and calls [[IndexedPrioQueue.decreaseKey]]
  * for any neighbour whose cost has improved since it was first offered.
  *
  * @tparam W the frontier element type (e.g. `(E, V)` for weighted traversals,
  *           or plain `V` for DFS / BFS)
  *
  * @tparam F the frontier container type (e.g. [[PrioQueue]], [[Stack]], Queue)
  */
trait CostUpdate[W, F[_]]:
  /**
    * Given the current frontier and the node `w` that was just settled,
    * return an updated frontier with any improved priorities applied.
    *
    * @param frontier the frontier after `w`'s neighbours have been offered
    * @param w        the element that was just settled
    * @return the frontier with any `decreaseKey` updates applied
    */
  def update(frontier: F[W], w: W): F[W]

/**
  * Default no-op implementation.
  * Resolved automatically for DFS (`Stack`) and BFS (`Queue`) traversals,
  * and for any weighted traversal that does not need re-keying.
  */
given [W, F[_]]: CostUpdate[W, F] with
  def update(frontier: F[W], w: W): F[W] = frontier