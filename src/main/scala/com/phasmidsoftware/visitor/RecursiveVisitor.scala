package com.phasmidsoftware.visitor

import com.phasmidsoftware.visitor.RecursiveVisitor.{recurseWithInVisit, updatedAppendables}

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
case class RecursiveVisitor[X](map: Map[Message, Appendable[X]], f: X => Seq[X]) extends AbstractMultiVisitor[X](map) {

  /**
   * Recursively processes an element of type `X` using a depth-first traversal strategy.
   * This method updates the internal state of the `RecursiveVisitor` at each step of the recursion,
   * applying pre-, in-, and post-visit modifications as determined by the `Pre`-, `In`- and `Post`-messages.
   *
   * @param x the element of type `X` to be processed and traversed recursively.
   * @return a new instance of `RecursiveVisitor` with the updated state after processing
   *         and visiting the provided element and its sub-elements.
   */
  def recurse(x: X): RecursiveVisitor[X] = {
    val xs = f(x)
    val visitor = unit(updatedAppendables(x, Pre, map))
    val result: AbstractMultiVisitor[X] =
      if (Range.inclusive(0, 2).contains(xs.length) && map.contains(In))
        recurseWithInVisit(visitor, x, xs)
      else xs.foldLeft(visitor) {
        (v, child) => v.recurse(child)
      }
    unit(updatedAppendables(x, Post, result.mapAppendables))
  }

  /**
   * Creates a new `Visitor` instance with the provided updated mapAppendables.
   *
   * This method is used to update the internal state of the Visitor by creating
   * a new instance with the modified mappings from `Message` to `Appendable`.
   *
   * @param map a map containing updated associations of `Message` to `Appendable[X]`
   * @return a new `Visitor[X]` instance that reflects the updated mapAppendables
   */
  def unit(map: Map[Message, Appendable[X]]): RecursiveVisitor[X] =
    copy(map = map)
}

object RecursiveVisitor {
  def updatedAppendables[X](x: X, msg: Message, map: Map[Message, Appendable[X]]): Map[Message, Appendable[X]] = {
    map.map {
      case (k@`msg`, a) => (k, a.append(x))
      case e => e
    }
  }

  private def recurseWithInVisit[X](visitor: RecursiveVisitor[X], x: X, xs: Seq[X]) = {
    val maybeLeft = xs.headOption
    val maybeRight = xs.lastOption
    val visitedLeft = maybeLeft.fold(visitor)(visitor.recurse)
    val visitedLeftAndElement = visitedLeft.visit(In)(x)
    maybeRight.foldLeft(visitedLeftAndElement) {
      case (v: RecursiveVisitor[X], last) =>
        v.recurse(last)
    }
  }

}