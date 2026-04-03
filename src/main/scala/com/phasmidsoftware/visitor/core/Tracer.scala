/*
 * Copyright (c) 2026. Phasmid Software
 */

package com.phasmidsoftware.visitor.core

import java.io.PrintStream

/**
  * Typeclass for algorithm tracing.  Implementations produce human-readable output
  * showing the steps of a graph algorithm at a given nesting depth; the depth is
  * used to indent output so that the logical structure of the algorithm is visible.
  *
  * Each trace call provides two lazily-evaluated messages:
  *  - `brief`  — a short summary (e.g. a count); always used by non-silent tracers
  *  - `detail` — a fuller description (e.g. collection elements); used only by `verbose`
  *
  * The canonical no-op `given` (`Tracer.silent`) is resolved automatically, so
  * existing call sites require no changes.  Students opt in by summoning a tracer:
  *
  * {{{
  *   given Tracer[Int] = Tracer.verbose()
  *   Kosaraju.stronglyConnectedComponents(graph)
  * }}}
  *
  * Both `brief` and `detail` are by-name so that string interpolation is never
  * evaluated in the silent case — zero overhead in production use.
  *
  * Three built-in modes:
  *  - `silent`  — no output, neither `brief` nor `detail` evaluated
  *  - `summary` — emits `brief` only, and only at depth 0
  *  - `verbose` — emits `detail` (falling back to `brief` if `detail` is empty),
  *    up to a configurable `maxDepth`
  *
  * @tparam V the vertex type (present so that, for example, a vertex tracer and
  *           an edge tracer can coexist in scope via their distinct type parameters)
  */
trait Tracer[V]:
  /**
    * Emit a trace message at the given nesting depth.
    *
    * @param depth  nesting depth (0 = top-level algorithm entry, 1 = outer loop
    *               body, 2 = inner loop / recursive call, etc.)
    *
    * @param brief  a short summary message, evaluated lazily
    * @param detail a fuller description, evaluated lazily; defaults to `""` so
    *               that existing single-string call sites compile unchanged
    */
  def trace(depth: Int, brief: => String, detail: => String = ""): Unit

object Tracer:

  /**
    * Convenience helper for the common case where `brief` is a collection size
    * and `detail` is the collection elements.
    *
    * {{{
    *   val (b, d) = Tracer.collectionMsg(vertices)
    *   tracer.trace(1, b, d)
    * }}}
    */
  def collectionMsg[A](c: Iterable[A]): (String, String) =
    (s"${c.size} elements", c.mkString(", "))

  /**
    * The silent (no-op) tracer.  Resolved automatically as the default `given`
    * so that all algorithms compile and run without any explicit tracer in scope.
    * Neither `brief` nor `detail` is evaluated.
    */
  given silent[V]: Tracer[V] with
    def trace(depth: Int, brief: => String, detail: => String = ""): Unit = ()

  /**
    * A verbose tracer that indents output by two spaces per depth level and
    * emits `detail` when non-empty, falling back to `brief` otherwise.
    *
    * @param maxDepth only messages at or below this depth are emitted; defaults
    *                 to `Int.MaxValue` (emit everything).
    *
    * @param out      the stream to write to; defaults to `System.out`.
    *                 Pass a custom `PrintStream` in tests to capture output
    *                 without touching global state.
    *
    * @tparam V the vertex type
    */
  def verbose[V](maxDepth: Int = Int.MaxValue, out: PrintStream = System.out): Tracer[V] = new Tracer[V]:
    def trace(depth: Int, brief: => String, detail: => String = ""): Unit =
      if depth <= maxDepth then
        val msg = detail match
          case "" => brief
          case d => d
        out.println("  " * depth + msg)

  /**
    * A summary tracer that emits only top-level (depth 0) messages, using
    * `brief` only — `detail` is never evaluated.
    *
    * Useful for a first look: students see only the major phases of the
    * algorithm without the per-vertex noise.
    *
    * @param out the stream to write to; defaults to `System.out`.
    */
  def summary[V](out: PrintStream = System.out): Tracer[V] = new Tracer[V]:
    def trace(depth: Int, brief: => String, detail: => String = ""): Unit =
      if depth == 0 then out.println(brief)