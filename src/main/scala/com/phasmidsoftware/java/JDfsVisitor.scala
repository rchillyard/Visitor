package com.phasmidsoftware.java

import com.phasmidsoftware.visitor
import com.phasmidsoftware.visitor.*
import com.phasmidsoftware.visitor.Message.fromJMessage

import scala.concurrent.duration.DurationConversions.fromNowConvert.R
import scala.jdk.CollectionConverters.{CollectionHasAsScala, IterableHasAsJava}

/**
 * A wrapper class for a `Visitor` instance, providing Java-compatible methods and functionalities.
 *
 * The `JVisitor` class bridges the gap between Scala's `Visitor` and Java clients by providing 
 * Java-friendly APIs while preserving the original functionality of the `Visitor`. It extends 
 * the `AutoCloseable` interface to support safe resource management.
 *
 * @param visitor the underlying Scala `Visitor` instance to be wrapped
 * @tparam X the type of the state or context associated with the `Visitor`
 */
class JDfsVisitor[X](val visitor: DfsVisitor[X]) extends AutoCloseable {

  /**
   * Executes a Depth-First Search (DFS) starting from the specified element `k`.
   * The traversal follows a recursive strategy, where each visited element may invoke
   * updates to the visiting state encapsulated in type `R`.
   *
   * @param k the starting element of type `K` for the DFS traversal
   * @return an updated instance of type `R` that reflects the visitor's state after the traversal
   */
  def dfs(k: X): JDfsVisitor[X] =
    new JDfsVisitor(visitor.dfs(k))

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
   * Retrieves the collection of `Appendable[X]` instances associated with this `AbstractVisitor`.
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
object JDfsVisitor {

  /**
   * Creates a new instance of `SimpleVisitor` with the specified `Message` and `Appendable`.
   *
   * @param message the `Message` instance that this visitor processes
   * @param journal an `Appendable` instance used to handle state updates and collect elements of type `X`
   * @return a newly created `SimpleVisitor` instance configured with the provided `Message` and `Appendable`
   */
  def createDfs[X](message: JMessage, journal: com.phasmidsoftware.visitor.Appendable[X], f: java.util.function.Function[X, java.util.List[X]]): JDfsVisitor[X] = {
    val g: X => Seq[X] = x => f.apply(x).asScala.toSeq
    new JDfsVisitor[X](com.phasmidsoftware.visitor.DfsVisitor.create(fromJMessage(message), journal, g))
  }

  /**
   * Creates a `SimpleVisitor` instance that utilizes the `Pre` message type
   * and an empty `QueueJournal` as its initial state.
   *
   * This method is particularly useful for scenarios where a `Visitor` is required
   * with a `Pre` message directive and a queue-based journaling mechanism.
   *
   * @tparam X the type of elements that the `SimpleVisitor` will operate on
   * @return an instance of `SimpleVisitor[X]` configured with the `Pre` message
   *         and an empty `QueueJournal`
   */
  def createPreQueue[X](f: java.util.function.Function[X, java.util.List[X]]): JDfsVisitor[X] =
    createDfs(JMessage.PRE, QueueJournal.empty[X], f)

  /**
   * Creates a `SimpleVisitor` instance configured with a `Post` message and an empty `QueueJournal`.
   *
   * This method is specifically designed to facilitate the creation of a visitor suited for handling
   * `Post` messages while maintaining a queue-based state through the `QueueJournal` instance.
   *
   * @tparam X the type of elements that the `SimpleVisitor` will operate on
   * @return a new instance of `SimpleVisitor[X]` initialized with the `Post` message and an empty `QueueJournal`
   */
  def createPostQueue[X](f: java.util.function.Function[X, java.util.List[X]]): JDfsVisitor[X] =
    createDfs(JMessage.POST, QueueJournal.empty[X], f)

  /**
   * Creates a `SimpleVisitor` instance with a `Pre` message and an empty `ListJournal`.
   *
   * This method constructs a `SimpleVisitor` configured with a predefined `Pre` message,
   * which typically indicates actions to be performed before other operations.
   * The associated journal is an empty `ListJournal`, capable of storing and appending
   * elements as the visitor processes messages.
   *
   * @tparam X the type of elements to be handled by the `SimpleVisitor`
   * @return a new instance of `SimpleVisitor` initialized with the `Pre` message
   *         and an empty `ListJournal`
   */
  def createPreStack[X](f: java.util.function.Function[X, java.util.List[X]]): JDfsVisitor[X] =
    createDfs(JMessage.PRE, ListJournal.empty[X], f)

  /**
   * Creates a `SimpleVisitor` instance initialized with the `Post` message and an empty `ListJournal`.
   *
   * This method allows for constructing a visitor configured to operate with the `Post` message
   * and a stack-like data structure (represented by the `ListJournal`).
   *
   * @tparam X the type of elements that the `SimpleVisitor` operates on
   * @return a new `SimpleVisitor[X]` instance with the `Post` message and an empty `ListJournal`
   */
  def createPostStack[X](f: java.util.function.Function[X, java.util.List[X]]): JDfsVisitor[X] =
    createDfs(JMessage.POST, ListJournal.empty[X], f)
}