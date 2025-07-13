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

  def unit: Visitor[X] = this
}
