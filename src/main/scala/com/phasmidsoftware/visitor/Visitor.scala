package com.phasmidsoftware.visitor

/**
 * Represents a generic `Visitor` as part of the Visitor pattern.
 *
 * This trait defines methods for processing messages and maintaining the visitor's state.
 * It uses a generic type parameter `X` to represent the state or context associated
 * with the visitor during its operation.
 *
 * Subclasses or implementations of this trait should define behavior for specific
 * message types through the `visit` method, enabling extension of processing logic.
 *
 * @tparam X the type of the state or context that the visitor operates on.
 */
trait Visitor[X] extends HasAppendables[X] with AutoCloseable {

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
  def visit(msg: Message)(x: X): Visitor[X]
}

/**
 * Represents an abstract implementation of a `Visitor` which interacts with a collection
 * of `Appendable` objects.
 *
 * This trait defines a general structure for a visitor that operates on multiple appendable
 * entities, providing functionality to close all associated appendables collectively.
 *
 * @tparam X the type of element or context that the visitor and its associated appendables handle
 */
trait AbstractVisitor[X] extends Visitor[X] {
  /**
   * Represents the state of openness for the visitor or associated entities.
   *
   * NOTE this is a var!
   *
   * This variable indicates whether the visitor or its managed appendable resources
   * are currently open and operational. Typically, the variable starts as `true`
   * and is set to `false` when the `close` method is invoked, signaling that the
   * operations have been finalized and the resources are no longer active.
   */
  protected var open: Boolean = true

  /**
   * Closes all associated `Appendable` instances and marks this visitor as no longer open.
   *
   * This method iterates over each appendable in the collection and invokes their `close` method,
   * ensuring that necessary cleanup or finalization operations are performed on each appendable.
   * After all appendables are closed, the `open` flag is set to `false`.
   *
   * @return Unit (no specific value is returned)
   */
  def close(): Unit = {
    appendables foreach (_.close())
    open = false
  }
}

/**
 * A concrete implementation of the `Visitor` trait designed to operate with an `Appendable`
 * instance and a specific message. This class facilitates state updates based on message matching
 * and appends values of type `X` to the `Appendable`.
 *
 * CONSIDER implementing this in terms of MultiVisitor
 *
 * @constructor Creates a new `SimpleVisitor` with the specified `Appendable` and `Message`.
 * @param appendable an `Appendable` instance representing the current state,
 *                   which can be updated with new values
 * @param msg        a `Message` instance that this visitor matches against during the `visit` operation
 * @tparam X the type of elements that the `SimpleVisitor` operates on
 */
case class SimpleVisitor[X](msg: Message, appendable: Appendable[X]) extends AbstractVisitor[X] {

  /**
   * Processes a `Message` with a given value of type `X` and returns a new `Visitor`
   * that reflects the updated state based on the provided message and value.
   *
   * If the provided `msg` matches the `msg` of this visitor, a new visitor is created
   * with the value `x` appended to its `Appendable`. Otherwise, this visitor is returned unchanged.
   *
   * @param msg the message to be processed by this visitor
   * @param x   the value of type `X` to be added to the visitor's state if the message matches
   * @return a new `Visitor[X]` instance representing the updated state if the message matches,
   *         or the current visitor if the message does not match
   */
  def visit(msg: Message)(x: X): SimpleVisitor[X] =
    if (open)
      if (msg == this.msg)
        copy(appendable = appendable.append(x))
      else
        this
    else
      throw new UnsupportedOperationException(s"Visitor $this is closed")

  /**
   * Retrieves a collection of `Appendable` instances associated with this visitor.
   *
   * @return an `Iterable` containing the `Appendable` instances, typically representing
   *         the current state or outputs that can be updated or extended.
   */
  def appendables: Iterable[Appendable[X]] = Seq(appendable)
}

/**
 * A utility object for creating and managing instances of the `SimpleVisitor` class.
 *
 * The `SimpleVisitor` object provides factory methods to conveniently initialize
 * visitors with various configurations of `Message` and `Appendable` instances.
 * These methods support common use cases of the `SimpleVisitor` class, enabling
 * straightforward creation of pre-configured visitors for different processing requirements.
 */
object SimpleVisitor {

  /**
   * Creates an instance of `SimpleVisitor` using the provided `Message` and an empty `QueueJournal`.
   *
   * @param message the `Message` to be processed by the `SimpleVisitor`
   * @tparam X the type of elements that the `SimpleVisitor` will operate on
   * @return a new instance of `SimpleVisitor[X]` configured with the specified `Message` and an empty `QueueJournal`
   */
  def apply[X](message: Message): SimpleVisitor[X] =
    create(message, QueueJournal.empty[X])

  /**
   * Creates a new instance of `SimpleVisitor` with the specified `Message` and `Appendable`.
   *
   * @param message the `Message` instance that this visitor processes
   * @param journal an `Appendable` instance used to handle state updates and collect elements of type `X`
   * @return a newly created `SimpleVisitor` instance configured with the provided `Message` and `Appendable`
   */
  def create[X](message: Message, journal: Appendable[X]): SimpleVisitor[X] =
    new SimpleVisitor(message, journal)

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
  def createPreQueue[X]: SimpleVisitor[X] =
    create(Pre, QueueJournal.empty[X])

  /**
   * Creates a `SimpleVisitor` instance configured with a `Post` message and an empty `QueueJournal`.
   *
   * This method is specifically designed to facilitate the creation of a visitor suited for handling
   * `Post` messages while maintaining a queue-based state through the `QueueJournal` instance.
   *
   * @tparam X the type of elements that the `SimpleVisitor` will operate on
   * @return a new instance of `SimpleVisitor[X]` initialized with the `Post` message and an empty `QueueJournal`
   */
  def createPostQueue[X]: SimpleVisitor[X] =
    create(Post, QueueJournal.empty[X])

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
  def createPreStack[X]: SimpleVisitor[X] =
    create(Pre, ListJournal.empty[X])

  /**
   * Creates a `SimpleVisitor` instance initialized with the `Post` message and an empty `ListJournal`.
   *
   * This method allows for constructing a visitor configured to operate with the `Post` message
   * and a stack-like data structure (represented by the `ListJournal`).
   *
   * @tparam X the type of elements that the `SimpleVisitor` operates on
   * @return a new `SimpleVisitor[X]` instance with the `Post` message and an empty `ListJournal`
   */
  def createPostStack[X]: SimpleVisitor[X] =
    create(Post, ListJournal.empty[X])
}

/**
 * Abstract implementation of the `Visitor` trait, designed to work with multiple `Appendable` objects
 * mapped to specific `Message` instances.
 *
 * This class provides a foundation for creating visitors that manage state by associating
 * `Message` types with corresponding `Appendable` objects and defining behavior for processing
 * messages through the visitor pattern.
 *
 * @tparam X the type of the state or context that the visitor operates on.
 * @param mapAppendables a map associating `Message` instances with their corresponding `Appendable` objects.
 *                       This map defines the initial state of the visitor.
 */
abstract class AbstractMultiVisitor[X]
(val mapAppendables: Map[Message, Appendable[X]]) extends
  AbstractVisitor[X] {

  /**
   * Retrieves an optional `Appendable[X]` associated with the specified `Message`.
   *
   * This method looks up the internal mapping of `Message` to `Appendable[X]` and returns
   * the associated `Appendable[X]` if it exists. If no association exists for the given
   * `Message`, `None` is returned.
   *
   * @param message the `Message` for which to retrieve the associated `Appendable[X]`
   * @return an `Option[Appendable[X]]` containing the associated `Appendable[X]` if it exists, or `None` if there is no association
   */
  def appendable(message: Message): Option[Appendable[X]] = mapAppendables.get(message)

  /**
   * Adds an `Appendable` associated with a specified `Message` to the `Visitor`.
   *
   * This method allows the `Visitor` to handle specific messages by associating them
   * with their corresponding `Appendable`. The updated `Visitor` will use this association
   * to process the given message and manage the visitor's state.
   *
   * @param msg        the message to associate with the provided `Appendable`.
   * @param appendable the `Appendable` instance to associate with the specified message.
   * @return a new `Visitor[X]` instance that includes the updated association between the message and the `Appendable`.
   */
  def addAppendable(msg: Message, appendable: Appendable[X]): AbstractMultiVisitor[X] =
    unit(mapAppendables + (msg -> appendable)).asInstanceOf[AbstractMultiVisitor[X]]

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
  def visit(msg: Message)(x: X): AbstractMultiVisitor[X] = (open, mapAppendables.get(msg)) match {
    case (false, _) => throw new UnsupportedOperationException(s"Visitor $this is closed")
    case (_, Some(appendable)) =>
      val updatedAppendable: Appendable[X] = appendable.append(x)
      val kv: (Message, Appendable[X]) = msg -> updatedAppendable
      unit(mapAppendables + kv).asInstanceOf[AbstractMultiVisitor[X]]
    case (_, None) =>
      this
  }

  /**
   * Creates a new `Visitor` instance with the provided updated mapAppendables.
   *
   * This method is used to update the internal state of the Visitor by creating
   * a new instance with the modified mappings from `Message` to `Appendable`.
   *
   * @param updatedAppendables a map containing updated associations of `Message` to `Appendable[X]`
   * @return a new `Visitor[X]` instance that reflects the updated mapAppendables
   */
  def unit(updatedAppendables: Map[Message, Appendable[X]]): Visitor[X]

  /**
   * Retrieves an iterable collection of all `Appendable[X]` instances managed by the `AbstractMultiVisitor`.
   *
   * This method provides access to the `Appendable[X]` objects currently stored within the internal
   * map of the `AbstractMultiVisitor`. The returned iterable contains all the values from the
   * mapping without exposing the keys or the underlying map structure.
   *
   * @return an `Iterable` containing all `Appendable[X]` instances managed by the `AbstractMultiVisitor`
   */
  def appendables: Iterable[Appendable[X]] = mapAppendables.values
}

/**
 * Abstract base class for a visitor that maps `Message` instances to `Appendable` objects
 * containing key-value pairs of type `(K, V)`.
 *
 * This class extends `AbstractMultiVisitor`, leveraging its capabilities to manage multiple
 * `Appendable` objects while introducing specific functionality for handling journals of
 * mapped key-value pairs. It provides a method to retrieve all journal entries that are
 * of type `AbstractMapJournal[K, V]`.
 *
 * @tparam K the type of the key stored in the journal
 * @tparam V the type of the value stored in the journal
 * @param map a map associating `Message` instances with their corresponding `Appendable` objects
 *            containing elements of type `(K, V)`. This defines the initial state of the visitor.
 */
abstract class AbstractVisitorMapped[K, V]
(map: Map[Message, Appendable[(K, V)]]) extends
  AbstractMultiVisitor[(K, V)](map) {

  /**
   * Retrieves an iterable collection of all `Journal[X]` instances from this `Visitor`.
   *
   * This method filters the iterable collection of `Appendable[X]` instances, returning only those
   * that are of type `Journal[X]`. It performs a type check on each `Appendable[X]` and selectively
   * includes those that match the `Journal[X]` type.
   *
   * @return an `Iterable` containing all `Journal[X]` instances managed by this `Visitor`
   */
  def mapJournals: Iterable[AbstractMapJournal[K, V]] =
    for {
      appendable <- appendables
      xjo: Option[AbstractMapJournal[K, V]] = appendable match {
        case x: AbstractMapJournal[K, V] =>
          Some(x)
        case _ =>
          None
      }
      journal <- xjo
    } yield journal
}

/**
 * Abstract class that extends `AbstractVisitorMapped` and provides additional functionality
 * for mapping keys to values with children elements.
 *
 * This class enhances the base functionality of `AbstractVisitorMapped` by allowing 
 * the mapping of keys of type `K` to values of type `V` while also establishing a relationship 
 * with children elements, where children are derived from a key of type `K`.
 *
 * Typical use cases for this class include scenarios where hierarchical data needs to be processed 
 * or traversed, and the mapping of each key can generate a corresponding value along with potentially 
 * associated child elements.
 *
 * @tparam K the type of the key
 * @tparam C the type of children associated with a key
 * @tparam V the type of the value derived from the key
 * @param map      a `Map` associating `Message` instances with their corresponding 
 *                 `Appendable` objects containing key-value pairs having type `(K, V)`
 * @param fulfill  a function that maps a key of type `K` to a corresponding value of type `V`
 * @param children a function that generates a sequence of children of type `C` for a given key of type `K`
 */
abstract class AbstractVisitorMappedWithChildren[K, C, V]
(map: Map[Message, Appendable[(K, V)]], fulfill: Option[C] => K => V, children: K => Seq[C]) extends
  AbstractVisitorMapped[K, V](map) {
  /**
   * Creates a key-value pair by applying the function `fulfill` to a given key of type `K`.
   *
   * @param k the key of type `K` from which the pair is derived
   * @return a tuple `(K, V)` where the first element is the key `k` and the second element is the corresponding value obtained by applying the function `fulfill` to `k`
   */
  def keyValuePair(k: K): (K, V) = k -> fulfill(None)(k)
}

/**
 * A concrete implementation of `AbstractMultiVisitor` that represents a visitor
 * capable of handling multiple `Message` to `Appendable` mappings.
 *
 * `MultiVisitor` provides functionality to process `Message` objects using associated
 * `Appendable` instances. It extends the behavior of `AbstractMultiVisitor` by
 * enabling the combination of multiple appendable elements while maintaining immutability.
 *
 * @tparam X the type of elements processed by the visitor and stored in the associated `Appendable` instances
 * @param map a mapping of `Message` to corresponding `Appendable[X]` instances
 */
case class MultiVisitor[X](map: Map[Message, Appendable[X]]) extends AbstractMultiVisitor[X](map) {

  /**
   * Creates a new `Visitor` instance with the specified updated mapAppendables.
   *
   * This method is used to generate a new `Visitor` instance, encapsulating the state
   * provided by the updated mapping of `Message` to `Appendable[X]`. It enables the
   * replacement or update of the current `Appendable` mappings while maintaining
   * the immutable nature of the visitor.
   *
   * @param updatedAppendables a map associating `Message` instances with `Appendable[X]` instances
   *                           to represent the updated state of appendable elements.
   * @return a new `Visitor[X]` instance that reflects the updated state.
   */
  def unit(updatedAppendables: Map[Message, Appendable[X]]): Visitor[X] = MultiVisitor(updatedAppendables)

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
  override def visit(msg: Message)(x: X): MultiVisitor[X] = super.visit(msg)(x).asInstanceOf[MultiVisitor[X]]
}

/**
 * Companion object for the `MultiVisitor` case class, providing utility methods
 * to facilitate the creation of `MultiVisitor` instances.
 */
object MultiVisitor {

  /**
   * Creates a `MultiVisitor` instance with the specified sequence of `Message` to `Appendable` mappings.
   *
   * @param appendables a variable argument list of tuples where each tuple consists of:
   *                    - a `Message` that serves as the key for processing
   *                    - an `Appendable[X]` as the value associated with the key
   * @tparam X the type of elements stored in the `Appendable` instances
   * @return a `MultiVisitor[X]` initialized with the provided message-appendable mappings
   */
  def apply[X](appendables: (Message, Appendable[X])*): MultiVisitor[X] =
    MultiVisitor(Map(appendables: _*))
}

/**
 * A case class extending `AbstractVisitorMapped` for managing `Message` to `Appendable[(K, V)]` mappings
 * within the context of the Visitor pattern.
 *
 * TESTME this is not used currently.
 *
 * This class provides additional methods for updating and processing the internal mappings, allowing
 * it to serve as a mutable visitor that accommodates changes in state as it processes messages.
 *
 * @tparam K the type of the key stored in the journal
 * @tparam V the type of the value stored in the journal
 * @param map a map associating `Message` instances with their corresponding `Appendable` objects
 *            containing elements of type `(K, V)`. This defines the initial state of the visitor.
 */
case class VisitorMapped[K, V](map: Map[Message, Appendable[(K, V)]]) extends AbstractVisitorMapped[K, V](map) {
  /**
   * Creates a new `Visitor` instance with the provided updated mapAppendables.
   *
   * This method is used to update the internal state of the Visitor by creating
   * a new instance with the modified mappings from `Message` to `Appendable`.
   *
   * @param updatedAppendables a map containing updated associations of `Message` to `Appendable[X]`
   * @return a new `Visitor[X]` instance that reflects the updated mapAppendables
   */
  def unit(updatedAppendables: Map[Message, Appendable[(K, V)]]): VisitorMapped[K, V] =
    copy(map = updatedAppendables)

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
  override def visit(msg: Message)(x: (K, V)): VisitorMapped[K, V] =
    super.visit(msg)(x).asInstanceOf[VisitorMapped[K, V]]
}
