package com.phasmidsoftware.visitor

import scala.Predef.->
import scala.annotation.tailrec
import scala.collection.immutable.Queue
import scala.collection.mutable
//
///**
// * A concrete implementation of a breadth-first search (BFS) visitor with priority-based queueing functionality.
// *
// * The `BfsPriorityVisitor` class is designed for traversing data structures using a breadth-first search strategy,
// * supported by a priority queue for managing traversal order. It integrates visitor pattern functionalities
// * and supports goal-based search through a user-defined predicate.
// *
// * @tparam X the type of elements being traversed
// * @param pq   the priority queue implementation used for managing traversal elements
// * @param f    a function that takes an element of type `X` and returns a sequence of child elements for traversal
// * @param goal a predicate function that determines whether a given element satisfies the search goal
// */
//case class BfsPriorityVisitor[X](pq: PQ[X], f: X => Seq[X], goal: X => Boolean) extends AbstractVisitor[X]() with Bfs[X, BfsVisitor[X]] {
//
//  /**
//   * Performs a breadth-first search (BFS) starting with the given key `k`.
//   *
//   * @param k the starting key of type `K` to begin the BFS traversal
//   * @return a result of type `R` which is a subtype of `Visitor[_]`, representing the outcome of the BFS traversal
//   */
//  def bfs(k: X): BfsVisitor[X] =
//    throw new UnsupportedOperationException(s"Visitor $this is not a BfsPriorityVisitor")
//
//  /**
//   * Performs a breadth-first search (BFS) starting from the given element `x`.
//   * This method initializes the queue with the specified element and triggers
//   * the BFS traversal through the `inner` method.
//   *
//   * CONSIDER expanding this method to take a sequence of X values.
//   *
//   * @param x the starting element of type `X` for the BFS traversal.
//   * @return a new instance of `BfsVisitor` representing the state after
//   *         completing the BFS traversal.
//   */
//  def bfsg(x: X): (BfsPriorityVisitor[X], Option[X]) = {
//    pq.append(x)
//    inner
//  }
//
//  /**
//   * Make a visit, with the given message and `X` value, on this `Visitor` and return a new `Visitor`.
//   *
//   * This method defines the behavior for handling a `Message` in the context
//   * of the Visitor pattern. The implementation of this method should use the provided
//   * message and state to determine the next state and return the appropriate `Visitor`.
//   *
//   * @param msg the message to be processed by the visitor
//   * @param x   the current state or context associated with the visitor
//   * @return a new `Visitor[X]` instance that represents the updated state after processing the message
//   */
//  override def visit(msg: Message)(x: X): BfsPriorityVisitor[X] = 
//    if (open)
//      if (msg == Pre)
//        copy(pq = pq.append(x))
//      else
//        this
//    else
//      throw new UnsupportedOperationException(s"Visitor $this is closed")
//
//  /**
//   * Retrieves the collection of `Appendable[X]` instances associated with this `AbstractVisitor`.
//   *
//   * The method provides access to all the appendable entities that the visitor interacts with.
//   * This can be useful for iterating over, modifying, or closing the appendables as a group.
//   *
//   * @return an `Iterable` containing the appendable elements of type `Appendable[X]` associated with this visitor
//   */
//  def appendables: Iterable[Appendable[X]] = Seq(pq)
//
//  /**
//   * Performs the inner recursive logic of a breadth-first search (BFS) traversal.
//   *
//   * This method executes the BFS traversal by processing elements from the queue,
//   * visiting each element with a predefined message (`Pre`), and enqueuing the results
//   * of applying the function `children` to the current element. The traversal continues recursively
//   * until the queue is empty.
//   *
//   * @return a new instance of `BfsVisitor[X]` representing the state after completing
//   *         the BFS traversal or the current state if the queue is empty.
//   */
//  @tailrec
//  private def inner: (BfsPriorityVisitor[X], Option[X]) =
//    if (pq.isEmpty)
//      this -> None
//    else
//      pq.take match {
//        case (q, x) if goal(x) =>
//          this.copy(pq = q) -> Some(x)
//        case (q, x) =>
//          val visitor: BfsPriorityVisitor[X] = this.copy(pq = q).visit(Pre)(x)
//          f(x).foldLeft(visitor)((v, x) => v.copy(pq = v.pq.append(x))).inner
//      }
//}
//
///**
// * Contains utility methods for processing recursive logic within the `BfsVisitor` class.
// *
// * This object provides an internal helper function used to facilitate specific
// * recursive operations with handling of `In` messages during traversal.
// */
//object BfsPriorityVisitor {
//  /**
//   * Constructs a new instance of `BfsVisitor[X]` to facilitate breadth-first search traversal.
//   * It doesn't make a lot of sense to set up a Post-messaged BFS visitor so message is not a parameter of this
//   * `create` method.
//   *
//   * @param journal an instance of `Appendable[X]` that maintains a collection of traversed elements.
//   * @param f       a function that accepts an element of type `X` and produces a sequence of child elements for traversal.
//   * @param goal    a predicate function that determines whether a given element of type `X` satisfies the search goal.
//   * @tparam X the type of elements that the `BfsVisitor[X]` is designed to traverse and process.
//   * @return a newly created `BfsVisitor[X]` instance configured with an empty queue, a map containing the given message and journal, and the provided traversal logic.
//   */
//  def create[X](journal: Appendable[X], f: X => Seq[X], goal: X => Boolean): BfsVisitor[X] =
//    new BfsVisitor(Queue.empty, Map(Pre -> journal), f, goal)
//
//  /**
//   * Constructs a `BfsVisitor` configured for breadth-first traversal with a pre-visit strategy and an empty queue.
//   *
//   * This method initializes a `BfsVisitor` instance where the traversal is driven by a function to compute
//   * child elements (`f`) and a predicate to determine the goal state (`goal`). It uses a pre-visit message (`Pre`)
//   * and an empty `QueueJournal` for journaling the traversal progress.
//   *
//   * @param f    a function that takes an element of type `X` and generates a sequence of child elements for traversal
//   * @param goal a predicate function used to determine whether a given element of type `X` matches the search goal
//   * @tparam X the type of elements being traversed
//   * @return a new instance of `BfsVisitor[X]` initialized for pre-visit strategy
//   */
//  def createQueue[X](f: X => Seq[X], goal: X => Boolean): BfsVisitor[X] =
//    create(QueueJournal.empty[X], f, goal)
//
//
//}
