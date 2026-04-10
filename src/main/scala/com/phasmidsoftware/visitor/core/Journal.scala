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
trait Appendable[X] extends AutoCloseable:
  def append(x: X): Appendable[X]

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
trait Journal[X] extends Appendable[X]:
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
trait IterableJournal[X] extends Journal[X] with Iterable[X]

/**
  * A case class representing a journal backed by a list, which maintains a sequence of elements.
  *
  * This class provides methods to append elements to the journal and to iterate over its contents.
  * It is immutable, meaning every modification creates a new instance.
  *
  * @param xs the list containing the elements of the journal
  * @tparam X the type of elements stored in the journal
  */
case class ListJournal[X](xs: List[X]) extends IterableJournal[X]:
  def append(x: X): ListJournal[X] = copy(x :: xs)

  def iterator: Iterator[X] = xs.iterator

  def close(): Unit = ()

/**
  * Companion object for the ListJournal class, providing utility methods for creating instances of ListJournal.
  *
  * The ListJournal object serves as a factory for creating empty instances of ListJournal.
  * ListJournal is used to maintain a sequence of elements in prepend (LIFO) order.
  *
  * @tparam X the type of elements to be stored in the ListJournal
  */
object ListJournal:
  def empty[X]: ListJournal[X] = ListJournal(Nil)

/**
  * An implementation of [[IterableJournal]] that uses a [[Queue]] to manage journal entries.
  *
  * This class provides a mechanism for appending elements to a queue-based journal and iterating
  * over the elements in a sequential order. It is particularly useful in scenarios where a
  * first-in-first-out (FIFO) ordering of operations or entries is required.
  *
  * @param q the underlying queue used to store the journal's elements
  * @tparam X the type of elements stored in the journal
  */
case class QueueJournal[X](q: Queue[X]) extends IterableJournal[X]:
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
  * For each discovered vertex `v`, `map(v)` is the vertex from which `v` was first
  * discovered — i.e. the vertex that was being visited when `v` was added to the
  * frontier. The start vertex is absent from the map (it has no predecessor).
  *
  * This is sometimes called a "parent map" but "came-from" is more accurate: the
  * relationship is an artifact of traversal order, not a structural property of the
  * graph.
  *
  * Used in conjunction with [[JournaledVisitor]] when came-from tracking is requested
  * via the `withQueueJournalAndCameFrom` or `withListJournalAndCameFrom` factory methods.
  *
  * @param map the underlying came-from map: vertex → the vertex that discovered it
  * @tparam V the vertex type
  */
case class CameFromJournal[V](map: Map[V, V]) extends IterableJournal[(V, V)]:

  def append(x: (V, V)): CameFromJournal[V] =
    if map.contains(x._1) then this // already discovered — keep first
    else copy(map + x)

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

object CameFromJournal:
  def empty[V]: CameFromJournal[V] = CameFromJournal(Map.empty)