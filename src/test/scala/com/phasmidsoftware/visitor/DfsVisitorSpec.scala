package com.phasmidsoftware.visitor

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.util.{Failure, Success, Using}

class DfsVisitorSpec extends AnyFlatSpec with Matchers {

  behavior of "DfsVisitor"

  // Define a tree similar to a binary heap starting at slot 1
  //           10
  //        5--------13
  //      2---6---11---15
  //     1-3
  private val tree = Seq(-99, 10, 5, 13, 2, 6, 11, 15, 1, 3)

  // Define a function that gets the children of the input value `i`.
  def f(i: Int): Seq[Int] = {
    val twiceIndex = 2 * tree.indexOf(i)
    Seq(twiceIndex, twiceIndex + 1).filter(x => x > 0 && x < tree.length).map(tree)
  }

  it should "dfs pre-order" in {
    // Test a recursive pre-order traversal of the tree, starting at the root.
    Using(DfsVisitor[Int](Map(Pre -> QueueJournal.empty[Int]), f)) {
      visitor =>
        for {journal <- visitor.dfs(10).journals
             entry <- journal
             } yield entry
    } shouldBe Success(List(10, 5, 2, 1, 3, 6, 13, 11, 15))
  }

  it should "dfs pre-order with traversal type" in {
    val function: Int => String = x => x.toString
    // Test a recursive pre-order traversal of the tree, starting at the root.
    val entry: (Message, FunctionMapJournal[Int, String]) = Pre -> FunctionMapJournal.empty[Int, String](function)
    Using(DfsVisitorMapped[Int, String](Map(entry), function, f)) {
      visitor =>
        // NOTE that we do not return the journal as an Iterable because a Map will essentially return the entries in random order.
        visitor.dfs(10).journals.headOption.asInstanceOf[Option[FunctionMapJournal[Int, String]]]
    } match {
      case Success(Some(journal)) =>
        journal.get(10) shouldBe Some("10")
        journal.get(15) shouldBe Some("15")
        journal.get(13) shouldBe Some("13")
        journal.get(2) shouldBe Some("2")
      case Success(journal) =>
        fail(s"Expected MapJournal, got $journal")
      case Failure(exception) =>
        fail(exception)
    }
  }

  it should "dfs reverse post-order" in {
    // Test a recursive pre-order traversal of the tree, starting at the root.
    Using(DfsVisitor[Int](Map(Post -> ListJournal.empty[Int]), f)) {
      visitor =>
        for {journal <- visitor.dfs(10).journals
             entry <- journal
             } yield entry
    } shouldBe Success(List(10, 13, 15, 11, 5, 6, 2, 3, 1))
  }

  it should "dfs in-order" in {
    // Test a recursive pre-order traversal of the tree, starting at the root.
    Using(DfsVisitor[Int](Map(In -> QueueJournal.empty[Int]), f)) {
      visitor =>
        for {journal <- visitor.dfs(10).journals
             entry <- journal
             } yield entry
    } shouldBe Success(List(1, 2, 3, 5, 6, 10, 11, 13, 15))
  }

  it should "dfs with discovery" in {
    case class Discoverable(i: Int, var discovered: Boolean = false) {
      def discover(): Unit = discovered = true
    }
    object Discoverable {
      def create(i: Int): Discoverable = new Discoverable(i, false)
    }
    val inputs: Seq[Discoverable] = Seq(10, 5, 13, 2, 6, 11, 15, 1, 3) map Discoverable.create
    val f: Discoverable => Seq[Discoverable] = {
      x =>
        inputs.find(d => d.i == x.i) match {
          case Some(d) =>
            val index = inputs.indexOf(d)
            if (index < 0)
              throw new NoSuchElementException(s"No such element: $d")
            else if (index < inputs.length - 1)
              inputs.slice(index, index + 1) filterNot (d => d.discovered)
            else
              Seq.empty[Discoverable]
          case None =>
            throw new NoSuchElementException(s"No such element with value: $x")
        }
    }

    val preEntry = Pre -> QueueJournal.empty[Discoverable]
    val selfEntry = SelfVisit -> NonAppendable[Discoverable](_.discover())
    Using(DfsVisitor(Map(preEntry, selfEntry), f)) {
      visitor =>
        for {journal <- visitor.dfs(Discoverable(10)).journals
             entry <- journal
             } yield entry
    } match {
      case Success(discoverables) =>
        discoverables.forall(_.discovered) shouldBe true
      case Failure(exception) =>
        fail(exception)
    }
  }
}
