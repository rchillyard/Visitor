/*
 * Copyright (c) 2026. Phasmid Software
 */

package com.phasmidsoftware.visitor.core

import java.io.PrintStream

/**
  * Typeclass for algorithm tracing.  Implementations produce human-readable output
  * showing the steps of a graph algorithm at a given nesting level; the level is
  * used to indent output so that the logical structure of the algorithm is visible.
  *
  * The canonical no-op `given` (`Tracer.silent`) is resolved automatically, so
  * existing call sites require no changes.  Students opt in by summoning a verbose
  * tracer:
  *
  * {{{
  *   given Tracer[Int] = Tracer.verbose()
  *   Kosaraju.components(graph)
  * }}}
  *
  * The `msg` parameter is by-name so that string interpolation is never evaluated
  * in the silent case — zero overhead in production use.
  *
  * @tparam V the vertex type (present so that future implementations can, for
  *           example, distinguish a vertex tracer from an edge tracer via the
  *           type parameter)
  */
trait Tracer[V]:
  /**
    * Emit a trace message at the given nesting level.
    *
    * @param level nesting depth (0 = top-level algorithm entry, 1 = outer loop
    *              body, 2 = inner loop / recursive call, etc.)
    *
    * @param msg   the message to emit, evaluated lazily
    */
  def trace(level: Int, msg: => String): Unit

object Tracer:

  /**
    * The silent (no-op) tracer.  Resolved automatically as the default `given`
    * so that all algorithms compile and run without any explicit tracer in scope.
    */
  given silent[V]: Tracer[V] with
    def trace(level: Int, msg: => String): Unit = ()

  /**
    * A verbose tracer that indents output by two spaces per level.
    *
    * @param maxLevel only messages at or below this level are emitted; defaults
    *                 to `Int.MaxValue` (emit everything).  Useful for students
    *                 who want to understand the outer loop before diving into
    *                 deeper detail.
    *
    * @param out      the stream to write to; defaults to `System.out`.
    *                 Pass a custom `PrintStream` in tests to capture output
    *                 without touching global state.
    *
    * @tparam V the vertex type
    */
  def verbose[V](maxLevel: Int = Int.MaxValue, out: PrintStream = System.out): Tracer[V] = new Tracer[V]:
    def trace(level: Int, msg: => String): Unit =
      if level <= maxLevel then out.println("  " * level + msg)

  /**
    * A summary tracer that emits only top-level (level 0) messages.
    * Equivalent to `verbose(maxLevel = 0)`.
    *
    * Useful for a first look: students see only the major phases of the
    * algorithm without the per-vertex noise.
    *
    * @param out the stream to write to; defaults to `System.out`.
    */
  def summary[V](out: PrintStream = System.out): Tracer[V] = verbose[V](maxLevel = 0, out = out)