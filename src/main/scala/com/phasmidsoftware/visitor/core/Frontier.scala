package com.phasmidsoftware.visitor.core

import scala.collection.immutable.Queue

/**
  * Typeclass: abstracts over frontier data structures (Queue, Stack, PriorityQueue).
  * The four implementations below give us BFS, DFS, and best-first / Dijkstra-style
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
