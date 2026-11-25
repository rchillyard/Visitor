package com.phasmidsoftware.visitor.core

import scala.collection.immutable.Queue

// ============================================================
// Core typeclasses for the various behaviours
// ============================================================

/**
  * Typeclass: given a node of type V, extract an optional result of type R.
  * This is the "what do we want to know about a node" concern.
  *
  * @tparam V the node type
  * @tparam R the result type
  */
trait Evaluable[V, R]:
  def evaluate(v: V): Option[R]

/**
  * Typeclass: like [[Evaluable]], but also given the node this one was first reached from.
  *
  * Some algorithms decide on the strength of *how* they arrived, not just where they are —
  * cycle detection in an undirected graph is "a back edge to anything other than my parent",
  * and edge classification (tree / back / forward / cross) is the same family. `Evaluable` sees
  * the node alone and cannot express those.
  *
  * `from` is `None` for the start node, which was reached from nowhere, and for any node whose
  * predecessor the visitor did not record. It is the *first* node this one was discovered from:
  * a node reached again by another route keeps its original predecessor, matching
  * [[CameFromJournal]].
  *
  * Use it with [[ParentedVisitor]], which records predecessors at discovery and supplies them
  * here at visit time. No change to [[Traversal]] is needed: the engine already calls
  * `discover(v, parent)` before `visit(v)` for every non-start node.
  *
  * See [[https://github.com/rchillyard/Visitor/issues/20 issue #20]]. Note that this gives the
  * predecessor *node*, not the edge traversed — Visitor has no notion of an edge, since
  * [[Neighbours]] yields nodes. An algorithm needing the edge must carry it in `V`.
  *
  * @tparam V the node type
  * @tparam R the result type
  */
trait EvaluableFrom[V, R]:
  def evaluate(from: Option[V], v: V): Option[R]

/**
  * Companion utilities for creating [[EvaluableFrom]] instances.
  *
  * Provides adapters for cases where evaluation either does not depend on the
  * predecessor node or needs direct access to the predecessor recorded by a
  * traversal visitor.
  */
object EvaluableFrom:
  /**
    * Adapts an ordinary [[Evaluable]], ignoring the predecessor.
    *
    * @param ev the evaluable to adapt
    * @tparam V the node type
    * @tparam R the result type
    */
  def ignoringParent[V, R](ev: Evaluable[V, R]): EvaluableFrom[V, R] =
    (_: Option[V], v: V) => ev.evaluate(v)

  /** Yields each node paired with the node it was reached from. */
  def parentOf[V]: EvaluableFrom[V, Option[V]] =
    (from: Option[V], _: V) => Some(from)

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

// ============================================================
// Frontier typeclasses: Queueable and Stackable
// ============================================================

/**
  * Typeclass: abstracts over frontier data structures (Queue, Stack, PriorityQueue).
  * The three implementations below give us BFS, DFS, and best-first / Dijkstra-style
  * traversals for free — the traversal algorithm itself doesn't change at all.
  *
  * @tparam F the higher-kinded frontier container type
  */
trait Frontier[F[_]]:
  /**
    * Returns an empty instance of the parameterized type wrapped in the context of the effect type `F`.
    *
    * @return An empty instance of `F[T]`.
    */
  def empty[T]: F[T]

  /**
    * Applies a given value to a the frontier `f`.
    *
    * @param f The frontier of type `F[T]` that will receive the value.
    * @param t The value of type `T` to be applied to the frontier `f`.
    * @return A new frontier `F` that incorporates the provided value `t`.
    */
  def offer[T](f: F[T])(t: T): F[T]

  /**
    * Extracts an element of type `T` from the frontier `F[T]` and returns a tuple,
    * where the first element is the extracted value of type `T` and the second element
    * is the remaining frontier of type `F[T]` after the extraction.
    *
    * @param f the input frontier of type `F[T]` from which an element is extracted
    * @return a tuple containing the extracted element of type `T` and the remaining frontier of type `F[T]`
    */
  def take[T](f: F[T]): (T, F[T])

  /**
    * Checks if the given frontier is empty.
    *
    * @param f the frontier to be checked for emptiness
    * @return true if the frontier is empty, false otherwise
    */
  def isEmpty[T](f: F[T]): Boolean

  /**
    * Determines whether elements should be reversed upon being added to the frontier.
    * This behavior is typically useful in certain traversal algorithms where the order 
    * of elements affects the outcome, such as depth-first or breadth-first search.
    *
    * @return true if elements are reversed when offered to the frontier, false otherwise
    */
  def reverseOnOffer: Boolean = false

  /**
    * Adds all elements from the given list to the provided frontier. The order in which the 
    * elements are added depends on the value of `reverseOnOffer`. If `reverseOnOffer` is `true`, 
    * the elements are added in reverse order; otherwise, they are added as-is.
    *
    * @param f  The initial frontier of type `F[T]` to which the elements will be added.
    * @param ts A list of elements of type `T` to add to the frontier.
    * @return A new frontier of type `F[T]` that includes all elements from the list `ts`, 
    *         added in the specified order.
    */
  def offerAll[T](f: F[T])(ts: List[T]): F[T] =
    val ordered = if reverseOnOffer then ts.reverse else ts
    ordered.foldLeft(f)((acc, t) => offer(acc)(t))

/**
  * A type alias representing a stack, implemented as a `List`.
  *
  * @tparam T the type of elements stored in the stack
  */
type Stack[T] = List[T]

/**
  * Implementation of the `Frontier` typeclass for a stack-based data structure.
  * This implementation provides depth-first traversal behavior by leveraging 
  * the characteristics of a stack (LIFO - Last In, First Out).
  */
given Frontier[Stack] with
  def empty[T]: Stack[T] = Nil

  def offer[T](f: Stack[T])(t: T): Stack[T] = t :: f

  def take[T](f: Stack[T]): (T, Stack[T]) = (f.head, f.tail)

  def isEmpty[T](f: Stack[T]): Boolean = f.isEmpty

  override def reverseOnOffer: Boolean = true

/**
  * Implementation of the `Frontier` typeclass for the `Queue` data structure.
  * This implementation provides a breadth-first search (BFS) traversal strategy
  * due to the FIFO (First-In-First-Out) nature of `Queue`.
  *
  * Functions:
  * - `empty` — Constructs an empty `Queue`.
  * - `offer` — Enqueues an element into the `Queue`.
  * - `take` — Dequeues an element from the `Queue`, returning the element and the resulting `Queue`.
  * - `isEmpty` — Checks if the `Queue` is empty.
  */
given Frontier[Queue] with
  def empty[T]: Queue[T] = Queue.empty

  def offer[T](f: Queue[T])(t: T): Queue[T] = f.enqueue(t)

  def take[T](f: Queue[T]): (T, Queue[T]) = f.dequeue

  def isEmpty[T](f: Queue[T]): Boolean = f.isEmpty

/**
  * Priority-queue frontier: best-first traversal using [[PrioQueue]].
  *
  * Duplicates are permitted. Used by `bestFirst` and `bestFirstMax`.
  * `Ordering[T]` is captured at `PrioQueue` construction time.
  *
  * Supply `given PrioQueue[T] = PrioQueue.empty[T]` (or `emptyMax`) at the call site.
  */
given Frontier[PrioQueue] with
  def empty[T]: PrioQueue[T] =
    throw new UnsupportedOperationException(
      "Supply an explicit `given PrioQueue[T] = PrioQueue.empty[T]` at the call site."
    )
  def offer[T](f: PrioQueue[T])(t: T): PrioQueue[T] = f.offer(t)

  def take[T](f: PrioQueue[T]): (T, PrioQueue[T]) = f.take

  def isEmpty[T](f: PrioQueue[T]): Boolean = f.isEmpty

/**
  * Indexed priority-queue frontier: best-first traversal using [[IndexedPrioQueue]].
  *
  * Duplicates are not permitted — `offer` is a no-op if the element is already
  * present. Supports `decreaseKey` via [[CostUpdate]]. Used by `bestFirstWeighted`
  * (Dijkstra, Prim).
  *
  * Supply `given IndexedPrioQueue[T] = IndexedPrioQueue.empty[T]` at the call site.
  */
given Frontier[IndexedPrioQueue] with
  def empty[T]: IndexedPrioQueue[T] =
    throw new UnsupportedOperationException(
      "Supply an explicit `given IndexedPrioQueue[T] = IndexedPrioQueue.empty[T]` at the call site."
    )
  def offer[T](f: IndexedPrioQueue[T])(t: T): IndexedPrioQueue[T] = f.offer(t)

  def take[T](f: IndexedPrioQueue[T]): (T, IndexedPrioQueue[T]) = f.take

  def isEmpty[T](f: IndexedPrioQueue[T]): Boolean = f.isEmpty

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