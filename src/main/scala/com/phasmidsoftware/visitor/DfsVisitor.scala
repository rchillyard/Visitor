package com.phasmidsoftware.visitor

import com.phasmidsoftware.visitor.DfsVisitor.recurseWithInVisit

/**
 * A specialized visitor implementing recursive traversal and processing logic.
 * Essentially, this method performs depth-first-search on a tree structure,
 * where a tree can, of course, be a subgraph of a graph.
 *
 * This case class extends `AbstractMultiVisitor` and is designed to handle recursive
 * visits of elements of type `X`, using the provided function `children` to generate sub-elements
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
case class DfsVisitor[X](map: Map[Message, Appendable[X]], f: X => Seq[X]) extends AbstractMultiVisitor[X](map) with Dfs[X, DfsVisitor[X]] {
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
        DfsVisitor.recurseWithInVisit(visitor, x, xs)
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
   * CONSIDER should we pass in the dfs function as a parameter?
   *
   * @param visitor the current `DfsVisitor` instance that maintains
   *                the state of the recursive traversal.
   * @param x       the element of type `X` that is being processed and traversed.
   * @param xs      a sequence of type `X` representing the sub-elements related to `x`.
   *                This sequence must contain at most two elements.
   */
  private def recurseWithInVisit[X](visitor: DfsVisitor[X], x: X, xs: Seq[X]) = {
    require(xs.length <= 2, "xs must contain at most two elements")
    val visitor1 = xs.headOption.fold(visitor)(visitor.dfs)
    val visitor2 = visitor1.visit(In)(x)
    xs.lastOption.fold(visitor2)(visitor2.dfs)
  }
}

/**
 * A case class for managing depth-first traversal using the Visitor design pattern based on a key-value pair.
 *
 * `DfsVisitorMapped` combines a custom traversal strategy with visitor behavior,
 * allowing for recursive traversal of elements (`K`) while applying transformations (`f`)
 * to produce associated values (`V`). The traversal is guided by the map of `Message` to `Appendable`
 * and the `children` function that specifies the structure to navigate.
 *
 * @param map      a mapping from `Message` instances to `Appendable[(K, V)]`, defining how messages are handled.
 * @param f        a function that transforms an element of type `K` into a value of type `V`.
 * @param children a function that takes an element of type `K` and returns a sequence of its child elements to traverse.
 * @tparam K the type of the keys or elements being traversed.
 * @tparam V the type of the associated values produced during the traversal process.
 */
case class DfsVisitorMapped[K, V](map: Map[Message, Appendable[(K, V)]], f: K => V, children: K => Seq[K]) extends AbstractMultiVisitor[(K, V)](map) with Dfs[K, DfsVisitorMapped[K, V]] {
  /**
   * Performs a depth-first traversal starting from the provided key `k`.
   * This method applies visitor operations in a specific sequence: pre-visit, self-visit, recursive traversal, and post-visit.
   *
   * Depending on the conditions of the children of the key, the traversal may apply special in-visit logic if there are exactly two children.
   * Returns an updated `DfsVisitorMapped` instance reflecting the changes made during the traversal.
   *
   * @param k the starting key for the depth-first search operation
   * @return a new `DfsVisitorMapped[K, V]` instance after performing the DFS traversal from the given key
   */
  def dfs(k: K): DfsVisitorMapped[K, V] = {
    val v = f(k)
    val kv = k -> v

    def performRecursion(xs: Seq[K], visitor: DfsVisitorMapped[K, V]) =
      if (xs.length <= 2 && map.contains(In))
        DfsVisitorMapped.recurseWithInVisit(visitor, k, v, xs)
      else
        xs.foldLeft(visitor)(_ dfs _)

    // First do a pre-visit, then a "SelfVisit", next perform the recursion, finally do a post-visit
    performRecursion(children(k), visit(Pre)(kv).visit(SelfVisit)(kv)).visit(Post)(kv)
  }

  /**
   * Make a visit, with the given message and `(K,V)` value, on this `Visitor` and return a new `Visitor`.
   *
   * This method defines the behavior for handling a `Message` in the context
   * of the Visitor pattern. The implementation of this method should use the provided
   * message and state to determine the next state and return the appropriate `Visitor`.
   *
   * @param msg the message to be processed by the visitor
   * @param kv   the current state or context associated with the visitor
   * @return a new `DfsVisitorMapped[K, V]` instance that represents the updated state after processing the message
   */
  override def visit(msg: Message)(kv: (K, V)): DfsVisitorMapped[K, V] =
    super.visit(msg)(kv).asInstanceOf[DfsVisitorMapped[K, V]]

  /**
   * Creates a new `Visitor` instance with the provided updated mapAppendables.
   *
   * This method is used to update the internal state of the Visitor by creating
   * a new instance with the modified mappings from `Message` to `Appendable`.
   *
   * @param map a map containing updated associations of `Message` to `Appendable[(K,V)]`
   * @return a new `DfsVisitorMapped[K, V]` instance that reflects the updated mapAppendables
   */
  def unit(map: Map[Message, Appendable[(K, V)]]): DfsVisitorMapped[K, V] =
    copy(map = map)
}

/**
 * Companion object for the `DfsVisitorMapped` class, incorporating utility methods to manage
 * depth-first traversal operations with a visitor-centric approach.
 */
object DfsVisitorMapped {

  /**
   * Performs recursive operations on a key-value pair with a depth-first search (DFS) visitor,
   * applying in-visit logic if the provided sequence of children contains at most two elements.
   * Depending on the contents of the sequence `xs`, it updates the visitor state accordingly.
   *
   * @param visitor the initial DFS visitor instance to be updated during the recursive process
   * @param k       the current key to be processed
   * @param v       the current associated value of the key
   * @param xs      a sequence of child keys to be processed; must contain at most two elements
   */
  private def recurseWithInVisit[K, V](visitor: DfsVisitorMapped[K, V], k: K, v: V, xs: Seq[K]) = {
    require(xs.length <= 2, "xs must contain at most two elements")
    val visitor1: DfsVisitorMapped[K, V] = xs.headOption.fold(visitor)(visitor.dfs)
    val visitor2: DfsVisitorMapped[K, V] = visitor1.visit(In)(k -> v)
    xs.lastOption.fold(visitor2)(visitor2.dfs)
  }
}