package com.phasmidsoftware.visitor.core

/**
  * A Visitor accumulates `(V, Option[R])` pairs into a journal `J` as it
  * traverses a structure.
  *
  * Two members provide access to the accumulated journal:
  *   - `journal` — the raw journal value of type `J`; used internally by
  *     concrete implementations (e.g. `JournaledVisitor.copy(journal = ...)`)
  *     where the specific `J` type must be visible for polymorphic copying.
  *   - `result` — a public alias for `journal`, defaulting to `journal`; intended
  *     as the external API for consumers of a completed traversal. A concrete
  *     implementation could override `result` to apply post-processing (e.g.
  *     reversing a `ListJournal`) without affecting the internal `journal`.
  *
  * Two events are distinguished:
  *   - `visit(v)` — called when a vertex is settled (dequeued/popped and processed).
  *     Records `(v, ev.evaluate(v))` in the journal.
  *   - `discover(v, cameFrom)` — called when a vertex is first seen as a neighbour
  *     of `cameFrom`, before it is added to the frontier. Default implementation
  *     is a no-op; override in concrete implementations that track came-from pointers.
  *
  * @tparam V the node type
  * @tparam R the result type extracted from each node
  * @tparam J the journal type (must be Appendable of (V, Option[R]))
  */
trait Visitor[V, R, J <: Appendable[(V, Option[R])]]:
  /**
    * Retrieves the journal `J` that accumulates pairs of `(V, Option[R])`
    * as a result of traversing a structure. The journal is an appendable
    * collection used for recording events during the traversal.
    *
    * @return the journal of type `J` containing the accumulated data.
    */
  def journal: J

  /**
    * Visits a vertex `v`, evaluates it using the implicit `Evaluable` instance,
    * and records the result in the journal. The visitor is returned after the
    * operation with an updated state.
    *
    * @param v  the vertex to be visited.
    * @param ev the implicit `Evaluable` instance to extract a result of type `R`
    *           from the vertex `v`.
    *
    * @return the updated `Visitor` after the vertex is processed, with its journal
    *         containing the new `(v, Option[R])` entry.
    */
  def visit(v: V)(using ev: Evaluable[V, R]): Visitor[V, R, J]

  /**
    * Called when vertex `v` is discovered as a neighbour of `cameFrom`.
    * Default: no-op. Override to record came-from relationships.
    *
    * @param v        the newly discovered vertex.
    * @param cameFrom the vertex being visited when `v` was discovered.
    * @return an updated visitor.
    */
  def discover(v: V, cameFrom: V): Visitor[V, R, J] = this

  /**
    * Public accessor for the traversal result. Defaults to `journal`.
    * Override to apply post-processing before returning results to callers.
    */
  def result: J = journal

/**
  * Canonical immutable implementation of [[Visitor]].
  *
  * Optionally carries a [[CameFromJournal]] for tracking came-from relationships.
  * When `cameFromJournal` is non-empty, `discover` records `(v, cameFrom)` pairs.
  * The start vertex is absent from the came-from map — it has no predecessor.
  *
  * Use the factory methods on the companion object to construct instances.
  *
  * @param journal         the visit journal (ListJournal or QueueJournal).
  * @param cameFromJournal optionally, a journal recording came-from relationships.
  */
case class JournaledVisitor[V, R, J <: Appendable[(V, Option[R])]](
                                                                    journal: J,
                                                                    cameFromJournal: Option[CameFromJournal[V]] = None
                                                                  ) extends Visitor[V, R, J]:

  def visit(v: V)(using ev: Evaluable[V, R]): JournaledVisitor[V, R, J] =
    copy(journal = journal.append(v -> ev.evaluate(v)).asInstanceOf[J])

  override def discover(v: V, cameFrom: V): JournaledVisitor[V, R, J] =
    cameFromJournal match
      case None => this
      case Some(cfj) => copy(cameFromJournal = Some(cfj.append(v -> cameFrom)))

  /**
    * Returns the came-from map, if came-from tracking was enabled.
    *
    * @return `Some(map)` if this visitor was created with a came-from journal,
    *         `None` otherwise.
    */
  def cameFrom: Option[Map[V, V]] = cameFromJournal.map(_.asMap)

object JournaledVisitor:
  /**
    * Creates a journaled visitor using a list-based journal for tracking visited nodes.
    *
    * The resulting visitor employs a `ListJournal` to record visited vertices in a 
    * last-in-first-out (LIFO) order. This is suitable for scenarios requiring LIFO journaling 
    * of visited nodes, such as depth-first traversal.
    *
    * @tparam V the type of vertices being visited
    * @tparam R the result type associated with each vertex
    * @return a `JournaledVisitor` configured with a `ListJournal`
    */
  def withListJournal[V, R]: JournaledVisitor[V, R, ListJournal[(V, Option[R])]] =
    JournaledVisitor(ListJournal.empty)

  /**
    * Creates a journaled visitor using a queue-based journal for tracking visited nodes.
    *
    * The resulting visitor employs a `QueueJournal` to record visited vertices in a 
    * first-in-first-out (FIFO) order. This is particularly suitable for breadth-first 
    * traversal scenarios or any situation requiring FIFO journaling of visited nodes.
    *
    * @tparam V the type of vertices being visited
    * @tparam R the result type associated with each vertex
    * @return a `JournaledVisitor` configured with a `QueueJournal`
    */
  def withQueueJournal[V, R]: JournaledVisitor[V, R, QueueJournal[(V, Option[R])]] =
    JournaledVisitor(QueueJournal.empty)

  /**
    * Creates a journaled visitor with a queue-based journal and an empty came-from journal.
    *
    * The resulting visitor uses a `QueueJournal` to track visited nodes in a FIFO order. 
    * Additionally, it enables tracking of came-from relationships to record predecessor 
    * information for visited nodes.
    *
    * @tparam V the type of vertices being visited
    * @tparam R the result type associated with each vertex
    * @return a `JournaledVisitor` configured with a `QueueJournal` and a came-from journal
    */
  def withQueueJournalAndCameFrom[V, R]: JournaledVisitor[V, R, QueueJournal[(V, Option[R])]] =
    JournaledVisitor(QueueJournal.empty, Some(CameFromJournal.empty))

  /**
    * Creates a journaled visitor with a list-based journal and an empty came-from journal.
    *
    * The resulting visitor uses a `ListJournal` to track visited nodes, maintaining 
    * a LIFO order. Additionally, it enables tracking of came-from relationships to record
    * predecessor information for visited nodes.
    *
    * @tparam V the type of vertices being visited
    * @tparam R the result type associated with each vertex
    * @return a `JournaledVisitor` configured with a `ListJournal` and a came-from journal
    */
  def withListJournalAndCameFrom[V, R]: JournaledVisitor[V, R, ListJournal[(V, Option[R])]] =
    JournaledVisitor(ListJournal.empty, Some(CameFromJournal.empty))