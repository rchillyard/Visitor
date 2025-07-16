package com.phasmidsoftware.visitor

import com.phasmidsoftware.visitor.DfsVisitor.recurseWithInVisit

/**
 * A specialized visitor implementing recursive traversal and processing logic.
 * Essentially, this method performs depth-first-search on a tree structure,
 * where a tree can, of course, be a subgraph of a graph.
 *
 * This case class extends `AbstractMultiVisitor` and is designed to handle recursive
 * visits of elements of type `X`, using the provided function `f` to generate sub-elements
 * for recursion. It is initialized with a map linking specific `Message` instances
 * to corresponding `Appendable[X]` objects.
 * The `Message` types supported by this method are: `Pre`, `In`, and `Post`.
 *
 * @param map a map associating `Message` types with `Appendable[X]` instances
 *            to manage state during the recursion process.
 * @param f   a function of type `X => Seq[X]` that defines how to retrieve
 *            sub-elements (children) for recursive processing from a given instance of `X`.
 * @tparam X the type of elements being visited and processed recursively.
 */
case class DfsVisitor[X](map: Map[Message, Appendable[X]], f: X => Seq[X]) extends AbstractMultiVisitor[X](map) {
  /**
   * Recursively processes an element of type `X` using a depth-first traversal strategy.
   * This method updates the internal state of the `DfsVisitor` at each step of the recursion,
   * applying pre-, in-, and post-visit modifications as determined by the `Pre`-, `In`- and `Post`-messages.
   *
   * @param x the element of type `X` to be processed and traversed recursively.
   * @return a new instance of `DfsVisitor` with the updated state after processing
   *         and visiting the provided element and its sub-elements.
   */
  def dfs(x: X): DfsVisitor[X] = {
    def performRecursion(xs: Seq[X], visitor: DfsVisitor[X]) =
      if (xs.length <= 2 && map.contains(In))
        recurseWithInVisit(visitor, x, xs)
      else
        xs.foldLeft(visitor)(_ dfs _)

    // First do a pre-visit, then a "SelfVisit", next perform the recursion, finally do a post-visit
    performRecursion(f(x), visit(Pre)(x).visit(SelfVisit)(x)).visit(Post)(x)
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
  override def visit(msg: Message)(x: X): DfsVisitor[X] = super.visit(msg)(x).asInstanceOf[DfsVisitor[X]]

  /**
   * Creates a new `Visitor` instance with the provided updated mapAppendables.
   *
   * This method is used to update the internal state of the Visitor by creating
   * a new instance with the modified mappings from `Message` to `Appendable`.
   *
   * @param map a map containing updated associations of `Message` to `Appendable[X]`
   * @return a new `Visitor[X]` instance that reflects the updated mapAppendables
   */
  def unit(map: Map[Message, Appendable[X]]): DfsVisitor[X] =
    copy(map = map)
}

/**
 * Contains utility methods for processing recursive logic within the `DfsVisitor` class.
 *
 * This object provides an internal helper function used to facilitate specific
 * recursive operations with handling of `In` messages during traversal.
 */
object DfsVisitor {

  /**
   * Performs a recursive traversal with specific handling for "In" messages.
   * This method processes the provided element `x` within a recursive structure
   * and updates the visitor state accordingly, ensuring it supports the case
   * of sequences with at most two elements.
   *
   * @param visitor the current `DfsVisitor` instance that maintains
   *                the state of the recursive traversal.
   * @param x       the element of type `X` that is being processed and traversed.
   * @param xs      a sequence of type `X` representing the sub-elements related to `x`.
   *                This sequence must contain at most two elements.
   */
  private def recurseWithInVisit[X](visitor: DfsVisitor[X], x: X, xs: Seq[X]) = {
    require(xs.length <= 2, "xs must contain at most two elements")
    val visitedLeft = xs.headOption.fold(visitor)(visitor.dfs)
    xs.lastOption.fold(visitedLeft.visit(In)(x))(visitedLeft.visit(In)(x).dfs)
  }
}