package com.phasmidsoftware.visitor

import scala.collection.immutable.Queue

/**
 * A trait representing a journal that maintains and manages entries of type `X`.
 *
 * A `Journal` extends the `Appendable` trait, enabling the appending of elements
 * to create a new instance while retaining existing entries. It also provides
 * functionality to close the journal, performing any necessary cleanup or finalization.
 *
 * This trait can be used as a base for implementations that deal with logging, recording,
 * or managing sequences of data entries, while ensuring immutability or controlled updates.
 *
 * @tparam X the type of entries managed by this journal
 */
trait Journal[X] extends Appendable[X] {

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
 * A trait that represents an iterable journal, combining the functionalities of both the `Journal`
 * and `Iterable` traits. 
 *
 * `IterableJournal` is intended to maintain a collection of elements of type `X` that
 * can be both appended (as supported by `Journal`) and iterated over (as supported by `Iterable`).
 *
 * This abstraction is useful for scenarios where a sequence of log-like entries or journaled
 * elements is required, while also enabling iteration for traversal, filtering, mapping, or other
 * higher-order functions.
 *
 * @tparam X the type of elements contained in the iterable journal
 */
trait IterableJournal[X] extends Journal[X] with Iterable[X]

/**
 * A trait that combines the behavior of a function and a journal.
 *
 * `FunctionMappedJournal` extends both a function `(X => Y)` and `Journal[(X, Y)]`.
 * This allows it to act as a function that maps input of type `X` to output of type `Y`,
 * while also maintaining a journal of past inputs and outputs in the form of `(X, Y)` pairs.
 *
 * The journal functionality provides the capability to keep track of operations
 * performed by the function, making it suitable for scenarios like auditing, logging,
 * or building histories of function calls and results.
 *
 * @tparam X the type of input to the function
 * @tparam Y the type of output from the function
 */
trait FunctionMappedJournal[X, Y] extends Journal[(X, Y)] with (X => Y)

/**
 * Represents a journal implemented as a list of elements of type `X`.
 *
 * `ListJournal` is an immutable data structure that extends the `Journal` trait.
 * It provides functionality to append elements and to iterate over the stored elements.
 *
 * @tparam X the type of elements stored in the journal
 * @param list the list of elements in the journal
 */
case class ListJournal[X](list: List[X]) extends IterableJournal[X] {
  /**
   * Appends the specified element to this `Appendable` object, returning a new instance
   * of the `Appendable` with the element included.
   *
   * @param x the element to be appended
   * @return a new `Appendable[X]` instance containing the existing elements and the newly appended element
   */
  def append(x: X): ListJournal[X] =
    ListJournal(x +: list)

  /**
   * Returns an iterator over the elements of this collection.
   *
   * @return an `Iterator` containing the elements of this collection in order
   */
  def iterator: Iterator[X] =
    list.iterator
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
  def empty[X]: ListJournal[X] =
    ListJournal(List.empty)
}

/**
 * Represents a journal implemented as a list of elements of type `X`.
 *
 * `ListJournal` is an immutable data structure that extends the `Journal` trait.
 * It provides functionality to append elements and to iterate over the stored elements.
 *
 * @tparam X the type of elements stored in the journal
 * @param queue the list of elements in the journal
 */
case class QueueJournal[X](queue: Queue[X]) extends IterableJournal[X] {
  /**
   * Appends the specified element to this `Appendable` object, returning a new instance
   * of the `Appendable` with the element included.
   *
   * @param x the element to be appended
   * @return a new `Appendable[X]` instance containing the existing elements and the newly appended element
   */
  def append(x: X): QueueJournal[X] =
    QueueJournal(queue.enqueue(x))

  /**
   * Returns an iterator over the elements of this collection.
   *
   * @return an `Iterator` containing the elements of this collection in order
   */
  def iterator: Iterator[X] =
    queue.iterator
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
  def empty[X]: QueueJournal[X] =
    QueueJournal(Queue.empty)
}

/**
 * An abstract class representing a map-based implementation of a `Journal`.
 * This class provides mechanisms to manage key-value pairs immutably in a journal-like structure.
 * It extends the `Journal` trait, implementing appendable and iterable behaviors.
 *
 * The `AbstractMapJournal` relies on an underlying immutable `Map` to store key-value pairs,
 * allowing retrieval, iteration, and the addition of elements in a functional style.
 *
 * @tparam K the type of keys maintained by this journal
 * @tparam V the type of values associated with keys in this journal
 * @constructor Initializes the journal with an immutable map of key-value pairs
 * @param map the underlying immutable map containing the journal entries
 */
abstract class AbstractMapJournal[K, V](map: Map[K, V]) extends Journal[(K, V)] {

  /**
   * Appends the specified element to this `Appendable` object, returning a new instance
   * of the `Appendable` with the element included.
   *
   * @param x the element to be appended
   * @return a new `Appendable[X]` instance containing the existing elements and the newly appended element
   */
  def append(x: (K, V)): AbstractMapJournal[K, V] =
    unit(map + x)

  /**
   * Retrieves the value associated with the specified key from the underlying map.
   *
   * @param key the key to look up in the map
   * @return an `Option` containing the value associated with the key, or `None` if the key does not exist
   */
  def get(key: K): Option[V] =
    map.get(key)

  /**
   * Retrieves the value associated with the specified key from the underlying map.
   *
   * @param key the key to look up in the map
   * @return the value associated with the specified key
   * @throws NoSuchElementException if the key does not exist in the map
   */
  def apply(key: K): V =
    map(key)

  /**
   * Retrieves the keys of the underlying map as an iterable collection.
   *
   * @return an `Iterable` containing all the keys present in the map
   */
  def keys: Iterable[K] =
    map.keys

  /**
   * Retrieves the values of the underlying map as an iterable collection.
   *
   * @return an `Iterable` containing all the values present in the map
   */
  def values: Iterable[V] =
    map.values

  /**
   * Creates a new instance of `AbstractMapJournal` with the given map.
   *
   * @param xs the map to be used for initializing the new `AbstractMapJournal` instance
   * @return a new instance of `AbstractMapJournal` containing the elements of the provided map
   */
  def unit(xs: Map[K, V]): AbstractMapJournal[K, V]
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
 * @param map the underlying map storing the key-value pairs of the journal
 */
case class MapJournal[K, V](map: Map[K, V]) extends AbstractMapJournal[K, V](map) {

  /**
   * Appends the specified element to this `Appendable` object, returning a new instance
   * of the `Appendable` with the element included.
   *
   * @param x the element to be appended
   * @return a new `Appendable[X]` instance containing the existing elements and the newly appended element
   */
  override def append(x: (K, V)): MapJournal[K, V] =
    super.append(x).asInstanceOf[MapJournal[K, V]]

  /**
   * Creates a new `MapJournal` instance containing the specified key-value pairs.
   *
   * This method is a utility for constructing a `MapJournal` from an existing `Map`.
   *
   * @param xs the key-value pairs to be included in the new `MapJournal`
   * @return a new instance of `MapJournal` containing the provided key-value pairs
   */
  def unit(xs: Map[K, V]): MapJournal[K, V] =
    MapJournal(xs)
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
  def empty[K, V]: MapJournal[K, V] =
    MapJournal(Map.empty)
}

/**
 * `FunctionMapJournal` is a case class that represents a specialized implementation of `AbstractMapJournal`,
 * which maintains a journal of key-value pairs along with a function that can derive values
 * based on keys. It extends both `AbstractMapJournal` and `FunctionMappedJournal`, providing a combination
 * of stateful journal behavior and functional operations.
 *
 * @param xs a map containing the initial key-value pairs in the journal
 * @param f  a function that, given a key, computes its corresponding value
 * @tparam K the type of the keys in the journal
 * @tparam V the type of the values in the journal
 */
case class FunctionMapJournal[K, V](xs: Map[K, V])(f: K => V) extends AbstractMapJournal[K, V](xs) with FunctionMappedJournal[K, V] {

  /**
   * Applies the function `f` to the given key and returns the resulting value.
   * This method uses the stored function `f` to compute the value associated with the provided key.
   * TESTME is this method ever actually invoked? It seems like we always invoke apply((K,V))
   * CONSIDER if this method isn't invoked, then we don't need the f parameter in the declaration.
   *
   * @param key the key for which the value is to be computed using the stored function `f`
   * @return the value corresponding to the provided key, computed by the function `f`
   */
  override def apply(key: K): V =
    f(key)

  /**
   * Appends a new entry to the journal by applying the stored function `f` to the given key `k`.
   * The resulting key-value pair is then appended to the internal map, producing a new instance
   * of `FunctionMapJournal` that reflects the updated state.
   *
   * @param k the key for which the function `f` will be applied to generate its corresponding value
   * @return a new `FunctionMapJournal[K, V]` instance containing the updated mapping
   */
  def appendByFunction(k: K): FunctionMapJournal[K, V] =
    append(k -> apply(k)).asInstanceOf[FunctionMapJournal[K, V]]

  /**
   * Creates a new instance of `AbstractMapJournal` with the given map.
   *
   * @param xs the map to be used for initializing the new `AbstractMapJournal` instance
   * @return a new instance of `AbstractMapJournal` containing the elements of the provided map
   */
  def unit(xs: Map[K, V]): AbstractMapJournal[K, V] =
    FunctionMapJournal(xs)(f)
}

/**
 * An implementation of a journal backed by a map and a key-value computing function.
 *
 * The `FunctionMapJournal` stores a map of keys and their associated values, as well as a function `f`
 * that computes the value for a given key. It provides methods to compute values, append to the journal
 * by applying the function `f` to a key, and create new instances with updated maps.
 *
 * This class is designed to be used in scenarios where values for keys need to be dynamically generated
 * or updated using a predefined function.
 *
 */
object FunctionMapJournal {
  /**
   * Creates an empty instance of `FunctionMapJournal` with an initial empty map
   * and a function `f` that computes values based on keys.
   *
   * @param f a function that, given a key of type `K`, computes a corresponding value of type `V`
   * @return a new `FunctionMapJournal[K, V]` instance with an empty map and the provided function
   */
  def empty[K, V](f: K => V): FunctionMapJournal[K, V] =
    FunctionMapJournal(Map.empty)(f)
}