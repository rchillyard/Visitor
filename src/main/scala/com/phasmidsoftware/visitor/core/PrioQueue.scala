package com.phasmidsoftware.visitor.core

import scala.annotation.tailrec

// ============================================================
// BinaryHeap — pure data structure
// ============================================================

/**
  * An immutable binary min-heap.
  *
  * This is a pure data structure: it knows only about the heap array and the
  * two structural operations that maintain the heap invariant (sift-up and
  * sift-down). It has no knowledge of indexing, deduplication, or priority
  * queue policy — those concerns belong to [[PrioQueue]] and [[IndexedPrioQueue]].
  *
  * The heap is 0-indexed: root at index 0, children of node i at 2i+1 and 2i+2.
  * All operations are O(log n) except [[isEmpty]] which is O(1).
  *
  * @param data the heap array
  * @param ord  the ordering used to maintain the heap invariant
  * @tparam T the element type
  */
private[core] case class BinaryHeap[T](data: Vector[T])(using val ord: Ordering[T]):

  def isEmpty: Boolean = data.isEmpty

  def size: Int = data.size

  def head: T =
    require(data.nonEmpty, "head on empty heap")
    data(0)

  /**
    * Insert a new element, restoring the heap invariant by sifting up. O(log n).
    * Duplicate elements are permitted.
    */
  def insert(t: T): BinaryHeap[T] =
    val appended = data :+ t
    copy(data = siftUp(appended, appended.size - 1))

  /**
    * Remove and return the minimum element, restoring the heap invariant by
    * sifting down. O(log n). Requires the heap to be non-empty.
    */
  def removeMin: (T, BinaryHeap[T]) =
    require(data.nonEmpty, "removeMin on empty heap")
    val min = data(0)
    if data.size == 1 then
      (min, copy(data = Vector.empty))
    else
      val promoted = data.updated(0, data.last).init
      (min, copy(data = siftDown(promoted, 0)))

  // -----------------------------------------------------------------------
  // Private helpers
  // -----------------------------------------------------------------------

  @tailrec
  private def siftUp(v: Vector[T], i: Int): Vector[T] =
    if i == 0 then v
    else
      val parent = (i - 1) / 2
      if ord.lteq(v(parent), v(i)) then v
      else siftUp(v.updated(i, v(parent)).updated(parent, v(i)), parent)

  @tailrec
  private def siftDown(v: Vector[T], i: Int): Vector[T] =
    val left = 2 * i + 1
    val right = 2 * i + 2
    val n = v.size
    val smallest =
      if left < n && ord.lt(v(left), v(i)) then left else i
    val smallest2 =
      if right < n && ord.lt(v(right), v(smallest)) then right else smallest
    if smallest2 == i then v
    else siftDown(v.updated(i, v(smallest2)).updated(smallest2, v(i)), smallest2)

private[core] object BinaryHeap:
  def empty[T: Ordering]: BinaryHeap[T] = BinaryHeap(Vector.empty)

// ============================================================
// PrioQueue — priority queue ADT, delegates to BinaryHeap
// ============================================================

/**
  * An immutable priority queue ADT backed by [[BinaryHeap]].
  *
  * Duplicates are permitted. The minimum element is dequeued first
  * (or maximum via [[PrioQueue.emptyMax]]).
  *
  * [[Ordering]][T] is captured at construction time so that
  * [[Frontier]][PrioQueue] remains fully polymorphic — no `Ordering`
  * context is needed at `offer` / `take` call sites.
  *
  * For a priority queue that also supports O(log n) `decreaseKey`,
  * see [[IndexedPrioQueue]].
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

  def size: Int = heap.size

  def head: T = heap.head

object PrioQueue:
  /** Min-priority queue — smallest element dequeued first. */
  def empty[T: Ordering]: PrioQueue[T] =
    PrioQueue(BinaryHeap.empty[T])

  /** Max-priority queue — largest element dequeued first. */
  def emptyMax[T: Ordering]: PrioQueue[T] =
    PrioQueue(BinaryHeap.empty[T](using Ordering[T].reverse))

// ============================================================
// IndexedPrioQueue — PrioQueue ADT extended with decreaseKey
// ============================================================

/**
  * An immutable indexed priority queue ADT backed by [[BinaryHeap]].
  *
  * Extends the [[PrioQueue]] contract with O(log n) `decreaseKey` and O(1)
  * `contains`, enabled by a `Map[T, Int]` index that tracks each element's
  * current position in the heap array.
  *
  * Unlike [[PrioQueue]], duplicates are not permitted: `offer` is a no-op if
  * the element is already present. Use `decreaseKey` to improve an existing
  * entry's priority. This one-entry-per-key invariant is what makes the index
  * meaningful and `decreaseKey` correct.
  *
  * The index is rebuilt from `heap.data` (O(n)) after each structural operation
  * (`offer` and `take`). This is acceptable because:
  *   - the O(n) rebuild cost is dominated by the O(E log V) traversal cost
  *   - the implementation remains simple, correct, and easy to reason about
  *
  * Minimum element is dequeued first (or maximum via [[IndexedPrioQueue.emptyMax]]).
  *
  * @param heap  the underlying binary heap
  * @param index a map from element to its current 0-based position in heap.data
  * @tparam T the element type
  */
case class IndexedPrioQueue[T] private(
                                        private[core] val heap: BinaryHeap[T],
                                        private val index: Map[T, Int]
                                      ):

  def isEmpty: Boolean = heap.isEmpty

  def size: Int = heap.size

  def head: T = heap.head

  /**
    * Add an element. No-op if the element is already present — use `decreaseKey`
    * to improve its priority. O(n) due to index rebuild.
    */
  def offer(t: T): IndexedPrioQueue[T] =
    if index.contains(t) then this
    else IndexedPrioQueue.fromHeap(heap.insert(t))

  /**
    * Remove and return the minimum element. O(n) due to index rebuild.
    */
  def take: (T, IndexedPrioQueue[T]) =
    val (min, h) = heap.removeMin
    (min, IndexedPrioQueue.fromHeap(h))

  /**
    * Replace `oldT` with `newT` if `newT` has strictly lower priority.
    * If `oldT` is not present, this is a no-op.
    *
    * Implemented by removing `oldT` from the heap array, inserting `newT`,
    * and rebuilding the heap from scratch. O(n log n) in the worst case, but
    * correct and simple. The index is rebuilt after the operation. O(n).
    */
  def decreaseKey(oldT: T, newT: T): IndexedPrioQueue[T] =
    index.get(oldT) match
      case None => this
      case Some(i) =>
        if heap.ord.lteq(heap.data(i), newT) then this
        else
          val withoutOld = heap.data.patch(i, Nil, 1)
          val rebuilt = withoutOld.foldLeft(BinaryHeap.empty[T](using heap.ord))(
            (h, t) => h.insert(t)
          ).insert(newT)
          IndexedPrioQueue.fromHeap(rebuilt)

  /**
    * Returns true if `t` is currently in the queue. O(1).
    */
  def contains(t: T): Boolean = index.contains(t)

object IndexedPrioQueue:

  /** Rebuild the index from the heap's data array. O(n). */
  private def fromHeap[T](h: BinaryHeap[T]): IndexedPrioQueue[T] =
    IndexedPrioQueue(h, h.data.zipWithIndex.toMap)

  /** Min-priority indexed queue — smallest element dequeued first. */
  def empty[T: Ordering]: IndexedPrioQueue[T] =
    IndexedPrioQueue(BinaryHeap.empty[T], Map.empty)

  /** Max-priority indexed queue — largest element dequeued first. */
  def emptyMax[T: Ordering]: IndexedPrioQueue[T] =
    IndexedPrioQueue(BinaryHeap.empty[T](using Ordering[T].reverse), Map.empty)