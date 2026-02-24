package com.phasmidsoftware.visitor.core

import scala.collection.immutable.Queue

// ============================================================
// Core typeclasses for the various behaviours
// ============================================================

/**
  * Typeclass: given a node of type V, extract an optional result of type R.
  * This is the "what do we want to know about a node" concern.
  *
  * @tparam V the node type
  * @tparam R the result type
  */
trait Evaluable[V, R]:
  def evaluate(v: V): Option[R]

/**
  * Typeclass: given a structure H, yield the neighbouring nodes of type V.
  * This is the "how do we traverse" concern — completely decoupled from domain.
  *
  * For homogeneous graphs (node points to node), use GraphNeighbours[V] = Neighbours[V, V].
  * For trees, H might be Tree[A] and V might be A.
  *
  * @tparam H the container / structure type
  * @tparam V the neighbour / child type
  */
trait Neighbours[H, V]:
  def neighbours(h: H): Iterator[V]

/** Convenience alias for homogeneous graphs. */
type GraphNeighbours[V] = Neighbours[V, V]

/** American English alias for [[Neighbours]] */
type Neighbors[H, V] = Neighbours[H, V]

/** American English alias for [[GraphNeighbours]] */
type GraphNeighbors[V] = GraphNeighbours[V]

/**
  * Typeclass: a set of visited nodes, used to prevent revisiting in cycle-containing graphs.
  * Immutable — markVisited returns a new instance.
  *
  * @tparam V the node type
  */
trait VisitedSet[V]:
  def isVisited(v: V): Boolean

  def markVisited(v: V): VisitedSet[V]

/** Default given: immutable Set-backed VisitedSet. */
given [V]: VisitedSet[V] = SetVisitedSet(Set.empty)

private case class SetVisitedSet[V](visited: Set[V]) extends VisitedSet[V]:
  def isVisited(v: V): Boolean = visited.contains(v)

  def markVisited(v: V): VisitedSet[V] = copy(visited + v)

// ============================================================
// Frontier typeclasses: Queueable and Stackable
// ============================================================

/**
  * Typeclass: abstracts over frontier data structures (Queue, Stack, PriorityQueue).
  * The three implementations below give us BFS, DFS, and best-first / Dijkstra-style
  * traversals for free — the traversal algorithm itself doesn't change at all.
  *
  * @tparam F the higher-kinded frontier container type
  */
trait Frontier[F[_]]:
  /**
    * Creates an empty frontier container of type F.
    *
    * @return an empty instance of the frontier container F[T]
    */
  def empty[T]: F[T]

  /**
    * Adds a given element to the frontier container, returning an updated instance of the container.
    *
    * @param f the frontier container of type F[T] to which the element will be added
    * @param t the element of type T to add to the frontier container
    * @return a new instance of the frontier container F[T] with the element added
    */
  def offer[T](f: F[T])(t: T): F[T]

  /**
    * Removes an element from the frontier container and returns a tuple containing the removed element
    * and the updated container with the element removed.
    *
    * @param f the frontier container of type F[T] to extract the element from
    * @return a tuple where the first element is the extracted element of type T and the second element
    *         is the updated frontier container of type F[T]
    */
  def take[T](f: F[T]): (T, F[T])

  /**
    * Determines if the given frontier container is empty.
    *
    * @param f the frontier container of type F[T] to be checked
    * @return true if the frontier container is empty, false otherwise
    */
  def isEmpty[T](f: F[T]): Boolean

  /** If true, offerAll reverses the list before offering (needed for LIFO stacks). */
  def reverseOnOffer: Boolean = false

  /**
    * Adds all elements from the given list to the specified frontier container in the order
    * determined by the `reverseOnOffer` flag. If `reverseOnOffer` is true, the list of elements
    * is reversed before being added. Each element is added using the `offer` method.
    *
    * @param f  the frontier container of type F[T] to which the elements will be added
    * @param ts the list of elements of type T to add to the frontier container
    * @return a new instance of the frontier container F[T] with all the elements added
    */
  def offerAll[T](f: F[T])(ts: List[T]): F[T] =
    val ordered = if reverseOnOffer then ts.reverse else ts
    ordered.foldLeft(f)((acc: F[T], t: T) => offer(acc)(t))

/** DFS frontier: LIFO Stack (represented as a List). */
type Stack[T] = List[T]

given Frontier[Stack] with
  def empty[T]: Stack[T] = Nil

  def offer[T](f: Stack[T])(t: T): Stack[T] = t :: f

  def take[T](f: Stack[T]): (T, Stack[T]) = (f.head, f.tail)

  def isEmpty[T](f: Stack[T]): Boolean = f.isEmpty

  override def reverseOnOffer: Boolean = true

/** BFS frontier: FIFO Queue. */
given Frontier[Queue] with
  def empty[T]: Queue[T] = Queue.empty

  def offer[T](f: Queue[T])(t: T): Queue[T] = f.enqueue(t)

  def take[T](f: Queue[T]): (T, Queue[T]) = f.dequeue

  def isEmpty[T](f: Queue[T]): Boolean = f.isEmpty

/**
  * Priority-queue frontier: best-first / Dijkstra-style traversal.
  * Requires an Ordering[T] at the point of use.
  *
  * Backed by an immutable SortedSet for simplicity; swap for a proper
  * binary heap if performance matters.
  *
  * NOTE: T must have an Ordering and must be unique (no duplicate nodes
  * in the frontier at the same priority). For weighted graphs you'd
  * typically wrap nodes as (priority, node) tuples.
  */
given Frontier[PrioQueue] with
  def empty[T]: PrioQueue[T] =
    throw new UnsupportedOperationException("Use PrioQueue.empty[T] directly")

  def offer[T](f: PrioQueue[T])(t: T): PrioQueue[T] = f.offer(t)

  def take[T](f: PrioQueue[T]): (T, PrioQueue[T]) = f.take

  def isEmpty[T](f: PrioQueue[T]): Boolean = f.isEmpty

// ============================================================
// Visitor
// ============================================================

/**
  * A Visitor accumulates (V, Option[R]) pairs into a journal J as it
  * traverses a structure. It knows nothing about the structure itself —
  * that knowledge lives in Neighbours. It knows nothing about what to
  * extract from a node — that lives in Evaluable.
  *
  * @tparam V the node type
  * @tparam R the result type extracted from each node
  * @tparam J the journal type (must be Appendable of (V, Option[R]))
  */
trait Visitor[V, R, J <: Appendable[(V, Option[R])]]:
  def journal: J

  /** Visit a single node, appending the result to the journal. */
  def visit(v: V)(using ev: Evaluable[V, R]): Visitor[V, R, J]

  /** Return the completed journal. */
  def result: J = journal

/** Canonical immutable implementation. */
case class JournaledVisitor[V, R, J <: Appendable[(V, Option[R])]](journal: J)
  extends Visitor[V, R, J]:
  def visit(v: V)(using ev: Evaluable[V, R]): JournaledVisitor[V, R, J] =
    copy(journal = journal.append(v -> ev.evaluate(v)).asInstanceOf[J])

object JournaledVisitor:
  def withListJournal[V, R]: JournaledVisitor[V, R, ListJournal[(V, Option[R])]] =
    JournaledVisitor(ListJournal.empty)

  def withQueueJournal[V, R]: JournaledVisitor[V, R, QueueJournal[(V, Option[R])]] =
    JournaledVisitor(QueueJournal.empty)
