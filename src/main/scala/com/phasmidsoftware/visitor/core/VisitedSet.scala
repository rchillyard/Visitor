package com.phasmidsoftware.visitor.core

/**
  * Typeclass: a set of visited nodes, used to prevent revisiting in cycle-containing graphs.
  * Immutable — markVisited returns a new instance.
  *
  * @tparam V the node type
  */
trait VisitedSet[V]:
  def isVisited(v: V): Boolean
  def markVisited(v: V): VisitedSet[V]

/**
  * Companion utilities for creating [[VisitedSet]] instances.
  *
  * A `VisitedSet` records which graph nodes have already been encountered during
  * traversal so algorithms can avoid revisiting nodes, including in cyclic graphs.
  * The values returned by this companion are immutable; operations that add a node
  * produce a new `VisitedSet` rather than modifying an existing one.
  *
  * Use [[empty]] to create a set with no visited nodes, or [[from]] to wrap an
  * existing `Set` of already-visited nodes without rebuilding it element by element.
  */
object VisitedSet:
  /**
    * An empty `VisitedSet`.
    *
    * @tparam V the node type
    */
  def empty[V]: VisitedSet[V] = SetVisitedSet(Set.empty)

  /**
    * A `VisitedSet` containing exactly `visited`, built in O(1).
    *
    * Without this, a caller holding a `Set` of already-visited nodes — for instance one running
    * a traversal over each component of a graph in turn — could only rebuild a `VisitedSet` by
    * folding `markVisited` over it, allocating once per element. The backing implementation is
    * private, so there was no way to do it directly.
    *
    * See [[https://github.com/rchillyard/Visitor/issues/22 issue #22]]; prefer
    * [[Traversal.dfsTracking]], which hands the visited set back rather than making you rebuild
    * one at all.
    *
    * @param visited the nodes to mark as already visited
    * @tparam V the node type
    */
  def from[V](visited: Set[V]): VisitedSet[V] = SetVisitedSet(visited)

/** Default given: immutable Set-backed VisitedSet for plain node types. */
given [V]: VisitedSet[V] = SetVisitedSet(Set.empty)

/**
  * Immutable `VisitedSet` implementation backed by a Scala `Set`.
  *
  * Membership and insertion semantics are determined by the underlying set.
  * Marking a value as visited returns a new visited-set instance and leaves the
  * original instance unchanged.
  *
  * @tparam V the node type tracked by this visited set
  */
private case class SetVisitedSet[V](visited: Set[V]) extends VisitedSet[V]:
  def isVisited(v: V): Boolean = visited.contains(v)
  def markVisited(v: V): VisitedSet[V] = copy(visited + v)

/**
  * A `VisitedSet` for `(E, V)` frontier tuples used in weighted traversals
  * (Dijkstra, Prim). Visited-ness is tracked on the vertex `V` alone —
  * the cost component `E` is ignored — so that stale (higher-cost) copies
  * of the same vertex in the frontier are correctly recognised as already visited.
  *
  * This given has higher priority than the plain `VisitedSet[V]` given because
  * it is more specific: it matches only `(E, V)` pairs, not arbitrary `V`.
  * It is resolved automatically whenever `Traversal` is invoked with a tuple
  * frontier element type.
  *
  * @tparam E the cost / edge-weight type
  * @tparam V the vertex type
  */
given [E, V]: VisitedSet[(E, V)] = TupleVisitedSet(Set.empty)

/**
  * Visited-set implementation for values represented as `(E, V)` tuples.
  *
  * This implementation treats the second tuple component as the identity of the
  * visited node and ignores the first component when testing or recording
  * visitation. It is useful when traversal items carry auxiliary information,
  * such as an edge, label, or predecessor, alongside the actual vertex to be
  * tracked.
  *
  * The set is immutable: marking a tuple as visited returns a new visited-set
  * instance containing the tuple's second component.
  *
  * @tparam E the type of the auxiliary tuple component
  * @tparam V the type of the visited node component
  * @param visited the set of node components already visited
  */
private case class TupleVisitedSet[E, V](visited: Set[V]) extends VisitedSet[(E, V)]:
  def isVisited(ev: (E, V)): Boolean = visited.contains(ev._2)
  def markVisited(ev: (E, V)): VisitedSet[(E, V)] = copy(visited + ev._2)
