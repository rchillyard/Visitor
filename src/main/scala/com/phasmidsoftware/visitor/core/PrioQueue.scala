package com.phasmidsoftware.visitor.core

import scala.annotation.tailrec

// ============================================================
// AbstractPrioQueue — the contract the two queues share
// ============================================================

/**
  * An immutable priority queue abstraction whose elements are retrieved according
  * to the priority ordering defined by a concrete implementation.
  *
  * Adding or removing elements does not mutate the existing queue. Instead,
  * operations return a new queue that reflects the requested change.
  *
  * Implementations determine how priorities are assigned, how elements with equal
  * priority are ordered, and '''whether duplicates are permitted''': [[PrioQueue]]
  * admits them, while [[IndexedPrioQueue]] holds at most one entry per element, which
  * is what makes its index meaningful and its `decreaseKey` correct.
  *
  * `head` and `take` are partial: both require a non-empty queue, and both raise
  * `IllegalArgumentException` otherwise. Guard with [[isEmpty]], as [[Traversal.traverse]]
  * does through [[Frontier]].
  *
  * @tparam T the type of elements stored in the priority queue
  */
trait AbstractPrioQueue[T]:

  /**
    * Adds an element to this priority queue, preserving the queue ordering.
    *
    * Whether a duplicate is stored, ignored or merged is the implementation's business;
    * see the note on duplicates above.
    *
    * @param t the element to add
    * @return a new priority queue incorporating `t`
    */
  def offer(t: T): AbstractPrioQueue[T]

  /**
    * Removes the head element from this priority queue and returns it together
    * with the remaining queue. The original queue is unchanged.
    *
    * @return a tuple containing the removed element and a new priority queue without that element
    * @throws IllegalArgumentException if this queue is empty
    */
  def take: (T, AbstractPrioQueue[T])

  /**
    * Determines whether this priority queue contains no elements.
    *
    * @return `true` if this priority queue has no elements, otherwise `false`
    */
  def isEmpty: Boolean

  /**
    * Returns the number of elements currently contained in this priority queue.
    *
    * @return the number of elements in this priority queue
    */
  def size: Int

  /**
    * Returns the head element of this priority queue without removing it.
    *
    * @return the element with the highest priority according to this queue's ordering
    * @throws IllegalArgumentException if this queue is empty
    */
  def head: T

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
case class PrioQueue[T] private(private val heap: BinaryHeap[T]) extends AbstractPrioQueue[T]:

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
                                      ) extends AbstractPrioQueue[T]:

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
    * Remove and return the head element. O(n) due to index rebuild.
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
      case None =>
        this
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

// ============================================================
// BinaryHeap — the pure data structure both queues delegate to
//
// Last in the file because it is the implementation detail, not the interface:
// a reader arriving at PrioQueue.scala wants the queues first.
// ============================================================

/**
  * An immutable binary min-heap.
  *
  * This is a pure data structure: it knows only about the heap array and the
  * two structural operations that maintain the heap invariant (sift-up and
  * sift-down). It has no knowledge of indexing, deduplication, or priority
  * queue policy — those concerns belong to [[PrioQueue]] and [[IndexedPrioQueue]].
  *
  * It is deliberately NOT an [[AbstractPrioQueue]]. Its `insert` and `removeMin` are
  * the structural operations the queues are built from, not the queue operations
  * themselves; conflating them would make the thing being delegated to look like a
  * sibling of the things delegating.
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
    // The size-1 branch is a fast path, not a correctness requirement, so mutating this
    // condition away is an equivalent mutant and `sbt stryker` will always report it as a
    // survivor. The general path yields the same answer: `data.last` is `data(0)`, so
    // `updated(0, data.last)` is the original vector, `.init` empties it, and
    // `siftDown(Vector.empty, 0)` returns at once (`left` and `right` both exceed `n = 0`).
    // What the branch buys is two skipped allocations on the last removal of a drain.
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
    // Like the size-1 branch in `removeMin`, this guard is an optimisation rather than a
    // correctness requirement, so it too survives mutation permanently. At `i == 0` the else
    // branch computes `parent = (0 - 1) / 2`, which is 0 — integer division truncates toward
    // zero — and `ord.lteq(v(0), v(0))` then holds for any lawful Ordering, so it returns `v`
    // without recursing. Keep the guard regardless: it saves a comparison, and it is the only
    // thing standing between a caller's non-reflexive Ordering and an infinite sift.
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
