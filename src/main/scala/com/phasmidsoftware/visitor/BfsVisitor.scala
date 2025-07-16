package com.phasmidsoftware.visitor

import scala.collection.immutable.Queue

/**
 * A specialized visitor implementing recursive traversal and processing logic.
 * Essentially, this method performs breadth-first-search on a tree structure,
 * where a tree can, of course, be a subgraph of a graph.
 *
 * This case class extends `AbstractMultiVisitor` and is designed to handle recursive
 * visits of elements of type `X`, using the provided function `f` to generate sub-elements
 * for recursion. It is initialized with a map linking specific `Message` instances
 * to corresponding `Appendable[X]` objects.
 * The `Message` types supported by this method are: `Pre`, `In`, and `Post`.
 *
 * @param queue a queue which starts out empty and is used to navigate through the tree.
 * @param map   a map associating `Message` types with `Appendable[X]` instances
 *              to manage state during the recursion process.
 * @param f     a function of type `X => Seq[X]` that defines how to retrieve
 *              sub-elements (children) for recursive processing from a given instance of `X`.
 * @tparam X the type of elements being visited and processed recursively.
 */
case class BfsVisitor[X](queue: Queue[X], map: Map[Message, Appendable[X]], f: X => Seq[X]) extends AbstractMultiVisitor[X](map) {

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
   * of applying the function `f` to the current element. The traversal continues recursively
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
   * Creates a new instance of `BfsVisitor` that facilitates recursive traversal
   * and processing of elements based on the given parameters.
   *
   * @param map a map that associates `Message` types with `Appendable[X]` instances.
   *            This map manages the state and actions performed on the elements being visited.
   * @param f   a function of type `X => Seq[X]` that generates a sequence of sub-elements
   *            (children) for recursive traversal from a given element of type `X`.
   * @tparam X the type of elements to be visited and processed.
   * @return a new instance of `BfsVisitor[X]` initialized with an empty queue, the specified map,
   *         and the function for generating sub-elements.
   */
  def apply[X](map: Map[Message, Appendable[X]], f: X => Seq[X]): BfsVisitor[X] = new BfsVisitor(Queue.empty, map, f)
}