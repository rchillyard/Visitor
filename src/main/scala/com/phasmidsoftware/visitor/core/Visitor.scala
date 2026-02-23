package com.phasmidsoftware.visitor.core

import scala.collection.immutable.Queue

// ============================================================
// Core typeclasses
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
  def empty[T]: F[T]

  def offer[T](f: F[T])(t: T): F[T]

  def take[T](f: F[T]): (T, F[T])

  def isEmpty[T](f: F[T]): Boolean

/** BFS frontier: FIFO Queue. */
given Frontier[Queue] with
  def empty[T]: Queue[T] = Queue.empty

  def offer[T](f: Queue[T])(t: T): Queue[T] = f.enqueue(t)

  def take[T](f: Queue[T]): (T, Queue[T]) = f.dequeue

  def isEmpty[T](f: Queue[T]): Boolean = f.isEmpty

/** DFS frontier: LIFO Stack (represented as a List). */
type Stack[T] = List[T]

given Frontier[Stack] with
  def empty[T]: Stack[T] = Nil

  def offer[T](f: Stack[T])(t: T): Stack[T] = t :: f

  def take[T](f: Stack[T]): (T, Stack[T]) = (f.head, f.tail)

  def isEmpty[T](f: Stack[T]): Boolean = f.isEmpty

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
// Journal / Appendable (unchanged from old package but repeated
// here for a self-contained skeleton)
// ============================================================

trait Appendable[X] extends AutoCloseable:
  def append(x: X): Appendable[X]

trait Journal[X] extends Appendable[X]:
  def close(): Unit

trait IterableJournal[X] extends Journal[X] with Iterable[X]

case class ListJournal[X](xs: List[X]) extends IterableJournal[X]:
  def append(x: X): ListJournal[X] = copy(x :: xs)

  def iterator: Iterator[X] = xs.iterator

  def close(): Unit = ()

object ListJournal:
  def empty[X]: ListJournal[X] = ListJournal(Nil)

case class QueueJournal[X](q: Queue[X]) extends IterableJournal[X]:
  def append(x: X): QueueJournal[X] = copy(q.enqueue(x))

  def iterator: Iterator[X] = q.iterator

  def close(): Unit = ()

object QueueJournal:
  def empty[X]: QueueJournal[X] = QueueJournal(Queue.empty)

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

// ============================================================
// Traversal engine
// ============================================================

/**
  * The traversal engine. Knows only about Frontier, Neighbours, Evaluable,
  * VisitedSet, and Visitor. Zero domain knowledge.
  *
  * The choice of frontier F determines the traversal order:
  * F = Queue     → BFS
  * F = Stack     → DFS
  * F = PrioQueue → best-first (requires Ordering[V] in scope)
  */
object Traversal:

  //  private def offerTo[F[_], T](fr: Frontier[F])(f: F[T])(t: T): F[T] = fr.offer(f)(t)

  /**
    * Traverses a (homogeneous) graph structure starting from a given node, visiting nodes iteratively
    * and accumulating results into a visitor. The traversal is controlled by the 
    * concepts of a frontier, visited nodes, and neighbours of nodes.
    *
    * NOTE: Homogeneous graph traversal — H and V are the same type.
    * The =:= witness lets the compiler accept V where H is expected.
    *
    * @param start   The starting node where the traversal begins.
    * @param visitor An instance of `Visitor` that collects results during traversal.
    * @param nbrs    A typeclass instance providing the neighbours for each node in the graph.
    * @param ev      A typeclass instance defining how to extract results from each node.
    * @param vs      A typeclass instance representing the set of visited nodes to prevent revisiting.
    * @param fr      A typeclass instance representing the frontier structure used for traversal.
    * @param initial The initial empty frontier structure for the traversal.
    * @return A `Visitor` instance containing the accumulated results after traversal completes.
    */
  def traverse[V, R, J <: Appendable[(V, Option[R])], F[_]](
                                                             start: V,
                                                             visitor: Visitor[V, R, J]
                                                           )(using
                                                             nbrs: Neighbours[V, V], // H = V explicitly
                                                             ev: Evaluable[V, R],
                                                             vs: VisitedSet[V],
                                                             fr: Frontier[F],
                                                             initial: F[V]
                                                           ): Visitor[V, R, J] =
    @annotation.tailrec
    def loop(
              frontier: F[V],
              vis: Visitor[V, R, J],
              visited: VisitedSet[V]
            ): Visitor[V, R, J] =
      if fr.isEmpty(frontier) then vis
      else
        val (node, rest) = fr.take(frontier)
        if visited.isVisited(node) then loop(rest, vis, visited)
        else
          val newVisited = visited.markVisited(node)
          val newVisitor = vis.visit(node)
          val newFrontier = nbrs.neighbours(node) // node: V, nbrs: Neighbours[V,V] ✓
            .filterNot(newVisited.isVisited)
            .foldLeft[F[V]](rest)((acc: F[V], v: V) => fr.offer(acc)(v))
          loop(newFrontier, newVisitor, newVisited)

    val seedFrontier = nbrs.neighbours(start).foldLeft[F[V]](initial)((acc: F[V], v: V) => fr.offer(acc)(v))
    loop(seedFrontier, visitor.visit(start), vs.markVisited(start))


  /**
    * Traverses a tree-like structure starting from a given root node, visiting nodes
    * as specified by the provided `Visitor`. The traversal is driven by the concepts
    * of a frontier (e.g., queue or stack), visited nodes, and neighbours of nodes.
    * NOTE that traverseTree doesn't visit start itself (no Evaluable[H, R] in scope), so visitor and vs are passed unseedeed to loop — 
    * unlike traverse where we call visitor.visit(start) and vs.markVisited(start) before entering the loop.
    *
    * @param start     The root node where the traversal begins.
    * @param visitor   An instance of `Visitor` that accumulates results during the traversal.
    * @param rootNbrs  A typeclass instance providing neighbour nodes for the root structure.
    * @param graphNbrs A typeclass instance providing neighbour nodes for the graph structure.
    * @param ev        A typeclass representing how to evaluate a node and extract its result.
    * @param vs        A typeclass representing a set of visited nodes to prevent revisits.
    * @param fr        A typeclass representing the frontier data structure used for traversal.
    * @param initial   The initial empty frontier structure.
    * @return A `Visitor` instance containing the accumulated results of the traversal.
    */
  def traverseTree[H, V, R, J <: Appendable[(V, Option[R])], F[_]](
                                                                    start: H,
                                                                    visitor: Visitor[V, R, J]
                                                                  )(using
                                                                    rootNbrs: Neighbours[H, V],
                                                                    graphNbrs: Neighbours[V, V],
                                                                    ev: Evaluable[V, R],
                                                                    vs: VisitedSet[V],
                                                                    fr: Frontier[F],
                                                                    initial: F[V]
                                                                  ): Visitor[V, R, J] =

    @annotation.tailrec
    def loop(
              frontier: F[V],
              vis: Visitor[V, R, J],
              visited: VisitedSet[V]
            ): Visitor[V, R, J] =
      if fr.isEmpty(frontier) then vis
      else
        val (node, rest) = fr.take(frontier)
        if visited.isVisited(node) then loop(rest, vis, visited)
        else
          val newVisited = visited.markVisited(node)
          val newVisitor = vis.visit(node)
          val newFrontier = graphNbrs.neighbours(node)
            .filterNot(newVisited.isVisited)
            .foldLeft[F[V]](rest)((acc: F[V], v: V) => fr.offer(acc)(v))
          loop(newFrontier, newVisitor, newVisited)

    val seedFrontier = rootNbrs.neighbours(start)
      .foldLeft[F[V]](initial)((acc: F[V], v: V) => fr.offer(acc)(v))
    loop(seedFrontier, visitor, vs)

  // ----------------------------------------------------------
  // Convenience entry points
  // ----------------------------------------------------------

  /** Breadth-first search. */
  def bfs[V, R, J <: Appendable[(V, Option[R])]](
                                                  start: V,
                                                  visitor: Visitor[V, R, J]
                                                )(using
                                                  nbrs: GraphNeighbours[V],
                                                  ev: Evaluable[V, R],
                                                  vs: VisitedSet[V]
                                                ): Visitor[V, R, J] =
    given Queue[V] = Queue.empty

    traverse[V, R, J, Queue](start, visitor)

  /** Depth-first search. */
  def dfs[V, R, J <: Appendable[(V, Option[R])]](
                                                  start: V,
                                                  visitor: Visitor[V, R, J]
                                                )(using
                                                  nbrs: GraphNeighbours[V],
                                                  ev: Evaluable[V, R],
                                                  vs: VisitedSet[V]
                                                ): Visitor[V, R, J] =
    given Stack[V] = List.empty

    traverse[V, R, J, Stack](start, visitor)

  /** Best-first / priority-queue traversal. Requires Ordering[V] in scope. */
  def bestFirst[V: Ordering, R, J <: Appendable[(V, Option[R])]](
                                                                  start: V,
                                                                  visitor: Visitor[V, R, J]
                                                                )(using
                                                                  nbrs: GraphNeighbours[V],
                                                                  ev: Evaluable[V, R],
                                                                  vs: VisitedSet[V]
                                                                ): Visitor[V, R, J] =
    given PrioQueue[V] = PrioQueue.empty[V] // Ordering[V] is in scope via : Ordering

    traverse[V, R, J, PrioQueue](start, visitor)

/** American English alias for [[Neighbours]] */
type Neighbors[H, V] = Neighbours[H, V]

/** American English alias for [[GraphNeighbours]] */
type GraphNeighbors[V] = GraphNeighbours[V]
