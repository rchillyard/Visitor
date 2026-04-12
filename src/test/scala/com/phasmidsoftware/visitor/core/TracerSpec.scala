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

  it should "produce no output at depth 0" in :
    val tracer: Tracer[Int] = summon[Tracer[Int]] // resolves the given silent
    val lines = captureOutput(ps => tracer.trace(0, "top-level message"))
    lines shouldBe empty

  it should "produce no output at depth 1" in :
    val tracer: Tracer[Int] = summon[Tracer[Int]]
    val lines = captureOutput(ps => tracer.trace(1, "nested message"))
    lines shouldBe empty

  it should "not evaluate brief" in :
    val tracer: Tracer[Int] = summon[Tracer[Int]]
    var evaluated = false
    captureOutput(ps => tracer.trace(0, {
      evaluated = true; "brief"
    }))
    evaluated shouldBe false

  it should "not evaluate detail" in :
    val tracer: Tracer[Int] = summon[Tracer[Int]]
    var evaluated = false
    captureOutput(ps => tracer.trace(0, "brief", {
      evaluated = true; "detail"
    }))
    evaluated shouldBe false

  // ---------------------------------------------------------------------------
  // Tracer.verbose — brief vs detail
  // ---------------------------------------------------------------------------
  behavior of "Tracer.verbose brief/detail"

  it should "emit brief when detail is omitted" in :
    val lines = captureOutput(ps => Tracer.verbose[Int](out = ps).trace(0, "brief message"))
    lines shouldBe List("brief message")

  it should "emit brief when detail is empty string" in :
    val lines = captureOutput(ps => Tracer.verbose[Int](out = ps).trace(0, "brief message", ""))
    lines shouldBe List("brief message")

  it should "emit detail when detail is non-empty" in :
    val lines = captureOutput(ps => Tracer.verbose[Int](out = ps).trace(0, "brief message", "detailed message"))
    lines shouldBe List("detailed message")

  it should "not evaluate detail when depth exceeds maxDepth" in :
    var evaluated = false
    captureOutput(ps => Tracer.verbose[Int](maxDepth = 0, out = ps).trace(1, "brief", {
      evaluated = true; "detail"
    }))
    evaluated shouldBe false

  it should "not evaluate brief when depth exceeds maxDepth" in :
    var evaluated = false
    captureOutput(ps => Tracer.verbose[Int](maxDepth = 0, out = ps).trace(1, {
      evaluated = true; "brief"
    }))
    evaluated shouldBe false

  // ---------------------------------------------------------------------------
  // Tracer.verbose — indentation and depth
  // ---------------------------------------------------------------------------
  behavior of "Tracer.verbose"

  it should "emit an unindented message at depth 0" in :
    val lines = captureOutput(ps => Tracer.verbose[Int](out = ps).trace(0, "phase one"))
    lines shouldBe List("phase one")

  it should "indent by two spaces at depth 1" in :
    val lines = captureOutput(ps => Tracer.verbose[Int](out = ps).trace(1, "inner step"))
    lines shouldBe List("  inner step")

  it should "indent by four spaces at depth 2" in :
    val lines = captureOutput(ps => Tracer.verbose[Int](out = ps).trace(2, "deeper step"))
    lines shouldBe List("    deeper step")

  it should "emit all depths when no maxDepth is set" in :
    val lines = captureOutput: ps =>
      val tracer = Tracer.verbose[Int](out = ps)
      tracer.trace(0, "depth 0")
      tracer.trace(1, "depth 1")
      tracer.trace(2, "depth 2")
    lines shouldBe List("depth 0", "  depth 1", "    depth 2")

  it should "evaluate brief" in :
    var evaluated = false
    captureOutput(ps => Tracer.verbose[Int](out = ps).trace(0, {
      evaluated = true; "msg"
    }))
    evaluated shouldBe true

  // ---------------------------------------------------------------------------
  // Tracer.verbose(maxDepth = n)
  // ---------------------------------------------------------------------------
  behavior of "Tracer.verbose(maxDepth)"

  it should "emit messages at or below maxDepth" in :
    val lines = captureOutput: ps =>
      val tracer = Tracer.verbose[Int](maxDepth = 1, out = ps)
      tracer.trace(0, "depth 0")
      tracer.trace(1, "depth 1")
    lines shouldBe List("depth 0", "  depth 1")

  it should "suppress messages above maxDepth" in :
    val lines = captureOutput: ps =>
      val tracer = Tracer.verbose[Int](maxDepth = 1, out = ps)
      tracer.trace(0, "depth 0")
      tracer.trace(1, "depth 1")
      tracer.trace(2, "depth 2 suppressed")
    lines shouldBe List("depth 0", "  depth 1")

  // ---------------------------------------------------------------------------
  // Tracer.summary
  // ---------------------------------------------------------------------------
  behavior of "Tracer.summary"

  it should "emit only depth 0 messages" in :
    val lines = captureOutput: ps =>
      val tracer = Tracer.summary[Int](out = ps)
      tracer.trace(0, "top")
      tracer.trace(1, "suppressed")
      tracer.trace(2, "also suppressed")
    lines shouldBe List("top")

  it should "emit brief and not detail at depth 0" in :
    val lines = captureOutput(ps => Tracer.summary[Int](out = ps).trace(0, "brief", "detail"))
    lines shouldBe List("brief")

  it should "not evaluate detail even at depth 0" in :
    var evaluated = false
    captureOutput(ps => Tracer.summary[Int](out = ps).trace(0, "brief", {
      evaluated = true; "detail"
    }))
    evaluated shouldBe false

  it should "produce no output when only deeper depths are traced" in :
    val lines = captureOutput: ps =>
      val tracer = Tracer.summary[Int](out = ps)
      tracer.trace(1, "inner")
      tracer.trace(2, "deeper")
    lines shouldBe empty

  // ---------------------------------------------------------------------------
  // Tracer.collectionMsg
  // ---------------------------------------------------------------------------
  behavior of "Tracer.collectionMsg"

  it should "return size as brief and elements as detail" in :
    val (brief, detail) = Tracer.collectionMsg(List(1, 2, 3))
    brief shouldBe "3 elements"
    detail shouldBe "1, 2, 3"

  it should "return '0 elements' and empty string for an empty collection" in :
    val (brief, detail) = Tracer.collectionMsg(List.empty[Int])
    brief shouldBe "0 elements"
    detail shouldBe ""

  it should "work correctly with verbose to emit elements" in :
    val (b, d) = Tracer.collectionMsg(List(10, 20, 30))
    val lines = captureOutput(ps => Tracer.verbose[Int](out = ps).trace(0, b, d))
    lines shouldBe List("10, 20, 30")

  it should "work correctly with summary to emit only size" in :
    val (b, d) = Tracer.collectionMsg(List(10, 20, 30))
    val lines = captureOutput(ps => Tracer.summary[Int](out = ps).trace(0, b, d))
    lines shouldBe List("3 elements")

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