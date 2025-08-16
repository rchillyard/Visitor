package com.phasmidsoftware.visitor

import scala.collection.mutable

/**
 * A trait representing a pseudo-immutable priority queue that extends the `Appendable` trait for adding elements of type `X`.
 *
 * `PQLike` provides functionality to query if the queue is empty and to extract and return an element
 * alongside a new priority queue that excludes the extracted element, ensuring an immutable operation.
 *
 * @tparam X the type of elements stored in the priority queue
 */
trait PQLike[X] extends Appendable[X] {
  /**
   * Indicates whether the priority queue is empty.
   *
   * @return true if the priority queue contains no elements, false otherwise
   */
  def isEmpty: Boolean

  /**
   * Extracts and returns an element from the priority queue along with a new priority queue
   * that excludes the extracted element. The operation does not modify the original priority queue,
   * preserving its immutability.
   *
   * @return a tuple containing the extracted element of type `X` and a new `PQLike[X]` instance
   *         with the element removed
   */
  def take: (X, PQLike[X])

  /**
   * Appends the specified element to this `Appendable` object, returning a new instance
   * of the `Appendable` with the element included.
   *
   * @param x the element to be appended
   * @return a new `PQLike[X]` instance containing the existing elements and the newly appended element
   */
  def append(x: X): PQLike[X]
}

/**
 * An abstract class representing a priority queue, implemented with a mutable `PriorityQueue`.
 *
 * This class defines operations for checking if the queue is empty, appending elements,
 * extracting the highest-priority element, and closing the queue. The operations adhere to
 * the semantics defined by the `PQLike` trait, ensuring pseudo-immutability where applicable.
 *
 * @constructor Creates an `AbstractPQ` wrapping an underlying mutable `PriorityQueue`.
 * @tparam X the type of elements stored in the priority queue
 * @param pq a mutable `PriorityQueue` instance used to manage the priority queue's internal state
 */
abstract class AbstractPQ[X](pq: mutable.PriorityQueue[X]) extends PQLike[X] {
  /**
   * Removes and returns the highest-priority element from the priority queue,
   * along with the updated priority queue wrapped as `this`.
   *
   * @return a tuple containing the extracted element of type `X` and the current instance of the priority queue
   */
  def take: (X, PQLike[X]) = (pq.dequeue(), this)

  /**
   * Appends the specified element to this priority queue, returning the updated instance.
   *
   * @param x the element to be appended to the priority queue
   * @return this `Appendable[X]` instance after the element has been added
   */
  def append(x: X): PQLike[X] = {
    pq += x
    this
  }

  /**
   * Checks if the priority queue is empty.
   *
   * @return true if the priority queue is empty, false otherwise
   */
  def isEmpty: Boolean = pq.isEmpty

  /**
   * Closes the priority queue, performing any necessary cleanup operations.
   *
   * @return Unit, indicating that the method performs the close operation without returning a value
   */
  def close(): Unit = {
  }
}

/**
 * A concrete implementation of a priority queue that extends the `AbstractPQ` class,
 * using a mutable `PriorityQueue` to manage its internal state.
 *
 * @constructor Creates an instance of `PQ` wrapping the specified mutable `PriorityQueue`.
 * @tparam X the type of elements stored in the priority queue
 * @param pq a mutable `PriorityQueue` instance used to back the priority queue
 */
case class PQ[X](pq: mutable.PriorityQueue[X]) extends AbstractPQ[X](pq) {
  /**
   * Removes and returns the highest-priority element from the priority queue,
   * along with the updated priority queue wrapped as `this`.
   *
   * @return a tuple containing the extracted element of type `X` and the current instance of the priority queue
   */
  override def take: (X, PQ[X]) = super.take.asInstanceOf[(X, PQ[X])]

  /**
   * Appends the specified element to this priority queue, returning the updated instance.
   *
   * @param x the element to be appended to the priority queue
   * @return this `Appendable[X]` instance after the element has been added
   */
  override def append(x: X): PQ[X] = super.append(x).asInstanceOf[PQ[X]]
}

/**
 * Companion object for the `PQ` class, providing factory methods for constructing
 * instances of a maximum-priority queue.
 *
 * The methods in this object allow easy construction of `PQ` instances either
 * from a collection of specified elements (via `apply`) or as an empty priority
 * queue (via `empty`). The priority queue is ordered based on the implicit `Ordering[X]`
 * of the element type.
 */
object MaxPQ {
  /**
   * Constructs a `PQ` (maximum-priority queue) instance containing the provided elements `xs`.
   * The queue is ordered based on the implicit `Ordering[X]` for the element type `X`.
   *
   * @param xs a variable number of elements of type `X` to initialize the priority queue
   * @return an instance of `PQ[X]` containing the specified elements
   */
  def apply[X: Ordering](xs: X*): PQ[X] = PQ(mutable.PriorityQueue(xs: _*))

  /**
   * Creates an empty instance of a maximum-priority queue for elements of type `X`.
   * The priority queue is initialized with an implicit `Ordering[X]` to determine
   * the element ordering.
   *
   * @return an empty `PQ[X]` instance
   */
  def empty[X: Ordering]: PQ[X] = PQ(mutable.PriorityQueue.empty[X](using implicitly[Ordering[X]]))
}

/**
 * Object for creating minimum PQs..
 *
 * Provides factory methods to create instances of the minimum priority queue (`MinPQ`).
 * The `MinPQ` uses a reversed `PriorityQueue` to manage elements such that the smallest element
 * is always at the front of the queue.
 */
object MinPQ {
  /**
   * Constructs a `MinPQ` instance using the provided sequence of elements.
   * The elements are stored in a reversed `PriorityQueue` to maintain a minimum priority queue,
   * where the smallest element has the highest priority.
   *
   * @param xs the sequence of elements of type `X` to initialize the minimum priority queue
   *           (must provide evidence of an implicit `Ordering` for type `X`)
   * @return a `MinPQ[X]` instance containing the elements in ascending order of priority
   */
  def apply[X: Ordering](xs: X*): PQ[X] = PQ(mutable.PriorityQueue(xs: _*)(using implicitly[Ordering[X]]).reverse)

  /**
   * Creates and returns an empty minimum priority queue (`MinPQ`).
   * The queue is initialized with a reversed `PriorityQueue` to ensure the smallest
   * element always has the highest priority.
   *
   * @tparam X the type of elements stored in the priority queue, requiring an implicit `Ordering` instance
   * @return an empty `MinPQ[X]` instance
   */
  def empty[X: Ordering]: PQ[X] = PQ(mutable.PriorityQueue.empty(using implicitly[Ordering[X]]).reverse)
}