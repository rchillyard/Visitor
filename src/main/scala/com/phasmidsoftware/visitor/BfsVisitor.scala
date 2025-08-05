package com.phasmidsoftware.visitor

import scala.collection.immutable.Queue

/**
 * A case class implementing a breadth-first search (BFS) traversal strategy within a visitor pattern.
 *
 * The `BfsVisitor` class encapsulates the state and behavior necessary to perform BFS over elements of type `X`.
 * It utilizes a queue to manage the traversal order, a function to generate child elements, and a goal function
 * to define termination criteria. The visitor pattern is leveraged to support extensible processing of elements
 * during the traversal.
 *
 * @param queue a queue of elements of type `X` representing the current state of the BFS traversal.
 * @param map   a mapping from `Message` instances to `Appendable[X]`, used to manage processing behavior for each type of message.
 * @param f     a function that takes an element of type `X` and generates a sequence of child elements for further traversal.
 * @param goal  a predicate function used to determine if a specific element of type `X` satisfies the search goal.
 * @tparam X the type of elements being visited and processed by the `BfsVisitor`.
 */
case class BfsVisitor[X](queue: Queue[X], map: Map[Message, Appendable[X]], f: X => Seq[X], goal: X => Boolean) extends AbstractMultiVisitor[X](map) with Bfs[X, BfsVisitor[X]] {

  /**
   * Performs a breadth-first search (BFS) starting from the given element `x`.
   * This method initializes the queue with the specified element and triggers
   * the BFS traversal through the `inner` method.
   *
   * CONSIDER expanding this method to take a sequence of X values.
   *
   * @param x the starting element of type `X` for the BFS traversal.
   * @return a new instance of `BfsVisitor` representing the state after
   *         completing the BFS traversal.
   */
  def bfs(x: X): BfsVisitor[X] =
    copy(queue = Queue(x)).inner

  /**
   * Performs the inner recursive logic of a breadth-first search (BFS) traversal.
   *
   * This method executes the BFS traversal by processing elements from the queue,
   * visiting each element with a predefined message (`Pre`), and enqueuing the results
   * of applying the function `children` to the current element. The traversal continues recursively
   * until the queue is empty.
   *
   * @return a new instance of `BfsVisitor[X]` representing the state after completing
   *         the BFS traversal or the current state if the queue is empty.
   */
  def inner: BfsVisitor[X] =
    if (queue.isEmpty)
      this
    else
      queue.dequeue match {
        case (x, q) if goal(x) =>
          this.copy(queue = q)
        case (x, q) =>
          val visitor: BfsVisitor[X] = this.copy(queue = q).visit(Pre)(x)
          f(x).foldLeft(visitor)((v, x) => v.copy(queue = v.queue.enqueue(x))).inner
      }

  /**
   * Make a visit, with the given message and `X` value, on this `Visitor` and return a new `Visitor`.
   *
   * This method defines the behavior for handling a `Message` in the context
   * of the Visitor pattern. The implementation of this method should use the provided
   * message and state to determine the next state and return the appropriate `Visitor`.
   *
   * @param msg the message to be processed by the visitor
   * @param x   the current state or context associated with the visitor
   * @return a new `Visitor[X]` instance that represents the updated state after processing the message
   */
  override def visit(msg: Message)(x: X): BfsVisitor[X] = super.visit(msg)(x).asInstanceOf[BfsVisitor[X]]

  /**
   * Creates a new `Visitor` instance with the provided updated mapAppendables.
   *
   * This method is used to update the internal state of the Visitor by creating
   * a new instance with the modified mappings from `Message` to `Appendable`.
   *
   * @param map a map containing updated associations of `Message` to `Appendable[X]`
   * @return a new `Visitor[X]` instance that reflects the updated mapAppendables
   */
  def unit(map: Map[Message, Appendable[X]]): BfsVisitor[X] =
    copy(map = map)
}

/**
 * Contains utility methods for processing recursive logic within the `BfsVisitor` class.
 *
 * This object provides an internal helper function used to facilitate specific
 * recursive operations with handling of `In` messages during traversal.
 */
object BfsVisitor {
  /**
   * Constructs a new instance of `BfsVisitor[X]` to facilitate breadth-first search traversal.
   * It doesn't make a lot of sense to set up a Post-messaged BFS visitor so message is not a parameter of this
   * `create` method.
   *
   * @param journal an instance of `Appendable[X]` that maintains a collection of traversed elements.
   * @param f       a function that accepts an element of type `X` and produces a sequence of child elements for traversal.
   * @param goal    a predicate function that determines whether a given element of type `X` satisfies the search goal.
   * @tparam X the type of elements that the `BfsVisitor[X]` is designed to traverse and process.
   * @return a newly created `BfsVisitor[X]` instance configured with an empty queue, a map containing the given message and journal, and the provided traversal logic.
   */
  def create[X](journal: Appendable[X], f: X => Seq[X], goal: X => Boolean): BfsVisitor[X] =
    new BfsVisitor(Queue.empty, Map(Pre -> journal), f, goal)

  /**
   * Constructs a `BfsVisitor` configured for breadth-first traversal with a pre-visit strategy and an empty queue.
   *
   * This method initializes a `BfsVisitor` instance where the traversal is driven by a function to compute
   * child elements (`f`) and a predicate to determine the goal state (`goal`). It uses a pre-visit message (`Pre`)
   * and an empty `QueueJournal` for journaling the traversal progress.
   *
   * @param f    a function that takes an element of type `X` and generates a sequence of child elements for traversal
   * @param goal a predicate function used to determine whether a given element of type `X` matches the search goal
   * @tparam X the type of elements being traversed
   * @return a new instance of `BfsVisitor[X]` initialized for pre-visit strategy
   */
  def createQueue[X](f: X => Seq[X], goal: X => Boolean): BfsVisitor[X] =
    create(QueueJournal.empty[X], f, goal)


}
