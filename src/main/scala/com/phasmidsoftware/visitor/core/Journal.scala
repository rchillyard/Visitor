package com.phasmidsoftware.visitor.core

import scala.collection.immutable.Queue

// ============================================================
// Journal / Appendable
// ============================================================

/**
  * A trait representing an appendable structure that supports the addition
  * of elements of type X and exhibits a fluent interface by returning itself
  * after each append operation.
  *
  * Implementing classes may define specific behavior for how elements are appended
  * and may provide additional lifecycle management by extending AutoCloseable.
  *
  * @tparam X the type of elements that can be appended to the structure
  */
trait Appendable[X, A <: Appendable[X, A]] extends AutoCloseable:
  /**
    * Appends the specified element to this appendable structure and returns
    * the instance itself, allowing for a fluent interface.
    *
    * @param x the element of type X to be appended
    * @return the instance of the appendable structure with the new element appended
    */
  def append(x: X): A

/**
  * A trait representing a journal for managing a collection of appendable elements of type X.
  *
  * The Journal extends the behavior of [[Appendable]], supporting the addition of elements
  * while also introducing lifecycle management through the ability to close the journal.
  *
  * Implementing classes can define additional behaviors, such as persistence, iteration, or
  * other domain-specific functionalities.
  *
  * @tparam X the type of elements that can be managed by the journal
  */
trait Journal[X, A <: Journal[X, A]] extends Appendable[X, A]:
  def close(): Unit

/**
  * A trait that combines the functionalities of a [[Journal]] and [[Iterable]], enabling append operations,
  * lifecycle management, and iterable access to elements.
  *
  * This abstraction is useful for scenarios where elements need to be managed sequentially,
  * support append operations, and be accessed through iteration.
  *
  * Implementing classes are expected to define the behavior for appending elements, providing iterators,
  * and any optional lifecycle management such as resource closure.
  *
  * @tparam X the type of elements contained in the journal
  */
trait IterableJournal[X, A <: IterableJournal[X, A]] extends Journal[X, A] with Iterable[X]

/**
  * A case class representing a journal backed by a list, which maintains a sequence of elements.
  *
  * This class provides methods to append elements to the journal and to iterate over its contents.
  * It is immutable, meaning every modification creates a new instance.
  *
  * @param xs the list containing the elements of the journal
  * @tparam X the type of elements stored in the journal
  */
case class ListJournal[X](xs: List[X]) extends IterableJournal[X, ListJournal[X]]:
  def append(x: X): ListJournal[X] = copy(x :: xs)

  def iterator: Iterator[X] = xs.iterator

  def close(): Unit = ()

/**
  * Companion object for the `ListJournal` class, providing utility methods for creating instances of ListJournal.
  *
  * The `ListJournal` object serves as a factory for creating empty instances of `ListJournal`.
  * `ListJournal` is used to maintain a sequence of elements in prepend (LIFO) order.
  */
object ListJournal:
  /**
    * Creates an empty instance of `ListJournal` containing no elements.
    *
    * @tparam X the type of elements to be stored in the `ListJournal`
    * @return a `ListJournal` instance with an empty list backing the journal
    */
  def empty[X]: ListJournal[X] = ListJournal(Nil)

/**
  * An implementation of [[IterableJournal]] that uses a `Queue` to manage journal entries.
  *
  * This class provides a mechanism for appending elements to a queue-based journal and iterating
  * over the elements in a sequential order. It is particularly useful in scenarios where a
  * first-in-first-out (FIFO) ordering of operations or entries is required.
  *
  * @param q the underlying queue used to store the journal's elements
  * @tparam X the type of elements stored in the journal
  */
case class QueueJournal[X](q: Queue[X]) extends IterableJournal[X, QueueJournal[X]]:
  def append(x: X): QueueJournal[X] = copy(q.enqueue(x))

  def iterator: Iterator[X] = q.iterator

  def close(): Unit = ()

/**
  * Companion object for the `QueueJournal` class.
  *
  * Provides utility methods such as creating an empty instance of `QueueJournal`.
  *
  * @tparam X the type of elements that the `QueueJournal` will store
  */
object QueueJournal:
  def empty[X]: QueueJournal[X] = QueueJournal(Queue.empty)

/**
  * A journal that records the came-from relationship established during graph traversal.
  *
  * For each reached vertex `v`, `map(v)` is the vertex it was reached from. The start vertex is
  * absent from the map, having no predecessor.
  *
  * This is sometimes called a "parent map" but "came-from" is more accurate: the
  * relationship is an artifact of traversal order, not a structural property of the
  * graph.
  *
  * ==Reached from where, exactly: `keepFirst`==
  *
  * A vertex can be reached from several places before it is visited, so "the vertex it was
  * reached from" has to pick one, and '''the two traversals need opposite answers'''.
  *
  *  - '''Breadth-first wants the FIRST''' (`keepFirst = true`). Discovery order is the tree: the
  *    vertex that first reached `v` is the one on a shortest path to it, so following the map
  *    backwards gives a path with the fewest edges.
  *  - '''Depth-first wants the LAST''' (`keepFirst = false`). The DFS tree is defined by
  *    ''exploration'', not discovery. A vertex may be seen early from the root and then seen
  *    again, deeper, before its turn comes — and it is expanded from the most recent of those,
  *    because that is the frame nearest the top of the stack. So the last writer before `v` is
  *    visited is exactly the vertex `v` is explored from.
  *
  * Through 1.9.x this was always first-wins, and so was right for BFS and wrong for DFS. The
  * symptom: on a transitive tournament — vertices 0..5, an edge `i -> j` for every `i < j` — DFS
  * walks the path `0,1,2,3,4,5` and then reported `{1->0, 2->0, 3->0, 4->0, 5->0}`, a star. The
  * traversal had the right answer and the journal discarded it, because every vertex really was
  * first seen from the source. With `keepFirst = false` the same traversal reports
  * `{1->0, 2->1, 3->2, 4->3, 5->4}`, which is the path it actually walked. See #55, and
  * rchillyard/Gryphon#144 where it surfaced.
  *
  * Used in conjunction with [[JournaledVisitor]] when came-from tracking is requested via
  * `withQueueJournalAndCameFrom` (BFS, first) or `withListJournalAndCameFrom` (DFS, last).
  *
  * @param map       the underlying came-from map: vertex → the vertex it was reached from
  * @param keepFirst whether the first writer wins (discovery, for BFS) or the last
  *                  (exploration, for DFS)
  * @tparam V the vertex type
  */
case class CameFromJournal[V](map: Map[V, V], keepFirst: Boolean = true)
        extends IterableJournal[(V, V), CameFromJournal[V]]:

  /**
    * Record that `x._1` was reached from `x._2`.
    *
    * A node can be reached from several places before it is visited, so this has to choose, and
    * `keepFirst` is that choice. See the class scaladoc: the two traversals want opposite answers,
    * and the engine offers each the one it needs.
    */
  def append(x: (V, V)): CameFromJournal[V] =
    if keepFirst && map.contains(x._1) then this // already discovered — keep the first
    else copy(map = map + x)

  def iterator: Iterator[(V, V)] = map.iterator

  def close(): Unit = ()

  /**
    * Returns the vertex that discovered `v`, if any.
    *
    * @param v the query vertex.
    * @return `Some(cameFrom)` if `v` was discovered during traversal, `None` for
    *         the start vertex or vertices not reached.
    */
  def cameFrom(v: V): Option[V] = map.get(v)

  /**
    * Returns the came-from map as a plain `Map[V, V]`.
    */
  def asMap: Map[V, V] = map

/**
  * Companion object for the `CameFromJournal` class.
  *
  * Provides utility methods to work with the `CameFromJournal`, including
  * initialization of an empty came-from journal.
  */
object CameFromJournal:
  /**
    * An empty journal recording the '''discovery''' parent: the first vertex to reach each one.
    *
    * This is what breadth-first search wants, and what `empty` has always given. Following it
    * backwards yields a path with the fewest edges.
    *
    * @return an empty `CameFromJournal` with `keepFirst = true`.
    */
  def discovery[V]: CameFromJournal[V] = CameFromJournal(Map.empty, keepFirst = true)

  /**
    * An empty journal recording the '''exploration''' parent: the last vertex to reach each one
    * before it is visited.
    *
    * This is what depth-first search wants, and what it did not get before 1.10.0. See the class
    * scaladoc for why the last writer is the vertex a DFS actually expands from.
    *
    * @return an empty `CameFromJournal` with `keepFirst = false`.
    */
  def exploration[V]: CameFromJournal[V] = CameFromJournal(Map.empty, keepFirst = false)

  /**
    * Creates an empty `CameFromJournal` instance, recording the discovery parent.
    *
    * Kept as the unqualified name, and as the discovery variant, because that is what it has
    * always meant. Prefer `discovery` or `exploration`, which say which one you are asking for
    * rather than leaving it to be inferred.
    *
    * @return an empty `CameFromJournal`, with an underlying empty map of type `Map[V, V]`.
    */
  def empty[V]: CameFromJournal[V] = discovery