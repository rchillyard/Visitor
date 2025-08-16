package com.phasmidsoftware.visitor

import scala.collection.mutable

/**
 * A trait representing a pseudo-immutable priority queue that extends the `Appendable` trait for adding elements of type `X`.
 *
 * `PQ` provides functionality to query if the queue is empty and to extract and return an element
 * alongside a new priority queue that excludes the extracted element, ensuring an immutable operation.
 *
 * @tparam X the type of elements stored in the priority queue
 */
trait PQ[X] extends Appendable[X] {
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
   * @return a tuple containing the extracted element of type `X` and a new `PQ[X]` instance
   *         with the element removed
   */
  def take: (X, PQ[X])

  /**
   * Appends the specified element to this `Appendable` object, returning a new instance
   * of the `Appendable` with the element included.
   *
   * @param x the element to be appended
   * @return a new `PQ[X]` instance containing the existing elements and the newly appended element
   */
  def append(x: X): PQ[X]
}

/**
 * An abstract class representing a priority queue, implemented with a mutable `PriorityQueue`.
 *
 * This class defines operations for checking if the queue is empty, appending elements,
 * extracting the highest-priority element, and closing the queue. The operations adhere to
 * the semantics defined by the `PQ` trait, ensuring pseudo-immutability where applicable.
 *
 * @constructor Creates an `AbstractPQ` wrapping an underlying mutable `PriorityQueue`.
 * @tparam X the type of elements stored in the priority queue
 * @param pq a mutable `PriorityQueue` instance used to manage the priority queue's internal state
 */
abstract class AbstractPQ[X](pq: mutable.PriorityQueue[X]) extends PQ[X] {
  /**
   * Removes and returns the highest-priority element from the priority queue,
   * along with the updated priority queue wrapped as `this`.
   *
   * @return a tuple containing the extracted element of type `X` and the current instance of the priority queue
   */
  def take: (X, PQ[X]) = (pq.dequeue(), this)

  /**
   * Appends the specified element to this priority queue, returning the updated instance.
   *
   * @param x the element to be appended to the priority queue
   * @return this `Appendable[X]` instance after the element has been added
   */
  def append(x: X): PQ[X] = {
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
 * Represents a maximum-priority queue for elements of type `X`, where the element ordering is determined
 * by an implicit `Ordering[X]`. This implementation wraps a mutable `PriorityQueue` to provide
 * the functionality as defined by the `AbstractPQ` class.
 *
 * The priority queue ensures that the highest-priority (maximum) element, as per the implicit ordering, 
 * is dequeued first. It supports operations such as appending elements, extracting the maximum element, 
 * checking if the queue is empty, and closing the queue.
 *
 * NOTE that there's currently no way to create a MaxPQ with an explicit priority queue.
 *
 * @tparam X the type of elements stored in the priority queue
 * @constructor Creates an instance of a maximum-priority queue.
 */
case class MaxPQ[X](pq: mutable.PriorityQueue[X]) extends AbstractPQ[X](pq) {
  /**
   * Removes and returns the highest-priority element from the priority queue,
   * along with the updated priority queue wrapped as `this`.
   *
   * @return a tuple containing the extracted element of type `X` and the current instance of the priority queue
   */
  override def take: (X, MaxPQ[X]) = super.take.asInstanceOf[(X, MaxPQ[X])]

  /**
   * Appends the specified element to this priority queue, returning the updated instance.
   *
   * @param x the element to be appended to the priority queue
   * @return this `Appendable[X]` instance after the element has been added
   */
  override def append(x: X): MaxPQ[X] = super.append(x).asInstanceOf[MaxPQ[X]]
}

/**
 * Companion object for the `MaxPQ` class, providing factory methods for constructing
 * instances of a maximum-priority queue.
 *
 * NOTE (Important) The pq provided already has its ordering determined. In that sense, the name MinPQ is very misleading.
 *
 * The methods in this object allow easy construction of `MaxPQ` instances either
 * from a collection of specified elements (via `apply`) or as an empty priority
 * queue (via `empty`). The priority queue is ordered based on the implicit `Ordering[X]`
 * of the element type.
 */
object MaxPQ {
  /**
   * Constructs a `MaxPQ` (maximum-priority queue) instance containing the provided elements `xs`.
   * The queue is ordered based on the implicit `Ordering[X]` for the element type `X`.
   *
   * @param xs a variable number of elements of type `X` to initialize the priority queue
   * @return an instance of `MaxPQ[X]` containing the specified elements
   */
  def apply[X: Ordering](xs: X*): MaxPQ[X] = MaxPQ(mutable.PriorityQueue(xs: _*))

  /**
   * Creates an empty instance of a maximum-priority queue for elements of type `X`.
   * The priority queue is initialized with an implicit `Ordering[X]` to determine
   * the element ordering.
   *
   * @return an empty `MaxPQ[X]` instance
   */
  def empty[X: Ordering]: MaxPQ[X] = MaxPQ(mutable.PriorityQueue.empty[X](using implicitly[Ordering[X]]))
}

/**
 * A case class representing a minimum priority queue (`MinPQ`), implemented as a wrapper around a reversed
 * Scala mutable `PriorityQueue`. The `MinPQ` ensures that the smallest element has the highest priority.
 *
 * NOTE (Important) The pq provided already has its ordering determined. In that sense, the name MinPQ is very misleading.
 *
 * @param pq the underlying mutable `PriorityQueue` instance used to manage the internal state
 *           of the `MinPQ`. The priority queue must implicitly provide an `Ordering` for type `X`.
 * @tparam X the type of elements stored in the priority queue, requiring an implicit `Ordering` instance
 */
case class MinPQ[X](pq: mutable.PriorityQueue[X]) extends AbstractPQ[X](pq) {
  /**
   * Removes and returns the highest-priority element from the priority queue,
   * along with the updated priority queue wrapped as `this`.
   *
   * @return a tuple containing the extracted element of type `X` and the current instance of the priority queue
   */
  override def take: (X, MinPQ[X]) = super.take.asInstanceOf[(X, MinPQ[X])]

  /**
   * Appends the specified element to this priority queue, returning the updated instance.
   *
   * @param x the element to be appended to the priority queue
   * @return this `Appendable[X]` instance after the element has been added
   */
  override def append(x: X): MinPQ[X] = super.append(x).asInstanceOf
}

/**
 * Companion object for the `MinPQ` class.
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
  def apply[X: Ordering](xs: X*): MinPQ[X] = MinPQ(mutable.PriorityQueue(xs: _*)(using implicitly[Ordering[X]]).reverse)

  /**
   * Creates and returns an empty minimum priority queue (`MinPQ`).
   * The queue is initialized with a reversed `PriorityQueue` to ensure the smallest
   * element always has the highest priority.
   *
   * @tparam X the type of elements stored in the priority queue, requiring an implicit `Ordering` instance
   * @return an empty `MinPQ[X]` instance
   */
  def empty[X: Ordering]: MinPQ[X] = MinPQ(mutable.PriorityQueue.empty(using implicitly[Ordering[X]]).reverse)
}