package com.phasmidsoftware.visitor

import scala.Option.when

/**
 * Provides utility functions for functional programming.
 */
object FP {

  /**
   * Executes the provided block of code conditionally based on the given boolean predicate.
   * If the predicate is true, the block of code is executed and its result is returned.
   * If the predicate is false, `None` is returned.
   * `whenever` is to `when` as `flatMap` is to `map`.
   *
   * @param p the boolean predicate that determines whether the block of code should be executed
   * @param x a by-name parameter representing the block of code returning an Option of type X
   * @return an Option containing the result of the block of code if `p` is true, or None if `p` is false
   */
  def whenever[X](p: Boolean)(x: => Option[X]): Option[X] =
    when(p)(x).flatten

}
