package com.phasmidsoftware.visitor

import scala.collection.immutable.Queue

/**
 * A trait representing a typeclass for queue-like data structures. It provides abstract operations
 * to enable offering (inserting) an element into a queue and taking (removing) an element from a queue.
 *
 * The `Queueable` trait works with a higher-kinded type `Q[_]`, representing a container or queue that holds elements of type `T`.
 * It allows implementing specific queue-like structures while adhering to a uniform interface for enqueueing and dequeueing elements.
 *
 * @tparam Q a higher-kinded type representing the queue-like container
 */
trait Queueable[Q[_]] {
  /**
   * Offers an element `t` to the queue `q` and returns a new queue containing the element.
   *
   * This method abstracts the operation of adding an element to a queue-like structure, ensuring
   * immutability of the underlying data structure by returning a new instance that includes the added element.
   *
   * @param q the queue-like container to which the element will be offered
   * @param t the element to be added to the queue
   * @tparam T the type of the element and container
   * @return a new queue-like container of type `Q[T]` with the element `t` added
   */
  def offer[T](q: Q[T])(t: T): Q[T]

  /**
   * Removes an element from the provided queue-like container and returns a tuple containing the
   * extracted element and a new queue-like container with the element removed.
   * 
   * CONSIDER returning (Option[T], Q[T]) in order to handle empty queueables.
   *
   * @param q the queue-like container of type `Q[T]` from which an element will be removed
   * @tparam T the type of the elements stored in the queue-like container
   * @return a tuple where the first element is the extracted element of type `T` and the second element
   *         is a new queue-like container of type `Q[T]` with the extracted element removed
   */
  def take[T](q: Q[T]): (T, Q[T])

  /**
   * Checks if the provided queue-like container is empty.
   *
   * This method abstractly determines whether a queue-like data structure
   * contains any elements, without mutating the original structure.
   *
   * @param q the queue-like container of type `Q[T]` to be checked
   * @tparam T the type of elements stored in the queue-like container
   * @return true if the queue-like container is empty, false otherwise
   */
  def isEmpty[T](q: Q[T]): Boolean
}

/**
 * This given instance provides an implementation of the `Queueable` typeclass for the `Queue` data structure.
 *
 * It defines how elements can be added (offered) to and removed (taken) from a `Queue` while adhering
 * to the `Queueable` interface. The operations leverage the `enqueue` method for appending elements
 * and the `dequeue` method for extracting elements, as applicable for the `Queue`.
 */
given Queueable[Queue] with {
  /**
   * Adds a given element to the end of the provided queue and returns a new queue with the element included.
   *
   * @param q the input queue of type `Queue[T]` to which the element will be added
   * @param t the element of type `T` to be appended to the queue
   * @return a new queue of type `Queue[T]` containing all elements of the input queue and the newly added element
   */
  def offer[T](q: Queue[T])(t: T): Queue[T] = q.enqueue(t)

  /**
   * Removes and returns the first element from the given queue along with the updated queue.
   *
   * @param q the queue from which the first element is to be removed
   * @return a tuple containing the removed element of type `T` and the updated queue
   */
  def take[T](q: Queue[T]): (T, Queue[T]) = q.dequeue

  /**
   * Determines whether the given queue is empty.
   *
   * @param q the queue to check for emptiness
   * @return true if the queue contains no elements, false otherwise
   */
  def isEmpty[T](q: Queue[T]): Boolean = q.isEmpty
}

/**
 * A given instance of the `Queueable` type class for the `PQLike` type, representing a priority queue.
 * This instance provides the implementation for the abstract methods `offer` and `take`, enabling
 * enqueueing and dequeueing operations specific to the `PQLike` type.
 *
 * The `offer` method appends an element into the `PQLike`, while the `take` method removes and returns the
 * highest-priority element, ensuring immutability by producing new `PQLike` instances.
 */
given Queueable[PQ] with {
  /**
   * Adds the specified element to the given priority queue and returns a new instance of the priority queue
   * with the element appended. The operation does not modify the original priority queue, preserving its immutability.
   *
   * @param q the priority queue to which the element should be added
   * @param t the element to be added to the priority queue
   * @tparam T the type of elements stored in the priority queue
   * @return a new `PQLike[T]` instance containing the existing elements along with the newly added element
   */
  def offer[T](q: PQ[T])(t: T): PQ[T] = q.append(t)

  /**
   * Removes and returns the highest-priority element from the specified priority queue,
   * along with the updated priority queue ensuring immutability.
   *
   * @param q the priority queue of type `PQLike[T]` from which the highest-priority element is extracted
   * @return a tuple containing the extracted element of type `T` and a new `PQLike[T]` instance
   *         with the element removed
   */
  def take[T](q: PQ[T]): (T, PQ[T]) = q.take

  /**
   * Checks whether the given priority queue is empty.
   *
   * @param q the priority queue to be checked
   * @return true if the priority queue is empty, false otherwise
   */
  def isEmpty[T](q: PQ[T]): Boolean = q.isEmpty
}
//
///**
// * Implicit implementation of the `Queueable` typeclass for the `PQ` data structure.
// * This provides methods for adding elements to the priority queue, removing the highest-priority element,
// * and checking if the queue is empty, adhering to the `Queueable` typeclass interface.
// *
// * The `PQ` is a maximum-priority queue, ensuring that elements are dequeued in order of
// * descending priority based on the implicit ordering of the element type.
// */
//given Queueable[PQ] with {
//  /**
//   * Appends the specified element `t` to the given maximum-priority queue `q` and returns the updated queue.
//   *
//   * @param q the instance of the maximum-priority queue to which the element `t` will be appended
//   * @param t the element to be appended to the priority queue
//   * @return the updated instance of the maximum-priority queue with the element `t` added
//   */
//  def offer[T](q: PQ[T])(t: T): PQ[T] = q.append(t)
//
//  /**
//   * Removes and returns the highest-priority element from the given maximum-priority queue,
//   * along with the updated priority queue.
//   *
//   * @param q the input maximum-priority queue from which the highest-priority element is to be removed
//   * @return a tuple containing the extracted element of type `T` and the updated `PQ[T]` instance
//   */
//  def take[T](q: PQ[T]): (T, PQ[T]) = q.take
//
//  /**
//   * Checks if the given maximum-priority queue is empty.
//   *
//   * @param q the `PQ` instance to be checked
//   * @return true if the priority queue is empty, false otherwise
//   */
//  def isEmpty[T](q: PQ[T]): Boolean = q.isEmpty
//}