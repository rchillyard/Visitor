package com.phasmidsoftware.visitor

/**
 * A case class implementation of the `Appendable` trait which executes a provided function
 * when an element is appended.
 *
 * The `NonAppendable` class takes a function `f` as a parameter, which is invoked
 * with the appended element whenever the `append` method is called. This implementation
 * is a simple wrapper that enables side effects or custom handling of appended elements
 * without retaining any internal state.
 *
 * @param f a function to be executed with each appended element
 * @tparam X the type of elements that can be appended to this `Appendable`
 */
case class NonAppendable[X](f: X => Unit) extends Appendable[X] {

  /**
   * "Appends" the specified element to this `Appendable` object, returning a new instance
   * of the `Appendable` with the element included.
   * In reality, nothing is appended.
   * However, there is a side effect in that the function `f` is invoked on the given value of `x`.
   *
   * @param x the element to be "appended"
   * @return a new `Appendable[X]` instance containing the existing elements and the newly appended element
   */
  def append(x: X): Appendable[X] = {
    f(x)
    this
  }

  def close(): Unit = {}
}
