package com.phasmidsoftware.visitor.core

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
trait FunctionMappedJournal[X, Y, A <: FunctionMappedJournal[X, Y, A]] extends Journal[(X, Y), A] with (X => Y)

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
abstract class AbstractMapJournal[K, V, A <: AbstractMapJournal[K, V, A]](map: Map[K, V]) extends Journal[(K, V), A] {

  /**
   * Appends the specified element to this `Appendable` object, returning a new instance
   * of the `Appendable` with the element included.
   *
   * @param x the element to be appended
   * @return a new `Appendable[X]` instance containing the existing elements and the newly appended element
   */
  def append(x: (K, V)): A =
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
  def values: Iterable[V] = {
    map.values
  }

  /**
   * Retrieves the entries of the underlying map as an iterable collection of key-value pairs.
   *
   * @return an `Iterable` containing all key-value pairs present in the map
   */
  def entries: Iterable[(K, V)] = map

  /**
   * Creates a new instance of `AbstractMapJournal` with the given map.
   *
   * @param xs the map to be used for initializing the new `AbstractMapJournal` instance
   * @return a new instance of `AbstractMapJournal` containing the elements of the provided map
   */
  def unit(xs: Map[K, V]): A
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
case class MapJournal[K, V](map: Map[K, V]) extends AbstractMapJournal[K, V, MapJournal[K, V]](map) {

  /**
   * Appends the specified element to this `Appendable` object, returning a new instance
   * of the `Appendable` with the element included.
   *
   * @param x the element to be appended
   * @return a new `Appendable[X]` instance containing the existing elements and the newly appended element
   */
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

  def close(): Unit = ()
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
case class FunctionMapJournal[K, V](xs: Map[K, V])(f: K => V) extends AbstractMapJournal[K, V, FunctionMapJournal[K, V]](xs) with FunctionMappedJournal[K, V, FunctionMapJournal[K, V]] {

  /**
   * Applies the function `f` to the given key and returns the resulting value.
   * This method uses the stored function `f` to compute the value associated with the provided key.
   * NOTE: invoked by `appendByFunction`, which is in turn exercised only by the specs — see
   * issue #31, which considers the fate of this package.
   *
   * @param key the key for which the value is to be computed using the stored function `f`
   * @return the value corresponding to the provided key, computed by the function `f`
   */
  override def apply(key: K): V =
    f(key)

  /**
   * Appends a new entry to the journal by applying the stored function `f` to the given key `h`.
   * The resulting key-value pair is then appended to the internal map, producing a new instance
   * of `FunctionMapJournal` that reflects the updated state.
   *
   * @param k the key for which the function `f` will be applied to generate its corresponding value
   * @return a new `FunctionMapJournal[K, V]` instance containing the updated mapping
   */
  def appendByFunction(k: K): FunctionMapJournal[K, V] =
    append(k -> apply(k))

  /**
   * Creates a new instance of `AbstractMapJournal` with the given map.
   *
   * @param xs the map to be used for initializing the new `AbstractMapJournal` instance
   * @return a new instance of `AbstractMapJournal` containing the elements of the provided map
   */
  def unit(xs: Map[K, V]): FunctionMapJournal[K, V] =
    FunctionMapJournal(xs)(f)

  def close(): Unit = ()
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