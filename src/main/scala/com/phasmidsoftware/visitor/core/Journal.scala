package com.phasmidsoftware.visitor.core

import scala.collection.immutable.Queue

// ============================================================
// Journal / Appendable (unchanged from old package but repeated
// here for a self-contained skeleton)
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
