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
case class MaxPQ[X: Ordering]() extends AbstractPQ[X](new mutable.PriorityQueue[X])

/**
 * A case class representing a minimum priority queue.
 *
 * The `MinPQ` class extends the functionality of `AbstractPQ` by using a reversed `PriorityQueue`
 * to manage elements in ascending order of priority, ensuring that the smallest element has the highest priority.
 *
 * NOTE that there's currently no way to create a MinPQ with an explicit priority queue.
 *
 * @tparam X the type of elements stored in the priority queue, which must provide evidence of an implicit `Ordering`
 */
case class MinPQ[X: Ordering]() extends AbstractPQ[X](new mutable.PriorityQueue[X](using implicitly[Ordering[X]]).reverse)
