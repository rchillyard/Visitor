package com.phasmidsoftware.visitor.javaAPI

import com.phasmidsoftware.visitor
import com.phasmidsoftware.visitor.*

import java.util.function.Predicate
import scala.jdk.CollectionConverters.{CollectionHasAsScala, IterableHasAsJava}
import scala.jdk.OptionConverters.RichOption

/**
 * Represents a bridge class that adapts a `BfsVisitor` to align with Java-compatible types.
 *
 * The `JBfsVisitor` class acts as a wrapper for a `BfsVisitor`, allowing for interaction
 * with Java constructs while retaining the breadth-first search traversal logic of the original
 * visitor. It provides methods for managing appendables, accessing iterable journal elements,
 * and handling resources through the implementation of the `AutoCloseable` interface.
 *
 * @tparam X the type of elements the visitor operates on during traversal
 */
class JBfsVisitor[X](val visitor: BfsVisitor[X]) extends AutoCloseable {

  /**
   * Performs a breadth-first search (BFS) starting from the given element `x`, updates the visitor state,
   * and constructs a `BfsResult` containing the updated visitor and an optional goal element.
   *
   * @param x the starting element for the BFS traversal
   * @return an instance of `BfsResult[X]` containing the updated visitor state and an optional goal element
   */
  def bfs(x: X): BfsResult[X] = {
    val tuple = visitor.bfs(x)
    new BfsResult[X](new javaAPI.JBfsVisitor(tuple._1), tuple._2.toJava)
  }

  /**
   * Closes the visitor and all associated resources.
   *
   * This method ensures that the visitor, along with any underlying appendable resources, is properly
   * finalized or cleaned up. Invoking this method indicates that the visitor is no longer active.
   *
   * @return Unit, as this method does not produce a result
   */
  def close(): Unit =
    visitor.close()

  /**
   * Retrieves the collection of `Appendable[X]` instances associated with this `JBfsVisitor`.
   *
   * The method provides access to all the appendable entities that the visitor interacts with.
   * This can be useful for iterating over, modifying, or closing the appendables as a group.
   *
   * @return an `Iterable` containing the appendable elements of type `Appendable[X]` associated with this visitor
   */
  def appendables(): java.lang.Iterable[com.phasmidsoftware.visitor.Appendable[X]] =
    visitor.appendables.asJava

  /**
   * Retrieves an iterable collection of all `Journal[X]` instances from this `Visitor` that are Iterable.
   *
   * This method filters the iterable collection of `Appendable[X]` instances, returning only those
   * that are of type `Journal[X]`. It performs a type check on each `Appendable[X]` and selectively
   * includes those that match the `Journal[X]` type.
   *
   * @return an `Iterable` containing all `Journal[X]` instances managed by this `Visitor`
   */
  def iterableJournals: java.lang.Iterable[IterableJournal[X]] =
    visitor.iterableJournals.asJava

  /**
   * Retrieves the first `IterableJournal[X]` instance from the collection of iterable journals
   * managed by this visitor.
   *
   * This method accesses the `iterableJournals` collection and returns its first element. It is
   * intended to retrieve a specific journal that can both store entries (`Journal[X]`) and provide
   * iterable functionality (`Iterable[X]`).
   *
   * @return the first `IterableJournal[X]` instance from the iterable journals collection
   */
  def iterableJournal: IterableJournal[X] =
    visitor.iterableJournals.head
}

/**
 * Companion object for `JVisitor`, providing factory methods to create
 * instances of `SimpleVisitor` with various configurations.
 */
object JBfsVisitor {

  /**
   * Creates an instance of `JBfsVisitor` configured to perform a breadth-first search (BFS) traversal
   * using the specified journal, traversal function, and goal predicate.
   *
   * @param message an instance of `JMessage` indicating the type of message (e.g., PRE or POST) for the traversal.
   * @param journal an `Appendable[X]` where the traversal will log its visited elements.
   * @param f       a Java function that maps an element of type `X` to a list of its child elements for traversal.
   * @param goal    a predicate to determine if a given element satisfies the search goal.
   * @return a new `JBfsVisitor[X]` instance configured for BFS traversal.
   */
  def createBfs[X](message: JMessage, journal: com.phasmidsoftware.visitor.Appendable[X], f: java.util.function.Function[X, java.util.List[X]], goal: Predicate[X]): JBfsVisitor[X] = {
    val g: X => Seq[X] = x => f.apply(x).asScala.toSeq
    val h: X => Boolean = x => goal.test(x)
    new JBfsVisitor[X](com.phasmidsoftware.visitor.BfsVisitor.create(journal, g, h))
  }

  /**
   * Creates a `JBfsVisitor` instance configured for pre-order traversal using a queue-based journal.
   *
   * The method initializes a breadth-first search visitor (`BfsVisitor`) with the specified function and goal predicate,
   * while utilizing a pre-order traversal strategy and an empty `QueueJournal` for managing traversal state.
   *
   * @param f    a Java function mapping an element of type `X` to a list of its neighbors
   * @param goal a predicate specifying the goal condition, which determines when the traversal should stop
   * @return a `JBfsVisitor[X]` instance supporting pre-order traversal using a queue for managing elements
   */
  def createPreQueue[X](f: java.util.function.Function[X, java.util.List[X]], goal: Predicate[X]): JBfsVisitor[X] =
    createBfs(JMessage.PRE, QueueJournal.empty[X], f, goal)

  /**
   * Creates a `JBfsVisitor` instance configured for a post-order traversal using a queue for journaling.
   *
   * @param f    a Java function `Function[X, java.util.List[X]]` that maps an element of type `X` to a list of its neighbors
   * @param goal a `Predicate[X]` that defines the termination condition for the traversal
   * @return a `JBfsVisitor[X]` configured for post-order traversal with an empty `QueueJournal`
   */
  def createPostQueue[X](f: java.util.function.Function[X, java.util.List[X]], goal: Predicate[X]): JBfsVisitor[X] =
    createBfs(JMessage.POST, QueueJournal.empty[X], f, goal)

  /**
   * Constructs a `JBfsVisitor` using a breadth-first search configuration, where the order of traversal
   * is set to "PRE" and a `ListJournal` is used to manage visited elements.
   *
   * @param f    a Java function that, given an element, generates a list of neighboring elements to traverse
   * @param goal a predicate that defines the goal condition for the BFS traversal, which determines when to stop searching
   * @return a JBfsVisitor configured with a "PRE" traversal message and an empty `ListJournal`
   */
  def createPreStack[X](f: java.util.function.Function[X, java.util.List[X]], goal: Predicate[X]): JBfsVisitor[X] =
    createBfs(JMessage.PRE, ListJournal.empty[X], f, goal)

  /**
   * Creates a `JBfsVisitor` instance that performs a post-order traversal using a stack-based approach.
   *
   * This method utilizes the `ListJournal` for internal state tracking, a function to determine
   * possible transitions from each node, and a goal predicate that defines the desired condition
   * to terminate the search.
   *
   * @param f    a function that maps an element of type `X` to a `java.util.List[X]`, representing transitions or neighbors
   * @param goal a predicate that determines whether a given element of type `X` satisfies the termination condition
   * @return a `JBfsVisitor[X]` configured to perform a post-order traversal with a stack-based journal
   */
  def createPostStack[X](f: java.util.function.Function[X, java.util.List[X]], goal: Predicate[X]): JBfsVisitor[X] =
    createBfs(JMessage.POST, ListJournal.empty[X], f, goal)
}