package com.phasmidsoftware.visitor.core

import scala.annotation.tailrec

/**
  * An immutable binary min-heap, used as the backing structure for [[PrioQueue]].
  *
  * Implemented as a standard array-based heap over a Vector for structural sharing.
  * All operations are O(log n) except [[isEmpty]] which is O(1).
  *
  * @param data the heap array (1-indexed: root at index 1, children of i at 2i and 2i+1)
  * @param ord  the ordering used to maintain the heap invariant
  * @tparam T the element type
  */
private case class BinaryHeap[T](data: Vector[T])(using ord: Ordering[T]):

  def isEmpty: Boolean = data.isEmpty

  /** Insert an element, restoring the heap invariant by sifting up. O(log n). */
  def insert(t: T): BinaryHeap[T] =
    val appended = data :+ t
    copy(data = siftUp(appended, appended.size - 1))

  /** Remove and return the minimum element, restoring the heap invariant by sifting down. O(log n). */
  def removeMin: (T, BinaryHeap[T]) =
    require(data.nonEmpty, "removeMin on empty heap")
    val min = data.head
    val rest = if data.size == 1 then Vector.empty[T]
    else siftDown(data.last +: data.tail.init, 0)
    (min, copy(data = rest))

  def head: T =
    require(data.nonEmpty, "head on empty heap")
    data.head

  // Sift the element at index i upward until the heap invariant is restored.
  @tailrec
  private def siftUp(v: Vector[T], i: Int): Vector[T] =
    if i == 0 then v
    else
      val parent = (i - 1) / 2
      if ord.lteq(v(parent), v(i)) then v
      else siftUp(v.updated(i, v(parent)).updated(parent, v(i)), parent)

  // Sift the element at index i downward until the heap invariant is restored.
  @annotation.tailrec
  private def siftDown(v: Vector[T], i: Int): Vector[T] =
    val left = 2 * i + 1
    val right = 2 * i + 2
    val smallest =
      if left < v.size && ord.lt(v(left), v(i)) then left
      else i
    val smallest2 =
      if right < v.size && ord.lt(v(right), v(smallest)) then right
      else smallest
    if smallest2 == i then v
    else siftDown(v.updated(i, v(smallest2)).updated(smallest2, v(i)), smallest2)

/**
  * Factory methods for creating instances of the BinaryHeap.
  *
  * A BinaryHeap is a priority queue data structure that maintains the heap
  * property, enabling efficient insertion and removal of elements based 
  * on their priority.
  */
object BinaryHeap:
  def empty[T: Ordering]: BinaryHeap[T] = BinaryHeap(Vector.empty)

// ============================================================
// PrioQueue — public wrapper around BinaryHeap
// ============================================================

/**
  * An immutable priority queue backed by a [[BinaryHeap]].
  *
  * Captures [[Ordering]] at construction time so that [[Frontier]][PrioQueue]
  * can remain fully polymorphic without needing an `Ordering` context at
  * every `offer` / `take` call.
  *
  * Minimum element is dequeued first.
  *
  * @param heap the underlying binary heap
  * @tparam T the element type
  */
case class PrioQueue[T] private(private val heap: BinaryHeap[T]):
  def offer(t: T): PrioQueue[T] = copy(heap = heap.insert(t))

  def take: (T, PrioQueue[T]) =
    val (min, h) = heap.removeMin
    (min, copy(heap = h))

  def isEmpty: Boolean = heap.isEmpty

  def head: T = heap.head

/**
  * Provides factory methods for creating instances of `PrioQueue` with different priority orderings.
  */
object PrioQueue:
  /** Min-priority queue — smallest element dequeued first. */
  def empty[T: Ordering]: PrioQueue[T] = PrioQueue(BinaryHeap.empty[T])

  /** Max-priority queue — largest element dequeued first. */
  def emptyMax[T: Ordering]: PrioQueue[T] = PrioQueue(BinaryHeap.empty[T](using Ordering[T].reverse))
