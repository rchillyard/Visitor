package com.phasmidsoftware.visitor.javaAPI

import com.phasmidsoftware.visitor
import com.phasmidsoftware.visitor.*
import com.phasmidsoftware.visitor.Message.fromJMessage

import scala.jdk.CollectionConverters.IterableHasAsJava

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
class JVisitor[X](val visitor: Visitor[X]) extends AutoCloseable {

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
  def visit(msg: JMessage, x: X): JVisitor[X] =
    new JVisitor(visitor.visit(Message.fromJMessage(msg))(x))

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
object JVisitor {

  /**
   * Creates a new instance of `SimpleVisitor` with the specified `Message` and `Appendable`.
   *
   * @param message the `Message` instance that this visitor processes
   * @param journal an `Appendable` instance used to handle state updates and collect elements of type `X`
   * @return a newly created `SimpleVisitor` instance configured with the provided `Message` and `Appendable`
   */
  def createSimple[X](message: JMessage, journal: com.phasmidsoftware.visitor.Appendable[X]): JVisitor[X] =
    new JVisitor[X](com.phasmidsoftware.visitor.SimpleVisitor.create(fromJMessage(message), journal))

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
  def createPreQueue[X]: JVisitor[X] =
    createSimple(JMessage.PRE, QueueJournal.empty[X])

  /**
   * Creates a `SimpleVisitor` instance configured with a `Post` message and an empty `QueueJournal`.
   *
   * This method is specifically designed to facilitate the creation of a visitor suited for handling
   * `Post` messages while maintaining a queue-based state through the `QueueJournal` instance.
   *
   * @tparam X the type of elements that the `SimpleVisitor` will operate on
   * @return a new instance of `SimpleVisitor[X]` initialized with the `Post` message and an empty `QueueJournal`
   */
  def createPostQueue[X]: JVisitor[X] =
    createSimple(JMessage.POST, QueueJournal.empty[X])

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
  def createPreStack[X]: JVisitor[X] =
    createSimple(JMessage.PRE, ListJournal.empty[X])

  /**
   * Creates a `SimpleVisitor` instance initialized with the `Post` message and an empty `ListJournal`.
   *
   * This method allows for constructing a visitor configured to operate with the `Post` message
   * and a stack-like data structure (represented by the `ListJournal`).
   *
   * @tparam X the type of elements that the `SimpleVisitor` operates on
   * @return a new `SimpleVisitor[X]` instance with the `Post` message and an empty `ListJournal`
   */
  def createPostStack[X]: JVisitor[X] =
    createSimple(JMessage.POST, ListJournal.empty[X])

  /**
   * Converts a Scala `Iterable` to a Java: `java.util.List`.
   *
   * @param xs the Scala `Iterable` to be converted
   * @return a Java: `java.util.List` containing the elements of the provided `Iterable`
   */
  def toJava[X](xs: Iterable[X]): java.util.Collection[X] = xs.asJavaCollection
}