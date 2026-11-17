package com.phasmidsoftware.visitor.java

// Imported by name rather than by wildcard. This file sits in package
// `com.phasmidsoftware.visitor.java`, whose own members take precedence over a wildcard import,
// so a wildcard on `core` would be a standing invitation for a future `core` type to be shadowed
// by a same-named member of this package. The façade's own types are deliberately named apart
// from the core ones (NeighbourFunction/Neighbours, Evaluator/Evaluable, Order/DfsOrder,
// Traversals/Traversal) so that no such clash exists today.
import com.phasmidsoftware.visitor.core.{DfsOrder, Evaluable, JournaledVisitor, Neighbours, QueueJournal, Traversal, Visitor, given}
// java.* is aliased because this package is itself called `java`: an unqualified `java.util.List`
// would be looked up as `com.phasmidsoftware.visitor.java.util.List` and not found.
import java.util.function.Predicate as JavaPredicate
import java.util.{List as JavaList, Map as JavaMap}
import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*

/**
  * Internal Scala bridge used by the Java façade.
  *
  * All members are reachable from Java as `JavaBridge$.MODULE$.<method>`. This object is
  * package-private to `com.phasmidsoftware.visitor.java`; [[Traversals]] is the public API.
  *
  * Its whole job is to turn the Java functional interfaces into the `given` instances the
  * traversal engine asks for, run the traversal, and convert the journal back into Java
  * collections. The engine itself is untouched.
  */
private[java] object JavaBridge:

  /** The journal type used throughout: a queue, so the journal reads in visit order. */
  private type QJ[V, R] = QueueJournal[(V, Option[R])]

  /**
    * Breadth-first traversal.
    *
    * @param start     the node to start from
    * @param nbrs      the adjacency
    * @param evaluator what to extract from each node
    * @param goal      early-termination predicate
    * @return the visited nodes, the results, and the came-from map
    */
  def bfs[V, R](
                 start: V,
                 nbrs: NeighbourFunction[V, V],
                 evaluator: Evaluator[V, R],
                 goal: JavaPredicate[V]
               ): TraversalResult[V, R] =
    given Neighbours[V, V] = asNeighbours(nbrs)
    given Evaluable[V, R] = asEvaluable(evaluator)
    materialise(Traversal.bfs(start, newVisitor[V, R], asGoal(goal)))

  /**
    * Depth-first traversal.
    *
    * @param start     the node to start from
    * @param nbrs      the adjacency
    * @param order     pre- or post-order recording
    * @param evaluator what to extract from each node
    * @param goal      early-termination predicate
    * @return the visited nodes, the results, and the came-from map
    */
  def dfs[V, R](
                 start: V,
                 nbrs: NeighbourFunction[V, V],
                 order: Order,
                 evaluator: Evaluator[V, R],
                 goal: JavaPredicate[V]
               ): TraversalResult[V, R] =
    given Neighbours[V, V] = asNeighbours(nbrs)
    given Evaluable[V, R] = asEvaluable(evaluator)
    materialise(Traversal.dfs(start, newVisitor[V, R], asDfsOrder(order), asGoal(goal)))

  /**
    * Depth-first traversal of a tree whose root may have a different type from its descendants.
    *
    * The engine does not record came-from for a tree, so the result's came-from map is empty.
    *
    * @param root      the root structure
    * @param rootNbrs  the children of the root
    * @param childNbrs the children of each node below the root
    * @param order     pre- or post-order recording
    * @param evaluator what to extract from each node
    * @param goal      early-termination predicate
    * @return the visited nodes and the results
    */
  def traverseTree[H, V, R](
                             root: H,
                             rootNbrs: NeighbourFunction[H, V],
                             childNbrs: NeighbourFunction[V, V],
                             order: Order,
                             evaluator: Evaluator[V, R],
                             goal: JavaPredicate[V]
                           ): TraversalResult[V, R] =
    given Neighbours[H, V] = asNeighbours(rootNbrs)
    given Neighbours[V, V] = asNeighbours(childNbrs)
    given Evaluable[V, R] = asEvaluable(evaluator)
    materialise(Traversal.traverseTree(root, newVisitor[V, R], asDfsOrder(order), asGoal(goal)))

  // ---------------------------------------------------------------------------
  // Conversions
  // ---------------------------------------------------------------------------

  /** A visitor with a queue journal (visit order) and came-from tracking enabled. */
  private def newVisitor[V, R]: JournaledVisitor[V, R, QJ[V, R]] =
    JournaledVisitor.withQueueJournalAndCameFrom[V, R]

  private def asNeighbours[H, V](f: NeighbourFunction[H, V]): Neighbours[H, V] =
    (h: H) => f.neighbours(h).asScala.iterator

  private def asEvaluable[V, R](f: Evaluator[V, R]): Evaluable[V, R] =
    (v: V) => f.evaluate(v).toScala

  private def asGoal[V](goal: JavaPredicate[V]): V => Boolean =
    (v: V) => goal.test(v)

  private def asDfsOrder(order: Order): DfsOrder =
    order match
      case Order.PRE => DfsOrder.Pre
      case Order.POST => DfsOrder.Post
      // Scala treats the Java enum as exhaustive, so null is the only case left — and a Java
      // caller can certainly pass one. Failing here beats silently choosing an order.
      case null => throw new IllegalArgumentException("JavaBridge: Order must not be null")

  /**
    * Reads the traversal's journal out into Java collections.
    *
    * The journal holds `(node, Option[result])` pairs in visit order; nodes whose evaluation was
    * empty contribute to `visited` but not to `results`.
    */
  private def materialise[V, R](out: Visitor[V, R, QJ[V, R]]): TraversalResult[V, R] =
    val entries = out.result.iterator.toList
    val visited: JavaList[V] = entries.map(_._1).asJava
    val results: JavaList[R] = entries.flatMap(_._2).asJava
    // `Traversal` returns the `Visitor` supertype, but every visitor it can derive from a
    // `JournaledVisitor` is itself one: `visit` and `discover` both return `copy(...)`. So this
    // cast cannot fail for a visitor made by `newVisitor`.
    val journaled = out.asInstanceOf[JournaledVisitor[V, R, QJ[V, R]]]
    val cameFrom: JavaMap[V, V] = journaled.cameFrom.getOrElse(Map.empty).asJava
    new TraversalResult(visited, results, cameFrom)
