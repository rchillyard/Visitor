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
trait Visitor[X] {

  /**
   * Make a visit, with the given message and `X` value, on this `Visitor` and return a new `Visitor`..
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
 * Abstract implementation of the `Visitor` trait, designed to work with multiple `Appendable` objects
 * mapped to specific `Message` instances.
 *
 * This class provides a foundation for creating visitors that manage state by associating
 * `Message` types with corresponding `Appendable` objects and defining behavior for processing
 * messages through the visitor pattern.
 *
 * @tparam X the type of the state or context that the visitor operates on.
 * @param appendables a map associating `Message` instances with their corresponding `Appendable` objects.
 *                    This map defines the initial state of the visitor.
 */
abstract class AbstractMultiVisitor[X](appendables: Map[Message, Appendable[X]]) extends Visitor[X] {

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
  def appendable(message: Message): Option[Appendable[X]] = appendables.get(message)

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
  def addAppendable(msg: Message, appendable: Appendable[X]): Visitor[X] =
    unit(appendables + (msg -> appendable))

  /**
   * Make a visit, with the given message and `X` value, on this `Visitor` and return a new `Visitor`..
   *
   * This method defines the behavior for handling a `Message` in the context
   * of the Visitor pattern. The implementation of this method should use the provided
   * message and state to determine the next state and return the appropriate `Visitor`.
   *
   * @param msg the message to be processed by the visitor
   * @param x   the current state or context associated with the visitor
   * @return a new `Visitor[X]` instance that represents the updated state after processing the message
   */
  def visit(msg: Message)(x: X): Visitor[X] = appendables.get(msg) match {
    case Some(appendable) =>
      val updatedAppendable: Appendable[X] = appendable.append(x)
      val kv: (Message, Appendable[X]) = msg -> updatedAppendable
      unit(appendables + kv)
    case None =>
      this
  }

  /**
   * Creates a new `Visitor` instance with the provided updated appendables.
   *
   * This method is used to update the internal state of the Visitor by creating
   * a new instance with the modified mappings from `Message` to `Appendable`.
   *
   * @param updatedAppendables a map containing updated associations of `Message` to `Appendable[X]`
   * @return a new `Visitor[X]` instance that reflects the updated appendables
   */
  def unit(updatedAppendables: Map[Message, Appendable[X]]): Visitor[X]
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
 * @param appendables a mapping of `Message` to corresponding `Appendable[X]` instances
 */
case class MultiVisitor[X](appendables: Map[Message, Appendable[X]]) extends AbstractMultiVisitor[X](appendables) {

  /**
   * Creates a new `Visitor` instance with the specified updated appendables.
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
   * Make a visit, with the given message and `X` value, on this `Visitor` and return a new `Visitor`..
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