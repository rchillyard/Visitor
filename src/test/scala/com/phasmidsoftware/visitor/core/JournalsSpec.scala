package com.phasmidsoftware.visitor.core

import java.io.StringWriter
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import scala.collection.mutable.ListBuffer
import scala.util.Using

/**
  * Tests for the [[Journal]] hierarchy, concentrating on what the per-type specs leave out.
  *
  * `ListJournalSpec`, `QueueJournalSpec` and `MapJournalSpec` each cover appending and
  * iteration for one implementation. Three things were covered by none of them:
  *
  *   1. '''`close`'''. [[Appendable]] extends `AutoCloseable` and [[Journal]] declares
  *      `close()`, yet nothing in the suite called it. Every journal here implements it as a
  *      no-op, which is correct — they hold no resource — but "correct" and "untested" were
  *      indistinguishable, and a no-op is exactly the kind of thing that is quietly made
  *      non-trivial later. [[AppendableWriter]] is the one implementation where `close` does
  *      real work, and it closes two things, only one of which is obvious.
  *   2. '''[[NonAppendable]]''', which had no spec at all.
  *   3. The accessors [[AbstractMapJournal]] offers beyond `append` and `get`.
  */
class JournalsSpec extends AnyFlatSpec with Matchers:

  /** A journal that counts how often it is closed, to prove that something closes it. */
  private class RecordingJournal extends Journal[Int, RecordingJournal]:
    var closeCount: Int = 0

    var appended: List[Int] = Nil

    def append(x: Int): RecordingJournal =
      appended = appended :+ x
      this

    def close(): Unit = closeCount += 1

  /** A `Writer` that records whether it was closed. */
  private class RecordingWriter extends StringWriter:
    var closed: Boolean = false

    override def close(): Unit =
      closed = true
      super.close()

  /** The secondary resource an `AppendableWriter` may be asked to close as well. */
  private class RecordingCloseable extends AutoCloseable:
    var closed: Boolean = false

    def close(): Unit = closed = true

  behavior of "close on the journals that hold no resource"

  it should "be a no-op that leaves a ListJournal usable" in :
    val j = ListJournal.empty[Int].append(1).append(2)
    j.close()
    j.toList shouldBe List(2, 1)
    j.append(3).toList shouldBe List(3, 2, 1)

  it should "be a no-op that leaves a QueueJournal usable" in :
    val j = QueueJournal.empty[Int].append(1).append(2)
    j.close()
    j.toList shouldBe List(1, 2)
    j.append(3).toList shouldBe List(1, 2, 3)

  it should "be a no-op that leaves a MapJournal usable" in :
    val j = MapJournal.empty[String, Int].append("a" -> 1)
    j.close()
    j.get("a") shouldBe Some(1)
    j.append("b" -> 2).get("b") shouldBe Some(2)

  it should "be a no-op that leaves a FunctionMapJournal usable" in :
    val j = FunctionMapJournal.empty[String, Int](_.length)
    j.close()
    j.appendByFunction("abc").get("abc") shouldBe Some(3)

  it should "be a no-op that leaves a CameFromJournal usable" in :
    val j = CameFromJournal.empty[Int].append(2 -> 1)
    j.close()
    j.cameFrom(2) shouldBe Some(1)
    j.append(3 -> 1).asMap shouldBe Map(2 -> 1, 3 -> 1)

  it should "be idempotent" in :
    // Nothing in the hierarchy guards against a second close, and nothing should need to.
    val j = ListJournal.empty[Int].append(1)
    j.close()
    j.close()
    j.toList shouldBe List(1)

  behavior of "the AutoCloseable contract"

  it should "let a journal be managed by Using, which closes it exactly once" in :
    val j = new RecordingJournal
    Using(j)(_.append(1).append(2)).isSuccess shouldBe true
    j.appended shouldBe List(1, 2)
    j.closeCount shouldBe 1

  it should "close the journal even when the body throws" in :
    val j = new RecordingJournal
    val result = Using(j)(_ => throw new IllegalStateException("boom"))
    result.isFailure shouldBe true
    withClue("the resource must still be released: ") :
      j.closeCount shouldBe 1

  it should "work for an ordinary journal, since Appendable extends AutoCloseable" in :
    Using(ListJournal.empty[Int])(_.append(1).append(2).toList) shouldBe util.Success(List(2, 1))

  behavior of "close on AppendableWriter, where it does real work"

  it should "close the underlying writer" in :
    val w = new RecordingWriter
    val a = new AppendableWriter(w)(None)
    a.append("hello")
    w.closed shouldBe false
    a.close()
    w.closed shouldBe true

  it should "also close the secondary closeable, which is easy to miss" in :
    // `close` closes the writer AND the optional closeable. Deleting the second line would
    // leak the FileWriter that the file-based factory wraps, and nothing else would notice.
    val w = new RecordingWriter
    val c = new RecordingCloseable
    val a = new AppendableWriter(w)(Some(c))
    a.close()
    w.closed shouldBe true
    withClue("the secondary closeable must be closed too: ") :
      c.closed shouldBe true

  it should "tolerate no secondary closeable" in :
    val a = AppendableWriter()
    a.append("x")
    noException should be thrownBy a.close()

  it should "append through to the writer and return itself" in :
    val w = new StringWriter
    val a = new AppendableWriter(w)(None)
    a.append("a").append("b") should be theSameInstanceAs a
    w.toString shouldBe "ab"

  it should "reject a file name that is not an absolute .txt path" in :
    // The four `require`s in the factory, none of which touches the filesystem when it fires.
    an[IllegalArgumentException] should be thrownBy AppendableWriter("")
    an[IllegalArgumentException] should be thrownBy AppendableWriter("relative.txt")
    an[IllegalArgumentException] should be thrownBy AppendableWriter("/absolute.dat")

  behavior of "NonAppendable"

  it should "run the side effect once per append and accumulate nothing" in :
    val seen = ListBuffer.empty[Int]
    val n = NonAppendable[Int](x => { seen += x; () })
    val after = n.append(1).append(2).append(3)
    seen.toList shouldBe List(1, 2, 3)
    withClue("it keeps no state, so it returns itself: ") :
      after should be theSameInstanceAs n

  it should "close without complaint" in :
    noException should be thrownBy NonAppendable[Int](_ => ()).close()

  behavior of "the accessors on AbstractMapJournal"

  private val mapJournal = MapJournal.empty[String, Int].append("a" -> 1).append("b" -> 2)

  it should "retrieve a value by key with apply" in :
    mapJournal("a") shouldBe 1

  it should "throw for a key it does not hold" in :
    a[NoSuchElementException] should be thrownBy mapJournal("absent")

  it should "expose keys, values and entries" in :
    mapJournal.keys.toSet shouldBe Set("a", "b")
    mapJournal.values.toSet shouldBe Set(1, 2)
    mapJournal.entries.toSet shouldBe Set("a" -> 1, "b" -> 2)

  it should "build a new journal of the same type with unit" in :
    mapJournal.unit(Map("c" -> 3)).entries.toSet shouldBe Set("c" -> 3)

  it should "replace rather than duplicate when a key is appended twice" in :
    mapJournal.append("a" -> 99).get("a") shouldBe Some(99)

  behavior of "FunctionMapJournal as a function"

  it should "be usable wherever an X => Y is expected" in :
    // FunctionMappedJournal mixes in (K => V), which nothing else in the suite exercises.
    val j = FunctionMapJournal.empty[String, Int](_.length)
    List("a", "bb", "ccc").map(j) shouldBe List(1, 2, 3)

  it should "derive values through f rather than from the map" in :
    // `apply` is overridden to consult f, so it answers for a key the journal has never seen.
    val j = FunctionMapJournal.empty[String, Int](_.length)
    j("unseen") shouldBe 6
    j.get("unseen") shouldBe None

  it should "keep f when unit builds a new instance" in :
    val j = FunctionMapJournal.empty[String, Int](_.length).unit(Map("x" -> 0))
    j("abcd") shouldBe 4
