/*
 * Copyright (c) 2026. Phasmid Software
 */

package com.phasmidsoftware.visitor.core

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.io.{ByteArrayOutputStream, PrintStream}

class TracerSpec extends AnyFlatSpec with Matchers:

  // ---------------------------------------------------------------------------
  // Helper: capture output written to a fresh PrintStream during `block`,
  // with no global state mutation.
  // ---------------------------------------------------------------------------
  private def captureOutput(block: PrintStream => Unit): List[String] =
    val baos = ByteArrayOutputStream()
    val ps = PrintStream(baos)
    block(ps)
    ps.flush()
    baos.toString.linesIterator.filter(_.nonEmpty).toList

  // ---------------------------------------------------------------------------
  // Tracer.silent
  // ---------------------------------------------------------------------------
  behavior of "Tracer.silent"

  it should "produce no output at level 0" in :
    val tracer: Tracer[Int] = summon[Tracer[Int]] // resolves the given silent
    val lines = captureOutput(ps => tracer.trace(0, "top-level message"))
    lines shouldBe empty

  it should "produce no output at level 1" in :
    val tracer: Tracer[Int] = summon[Tracer[Int]]
    val lines = captureOutput(ps => tracer.trace(1, "nested message"))
    lines shouldBe empty

  it should "not evaluate the by-name message argument" in :
    val tracer: Tracer[Int] = summon[Tracer[Int]]
    var evaluated = false
    captureOutput(ps => tracer.trace(0, {
      evaluated = true; "side-effecting message"
    }))
    evaluated shouldBe false

  // ---------------------------------------------------------------------------
  // Tracer.verbose (no maxLevel cap)
  // ---------------------------------------------------------------------------
  behavior of "Tracer.verbose"

  it should "emit an unindented message at level 0" in :
    val lines = captureOutput(ps => Tracer.verbose[Int](out = ps).trace(0, "phase one"))
    lines shouldBe List("phase one")

  it should "indent by two spaces at level 1" in :
    val lines = captureOutput(ps => Tracer.verbose[Int](out = ps).trace(1, "inner step"))
    lines shouldBe List("  inner step")

  it should "indent by four spaces at level 2" in :
    val lines = captureOutput(ps => Tracer.verbose[Int](out = ps).trace(2, "deeper step"))
    lines shouldBe List("    deeper step")

  it should "emit all levels when no maxLevel is set" in :
    val lines = captureOutput: ps =>
      val tracer = Tracer.verbose[Int](out = ps)
      tracer.trace(0, "level 0")
      tracer.trace(1, "level 1")
      tracer.trace(2, "level 2")
    lines shouldBe List("level 0", "  level 1", "    level 2")

  it should "evaluate the by-name message argument" in :
    var evaluated = false
    captureOutput(ps => Tracer.verbose[Int](out = ps).trace(0, {
      evaluated = true; "msg"
    }))
    evaluated shouldBe true

  // ---------------------------------------------------------------------------
  // Tracer.verbose(maxLevel = n)
  // ---------------------------------------------------------------------------
  behavior of "Tracer.verbose(maxLevel)"

  it should "emit messages at or below maxLevel" in :
    val lines = captureOutput: ps =>
      val tracer = Tracer.verbose[Int](maxLevel = 1, out = ps)
      tracer.trace(0, "level 0")
      tracer.trace(1, "level 1")
    lines shouldBe List("level 0", "  level 1")

  it should "suppress messages above maxLevel" in :
    val lines = captureOutput: ps =>
      val tracer = Tracer.verbose[Int](maxLevel = 1, out = ps)
      tracer.trace(0, "level 0")
      tracer.trace(1, "level 1")
      tracer.trace(2, "level 2 suppressed")
    lines shouldBe List("level 0", "  level 1")

  it should "not evaluate the by-name message when level exceeds maxLevel" in :
    var evaluated = false
    captureOutput(ps => Tracer.verbose[Int](maxLevel = 0, out = ps).trace(1, {
      evaluated = true; "suppressed"
    }))
    evaluated shouldBe false

  // ---------------------------------------------------------------------------
  // Tracer.summary
  // ---------------------------------------------------------------------------
  behavior of "Tracer.summary"

  it should "emit only level 0 messages" in :
    val lines = captureOutput: ps =>
      val tracer = Tracer.summary[Int](out = ps)
      tracer.trace(0, "top")
      tracer.trace(1, "suppressed")
      tracer.trace(2, "also suppressed")
    lines shouldBe List("top")

  it should "produce no output when only deeper levels are traced" in :
    val lines = captureOutput: ps =>
      val tracer = Tracer.summary[Int](out = ps)
      tracer.trace(1, "inner")
      tracer.trace(2, "deeper")
    lines shouldBe empty

  // ---------------------------------------------------------------------------
  // given resolution
  // ---------------------------------------------------------------------------
  behavior of "Tracer given resolution"

  it should "resolve Tracer.silent as the default given for any vertex type" in :

    def run[V](using t: Tracer[V]): Boolean =
      var evaluated = false
      t.trace(0, {
        evaluated = true; "msg"
      })
      evaluated

    run[Int] shouldBe false
    run[String] shouldBe false