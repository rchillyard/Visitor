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
  * The index is repaired incrementally: a sift touches only one root-to-leaf path, so each
  * operation reports the O(log n) positions it disturbed and only those entries are rewritten.
  * Until 1.9.1 it was rebuilt from the whole array after every operation, and `decreaseKey`
  * rebuilt the heap itself as well — see the note on `decreaseKey` for what that cost.
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
    * to improve its priority. O(log n).
    */
  def offer(t: T): IndexedPrioQueue[T] =
    if index.contains(t) then this
    else
      val (h, moved) = heap.insertTracked(t)
      IndexedPrioQueue(h, IndexedPrioQueue.reindex(index, h, moved))

  /**
    * Remove and return the head element. O(log n).
    */
  def take: (T, IndexedPrioQueue[T]) =
    val (min, h, moved) = heap.removeMinTracked
    (min, IndexedPrioQueue(h, IndexedPrioQueue.reindex(index - min, h, moved)))

  /**
    * Replace `oldT` with `newT` if `newT` has strictly lower priority.
    * If `oldT` is not present, this is a no-op. O(log n).
    *
    * `newT` is written over `oldT` in place and sifted up. Lowering a key can only move an
    * element towards the root, so sifting up alone restores the invariant — which is why the
    * `lteq` guard above is a precondition rather than merely an optimisation: without it this
    * would need a sift down too, and `replaceTracked` does not do one.
    *
    * ==This was O(n log n) until 1.9.1==
    *
    * It removed `oldT` by patching the array, rebuilt the whole heap by folding `insert` over
    * every remaining element, inserted `newT`, then rebuilt the index from the finished array.
    * The class scaladoc claimed O(log n) throughout; the implementation was two orders worse,
    * and `offer` and `take` each paid an O(n) index rebuild as well.
    *
    * It had been judged acceptable on the grounds that the rebuild is dominated by the O(E log V)
    * traversal — true at the sizes these libraries were tested on, where the largest graph had 80
    * vertices. Measured at 1000: thirty thousand `decreaseKey` calls took 1.1 s with 250 elements
    * queued and 4.4 s with 1000, so four times the queue cost very nearly four times the time.
    * That is linear in the queue size where it should be logarithmic, and it accounted for
    * essentially the whole of Gryphon's 3.5 s Dijkstra on a 30,000-edge graph. See #32, and
    * rchillyard/Gryphon#164 for the measurements that prompted it.
    */
  def decreaseKey(oldT: T, newT: T): IndexedPrioQueue[T] =
    index.get(oldT) match
      case None =>
        this
      case Some(i) =>
        if heap.ord.lteq(heap.data(i), newT) then this
        else
          val (h, moved) = heap.replaceTracked(i, newT)
          IndexedPrioQueue(h, IndexedPrioQueue.reindex(index - oldT, h, moved))

  /**
    * Returns true if `t` is currently in the queue. O(1).
    */
  def contains(t: T): Boolean = index.contains(t)

object IndexedPrioQueue:

  /**
    * Repair the index for exactly the positions a sift disturbed. O(k) in the number of moves,
    * which is O(log n) for every operation here.
    *
    * Positions beyond the end of the heap are skipped: `removeMinTracked` reports position 0 even
    * when the heap has just shrunk to nothing, and a caller must not be made to care.
    */
  private def reindex[T](index: Map[T, Int], h: BinaryHeap[T], moved: List[Int]): Map[T, Int] =
    moved.foldLeft(index)((m, j) => if j < h.data.size then m + (h.data(j) -> j) else m)

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
  def insert(t: T): BinaryHeap[T] = insertTracked(t)._1

  /**
    * As `insert`, but also reporting every position whose contents changed.
    *
    * A sift touches only the nodes on one root-to-leaf path, so that report is O(log n) long —
    * which is what lets [[IndexedPrioQueue]] repair its index incrementally instead of rebuilding
    * it from the whole array. See the note on `IndexedPrioQueue.decreaseKey`.
    *
    * @return the new heap, and the indices whose occupant differs from before.
    */
  def insertTracked(t: T): (BinaryHeap[T], List[Int]) =
    val appended = data :+ t
    // Seeded with the position the new element landed in: if it never sifts, that is still the
    // one position whose occupant changed, and omitting it leaves the element unindexed.
    val (sifted, moved) = siftUpTracked(appended, appended.size - 1, List(appended.size - 1))
    (copy(data = sifted), moved)

  /**
    * Replace the element at `i` and restore the heap invariant by sifting up, reporting the
    * positions that changed. O(log n).
    *
    * Sifting UP only, so the caller must ensure the new element does not sort after the old one:
    * that is `decreaseKey`'s precondition, checked there. Replacing with a larger element would
    * need a sift down instead, and would silently corrupt the heap here.
    *
    * @return the new heap, and the indices whose occupant differs from before.
    */
  def replaceTracked(i: Int, t: T): (BinaryHeap[T], List[Int]) =
    val (sifted, moved) = siftUpTracked(data.updated(i, t), i, List(i))
    (copy(data = sifted), moved)

  /**
    * Remove and return the minimum element, restoring the heap invariant by
    * sifting down. O(log n). Requires the heap to be non-empty.
    */
  def removeMin: (T, BinaryHeap[T]) =
    val (t, h, _) = removeMinTracked
    (t, h)

  /**
    * As `removeMin`, but also reporting every position whose contents changed. O(log n).
    *
    * @return the minimum, the new heap, and the indices whose occupant differs from before.
    */
  def removeMinTracked: (T, BinaryHeap[T], List[Int]) =
    require(data.nonEmpty, "removeMinTracked on empty heap")
    val min = data(0)
    // The size-1 branch is a fast path, not a correctness requirement, so mutating this
    // condition away is an equivalent mutant and `sbt stryker` will always report it as a
    // survivor. The general path yields the same answer: `data.last` is `data(0)`, so
    // `updated(0, data.last)` is the original vector, `.init` empties it, and
    // `siftDown(Vector.empty, 0)` returns at once (`left` and `right` both exceed `n = 0`).
    // What the branch buys is two skipped allocations on the last removal of a drain.
    if data.size == 1 then
      (min, copy(data = Vector.empty), Nil)
    else
      val promoted = data.updated(0, data.last).init
      val (sifted, moved) = siftDownTracked(promoted, 0, List(0))
      (min, copy(data = sifted), moved)

  // -----------------------------------------------------------------------
  // Private helpers
  // -----------------------------------------------------------------------

  // The two sifts accumulate the positions they touch, so a caller maintaining an index can
  // repair exactly those entries. `moved` is built by prepending, so it is in reverse order of
  // visiting -- which does not matter, since the caller re-reads each position from the finished
  // vector rather than replaying the moves.

  @tailrec
  private def siftUpTracked(v: Vector[T], i: Int, moved: List[Int]): (Vector[T], List[Int]) =
    // Like the size-1 branch in `removeMinTracked`, this guard is an optimisation rather than a
    // correctness requirement, so it survives mutation permanently. At `i == 0` the else branch
    // computes `parent = (0 - 1) / 2`, which is 0 -- integer division truncates toward zero --
    // and `ord.lteq(v(0), v(0))` then holds for any lawful Ordering, so it returns without
    // recursing. Keep the guard regardless: it saves a comparison, and it is the only thing
    // standing between a caller's non-reflexive Ordering and an infinite sift.
    if i == 0 then (v, moved)
    else
      val parent = (i - 1) / 2
      if ord.lteq(v(parent), v(i)) then (v, moved)
      // BOTH ends of the swap change occupant, so both are recorded. Recording only the
      // destination leaves the element displaced into `i` holding a stale index.
      else siftUpTracked(v.updated(i, v(parent)).updated(parent, v(i)), parent, i :: parent :: moved)

  @tailrec
  private def siftDownTracked(v: Vector[T], i: Int, moved: List[Int]): (Vector[T], List[Int]) =
    val left = 2 * i + 1
    val right = 2 * i + 2
    val n = v.size
    val smallest =
      if left < n && ord.lt(v(left), v(i)) then left else i
    val smallest2 =
      if right < n && ord.lt(v(right), v(smallest)) then right else smallest
    if smallest2 == i then (v, moved)
    // As in siftUpTracked: both ends of the swap change occupant.
    else siftDownTracked(v.updated(i, v(smallest2)).updated(smallest2, v(i)), smallest2, i :: smallest2 :: moved)

private[core] object BinaryHeap:
  def empty[T: Ordering]: BinaryHeap[T] = BinaryHeap(Vector.empty)
