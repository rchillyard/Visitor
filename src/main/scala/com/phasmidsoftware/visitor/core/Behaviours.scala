package com.phasmidsoftware.visitor.core

import scala.collection.immutable.Queue

// ============================================================
// Zero typeclass
// ============================================================

/**
  * Typeclass: a type with an identity element.
  *
  * `Zero[A]` is the minimal requirement for weighted graph traversals that
  * need a seed cost but do not accumulate costs along a path — specifically
  * Prim's MST algorithm. Prim needs `identity` to seed the frontier but
  * never calls `combine`.
  *
  * `Monoid[A]` extends `Zero[A]` by adding `combine`, making `Zero` the
  * honest supertype for the two-context-bound pattern `E: {Zero, Ordering}`.
  *
  * Any `given Monoid[A]` automatically satisfies `Zero[A]` through inheritance.
  *
  * @tparam A the element type
  */
trait Zero[A]:
  /** The identity element. */
  def identity: A

given Zero[Int] with
  def identity: Int = 0

given Zero[Long] with
  def identity: Long = 0L

given Zero[Double] with
  def identity: Double = 0.0

given Zero[Float] with
  def identity: Float = 0.0f


// ============================================================
// Monoid typeclass
// ============================================================

/**
  * Typeclass: an associative binary operation with an identity element.
  *
  * Extends [[Zero]] with `combine`. Mirrors the Cats `Monoid` typeclass
  * but without the Cats dependency.
  *
  * Primary use in Visitor/Gryphon:
  *   - Dijkstra needs both `identity` (seed cost) and `combine` (path cost accumulation).
  *   - Prim needs only `identity` — use `Zero[E]` for Prim, `Monoid[E]` for Dijkstra.
  *
  * @tparam A the element type
  */
trait Monoid[A]:
  /** The identity element: `combine(identity, x) == x` for all x. */
  def identity: A

  /** An associative binary operation. */
  def combine(x: A, y: A): A

/** Additive monoid for [[Int]]. */
given Monoid[Int] with
  def identity: Int = 0

  def combine(x: Int, y: Int): Int = x + y

/** Additive monoid for [[Long]]. */
given Monoid[Long] with
  def identity: Long = 0L

  def combine(x: Long, y: Long): Long = x + y

/** Additive monoid for [[Double]]. */
given Monoid[Double] with
  def identity: Double = 0.0

  def combine(x: Double, y: Double): Double = x + y

/** Additive monoid for [[Float]]. */
given Monoid[Float] with
  def identity: Float = 0.0f

  def combine(x: Float, y: Float): Float = x + y

/** Any Monoid[A] automatically satisfies Zero[A]. */
given [A: Monoid]: Zero[A] with
  def identity: A = summon[Monoid[A]].identity

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
  * Typeclass: a set of visited nodes, used to prevent revisiting in cycle-containing graphs.
  * Immutable — markVisited returns a new instance.
  *
  * @tparam V the node type
  */
trait VisitedSet[V]:
  def isVisited(v: V): Boolean
  def markVisited(v: V): VisitedSet[V]

/** Default given: immutable Set-backed VisitedSet for plain node types. */
given [V]: VisitedSet[V] = SetVisitedSet(Set.empty)

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