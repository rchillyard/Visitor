package com.phasmidsoftware.visitor

/**
 * Represents a generic message that can be used in conjunction with the Visitor pattern.
 *
 * This trait serves as a marker type and is intended to be extended or instantiated
 * by specific message types for processing by a Visitor implementation. Objects like `Pre`, In,
 * `Post`, and `SelfVisit` are examples of concrete implementations of this trait.
 *
 * It is primarily used for defining behavior in methods such as `visit` in the `Visitor` pattern,
 * where actions are performed based on the type or identity of the provided message.
 */
sealed trait Message

/**
 * `Pre` is an object that extends the `Message` trait.
 *
 * It is a specific type of `Message` and can be used in conjunction with the Visitor pattern.
 * When utilized in a Visitor implementation, it might represent a directive or action
 * to be performed prior to other operations, as its name suggests.
 *
 * Typically used as a marker or control structure, `Pre` serves as a meaningful, predefined
 * instance within the overall context of message handling in the Visitor pattern.
 */
object Pre extends Message

/**
 * `Post` is an object that extends the `Message` trait.
 *
 * It represents a specific type of `Message` that can be utilized in the Visitor pattern.
 * As its name suggests, `Post` is typically interpreted as a marker or directive that
 * signifies an action or state occurring after a particular operation or process.
 *
 * When used with a `Visitor`, `Post` can serve as a meaningful message for controlling
 * or structuring behavior based on its identity or context within the message processing flow.
 *
 * It is intended to be a concrete, immutable instance in the collection of predefined `Message` types.
 */
object Post extends Message

/**
 * Represents a specific type of `Message` used in conjunction with the Visitor pattern.
 *
 * `In`-visitation occurs ONLY if an object has exactly two children (a binary tree, for instance).
 */
object In extends Message

/**
 * `SelfVisit` is an object that extends the `Message` trait.
 *
 * It represents a specific type of `Message` within the context of the Visitor pattern.
 * `SelfVisit` serves as a predefined, immutable instance that may be used to signify
 * the absence of action or a neutral state in message processing.
 *
 * As part of the `Message` hierarchy, it can be passed to a `Visitor` implementation,
 * with behavior or interpretation determined by the specific logic of the `visit` method.
 * This object provides a way to encode a semantic placeholder or non-operation
 * within the message processing flow.
 */
object SelfVisit extends Message

object Message {
  def fromJMessage(msg: JMessage): Message = msg match {
    case JMessage.PRE => Pre
    case JMessage.POST => Post
    case JMessage.IN => In
  }
}