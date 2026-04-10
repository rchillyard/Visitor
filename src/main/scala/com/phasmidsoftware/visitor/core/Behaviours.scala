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

given [A: Monoid]: Zero[A] with
  def identity: A = summon[Monoid[A]].identity

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
  def empty[T]: F[T]
  def offer[T](f: F[T])(t: T): F[T]
  def take[T](f: F[T]): (T, F[T])
  def isEmpty[T](f: F[T]): Boolean

  /** If true, offerAll reverses the list before offering (needed for LIFO stacks). */
  def reverseOnOffer: Boolean = false

  def offerAll[T](f: F[T])(ts: List[T]): F[T] =
    val ordered = if reverseOnOffer then ts.reverse else ts
    ordered.foldLeft(f)((acc, t) => offer(acc)(t))

/** DFS frontier: LIFO Stack (represented as a List). */
type Stack[T] = List[T]

given Frontier[Stack] with
  def empty[T]: Stack[T] = Nil

  def offer[T](f: Stack[T])(t: T): Stack[T] = t :: f

  def take[T](f: Stack[T]): (T, Stack[T]) = (f.head, f.tail)

  def isEmpty[T](f: Stack[T]): Boolean = f.isEmpty

  override def reverseOnOffer: Boolean = true

/** BFS frontier: FIFO Queue. */
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
// Visitor
// ============================================================

/**
  * A Visitor accumulates (V, Option[R]) pairs into a journal J as it
  * traverses a structure. It knows nothing about the structure itself —
  * that knowledge lives in Neighbours. It knows nothing about what to
  * extract from a node — that lives in Evaluable.
  *
  * @tparam V the node type
  * @tparam R the result type extracted from each node
  * @tparam J the journal type (must be Appendable of (V, Option[R]))
  */
trait Visitor[V, R, J <: Appendable[(V, Option[R])]]:
  def journal: J
  def visit(v: V)(using ev: Evaluable[V, R]): Visitor[V, R, J]
  def result: J = journal

/** Canonical immutable implementation. */
case class JournaledVisitor[V, R, J <: Appendable[(V, Option[R])]](journal: J)
  extends Visitor[V, R, J]:
  def visit(v: V)(using ev: Evaluable[V, R]): JournaledVisitor[V, R, J] =
    copy(journal = journal.append(v -> ev.evaluate(v)).asInstanceOf[J])

object JournaledVisitor:
  def withListJournal[V, R]: JournaledVisitor[V, R, ListJournal[(V, Option[R])]] =
    JournaledVisitor(ListJournal.empty)

  def withQueueJournal[V, R]: JournaledVisitor[V, R, QueueJournal[(V, Option[R])]] =
    JournaledVisitor(QueueJournal.empty)