package com.phasmidsoftware.visitor.core

/**
  * Typeclass: given a structure H, yield the neighbouring nodes of type V.
  * This is the "how do we traverse" concern — completely decoupled from domain.
  *
  * For homogeneous graphs (node points to node), use GraphNeighbours[V] = Neighbours[V, V].
  * For trees, H might be Tree[A] and V might be A.
  *
  * @tparam H the container / structure type
  * @tparam V the neighbour / child type
  */
trait Neighbours[H, V]:
  def neighbours(h: H): Iterator[V]

/** Convenience alias for homogeneous graphs. */
type GraphNeighbours[V] = Neighbours[V, V]

/** American English alias for [[Neighbours]] */
type Neighbors[H, V] = Neighbours[H, V]

/** American English alias for [[GraphNeighbours]] */
type GraphNeighbors[V] = GraphNeighbours[V]

/**
  * Typeclass: a node with a left and a right child, either of which may be absent.
  *
  * This is what in-order traversal needs and [[Neighbours]] cannot express: position, not
  * merely cardinality. Given `neighbours` alone, a node with one child says nothing about which
  * side that child is on, yet left-only and right-only yield different in-order sequences.
  * Counting the children cannot recover the distinction — it admits the very case that breaks —
  * so the property is asserted here, in the type: an instance must say which child is which,
  * and a third child cannot be presented at all. [[Traversal.inOrder]] therefore needs no
  * runtime guard, and is total.
  *
  * `neighbours` is supplied, left before right, so a single instance also drives
  * [[Traversal.dfs]], [[Traversal.bfs]] and the rest.
  *
  * See [[https://github.com/rchillyard/Visitor/issues/48 issue #48]].
  *
  * @tparam V the node type
  */
trait BinaryNeighbours[V] extends GraphNeighbours[V]:
  /**
    * The left child of `v`, if it has one.
    *
    * @param v the node whose left child is wanted.
    * @return the left child, or `None` if there is none.
    */
  def left(v: V): Option[V]

  /**
    * The right child of `v`, if it has one.
    *
    * @param v the node whose right child is wanted.
    * @return the right child, or `None` if there is none.
    */
  def right(v: V): Option[V]

  /**
    * The children of `v`, left before right.
    *
    * Defined in terms of `left` and `right` so that a binary tree needs only the one instance:
    * the engines that know nothing of left and right see an ordinary adjacency.
    *
    * @param v the node whose children are wanted.
    * @return the children of `v`, in left-to-right order.
    */
  def neighbours(v: V): Iterator[V] = left(v).iterator ++ right(v).iterator

/** American English alias for [[BinaryNeighbours]] */
type BinaryNeighbors[V] = BinaryNeighbours[V]
