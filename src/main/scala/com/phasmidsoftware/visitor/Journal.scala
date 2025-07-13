package com.phasmidsoftware.visitor

import scala.collection.immutable.Queue

/**
 * A trait representing a journal that maintains a collection of elements of type `X`.
 *
 * `Journal` combines the behaviors of `Appendable` and `Iterable`, allowing elements to be appended
 * while also being iterable for operations like traversal, filtering, and mapping.
 *
 * This abstraction is intended to represent a sequence of log-like or journaled entries
 * that can be modified immutably and iterated over. It may be useful in scenarios where
 * maintaining an appendable history or audit log alongside iterable access is required.
 *
 * @tparam X the type of elements contained in the `Journal`
 */
trait Journal[X] extends Appendable[X] with Iterable[X] {

  /**
   * Closes the journal, performing any necessary cleanup or finalization operations.
   *
   * This method may be overridden by subclasses to implement custom close behavior.
   *
   * @return Unit (no specific value is returned)
   */
  def close(): Unit = {
  }
}


/**
 * Represents a journal implemented as a list of elements of type `X`.
 *
 * `ListJournal` is an immutable data structure that extends the `Journal` trait.
 * It provides functionality to append elements and to iterate over the stored elements.
 *
 * @tparam X the type of elements stored in the journal
 * @param xs the list of elements in the journal
 */
case class ListJournal[X](xs: List[X]) extends Journal[X] {
  /**
   * Appends the specified element to this `Appendable` object, returning a new instance
   * of the `Appendable` with the element included.
   *
   * @param x the element to be appended
   * @return a new `Appendable[X]` instance containing the existing elements and the newly appended element
   */
  def append(x: X): ListJournal[X] = ListJournal(x +: xs)

  /**
   * Returns an iterator over the elements of this collection.
   *
   * @return an `Iterator` containing the elements of this collection in order
   */
  def iterator: Iterator[X] = xs.iterator
}

/**
 * Companion object for the `ListJournal` class, providing factory methods for creating instances of `ListJournal`.
 */
object ListJournal {
  /**
   * Creates an empty `ListJournal` instance.
   *
   * @tparam X the type of elements that can be stored in the `ListJournal`
   * @return an instance of `ListJournal` with an empty `List`
   */
  def empty[X]: ListJournal[X] = ListJournal(List.empty)
}

/**
 * Represents a journal implemented as a list of elements of type `X`.
 *
 * `ListJournal` is an immutable data structure that extends the `Journal` trait.
 * It provides functionality to append elements and to iterate over the stored elements.
 *
 * @tparam X the type of elements stored in the journal
 * @param xs the list of elements in the journal
 */
case class QueueJournal[X](xs: Queue[X]) extends Journal[X] {
  /**
   * Appends the specified element to this `Appendable` object, returning a new instance
   * of the `Appendable` with the element included.
   *
   * @param x the element to be appended
   * @return a new `Appendable[X]` instance containing the existing elements and the newly appended element
   */
  def append(x: X): QueueJournal[X] = QueueJournal(xs.enqueue(x))

  /**
   * Returns an iterator over the elements of this collection.
   *
   * @return an `Iterator` containing the elements of this collection in order
   */
  def iterator: Iterator[X] = xs.iterator
}

/**
 * Companion object for the `QueueJournal` class, providing factory methods for creating instances of `QueueJournal`.
 */
object QueueJournal {
  /**
   * Creates an empty `QueueJournal` instance.
   *
   * @tparam X the type of elements that can be stored in the `QueueJournal`
   * @return an instance of `QueueJournal` with an empty `Queue`
   */
  def empty[X]: QueueJournal[X] = QueueJournal(Queue.empty)
}

/**
 * A `MapJournal` is a concrete implementation of the `Journal` trait that maintains a collection
 * of elements represented as key-value pairs using a `Map`.
 *
 * This class provides functionality to append new elements, retrieve elements by key,
 * and iterate over the contents of the journal. It ensures immutability by returning
 * a new instance when modifications, such as appending, are performed.
 *
 * @tparam K the type of keys maintained by the map
 * @tparam V the type of values associated with the keys
 * @param xs the underlying map storing the key-value pairs of the journal
 */
case class MapJournal[K, V](xs: Map[K, V]) extends Journal[(K, V)] {
  /**
   * Appends the specified element to this `Appendable` object, returning a new instance
   * of the `Appendable` with the element included.
   *
   * @param x the element to be appended
   * @return a new `Appendable[X]` instance containing the existing elements and the newly appended element
   */
  def append(x: (K, V)): MapJournal[K, V] = MapJournal(xs + x)

  /**
   * Returns an iterator over the elements of this collection.
   *
   * @return an `Iterator` containing the elements of this collection in order
   */
  def iterator: Iterator[(K, V)] = xs.iterator

  /**
   * Retrieves the value associated with the specified key from the underlying map.
   *
   * @param key the key to look up in the map
   * @return an `Option` containing the value associated with the key, or `None` if the key does not exist
   */
  def get(key: K): Option[V] = xs.get(key)
}

/**
 * Companion object for the `MapJournal` class.
 *
 * Provides utility methods related to the `MapJournal` data structure,
 * which represents a journal implemented as a map of key-value pairs.
 */
object MapJournal {
  /**
   * Creates and returns an empty instance of `MapJournal`.
   *
   * @return a new `MapJournal[K, V]` instance with no key-value pairs
   */
  def empty[K, V]: MapJournal[K, V] = MapJournal(Map.empty)
}