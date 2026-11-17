package com.phasmidsoftware.visitor.core

/** What an evaluating journal holds: a node, paired with whatever was extracted from it. */
type Mapped[V, R] = (V, Option[R])

/**
  * A Visitor accumulates entries of type `E` into a journal `J` as it traverses a structure.
  *
  * `E` is the journal's element type, and it is what distinguishes the two implementations:
  *   - [[JournaledVisitor]] records `Mapped[V, R]` — each node with the result extracted from
  *     it by an [[Evaluable]].
  *   - [[SimpleVisitor]] records `V` — the nodes themselves, with no evaluation at all.
  *
  * Keeping `E` abstract here is what lets both be ordinary `Visitor`s, so every entry point on
  * [[Traversal]] accepts either. `Appendable` is invariant in its element type (it appears both
  * as `append`'s parameter and in its result), so `Appendable[V]` is not an `Appendable[(V,
  * Option[R])]`; without `E` the simple case could only be expressed by wrapping the journal in
  * an adapter that threw half of every entry away.
  *
  * Note that `visit` takes no `using` clause. A visitor that needs an [[Evaluable]] holds its
  * own, bound once when the visitor is built, rather than resolving one implicitly on every
  * call.
  *
  * Two members provide access to the accumulated journal:
  *   - `journal` — the raw journal value of type `J`; used internally by concrete
  *     implementations (e.g. `JournaledVisitor.copy(journal = ...)`) where the specific `J` type
  *     must be visible for polymorphic copying.
  *   - `result` — a public alias for `journal`, defaulting to `journal`; intended as the
  *     external API for consumers of a completed traversal. A concrete implementation could
  *     override `result` to apply post-processing (e.g. reversing a `ListJournal`) without
  *     affecting the internal `journal`.
  *
  * Two events are distinguished:
  *   - `visit(v)` — called when a node is settled (dequeued/popped and processed).
  *   - `discover(v, cameFrom)` — called when a node is first seen as a neighbour of `cameFrom`,
  *     before it is added to the frontier. The default is a no-op; override it in an
  *     implementation that tracks came-from pointers. Note that `traverseTree` never calls it,
  *     a tree's parent being evident from its shape.
  *
  * @tparam V the node type
  * @tparam E the journal's element type
  * @tparam J the journal type (must be an `Appendable` of `E`)
  */
trait Visitor[V, E, J <: Appendable[E, J]]:
  /**
    * Visits a node `v` and records it in the journal, returning the updated visitor.
    *
    * @param v the node to be visited.
    * @return the updated `Visitor`, its journal carrying the new entry.
    */
  def visit(v: V): Visitor[V, E, J]

  /**
    * Called when `v` is first discovered as a neighbour of `cameFrom`, before `v` is offered to
    * the frontier. The default implementation ignores it.
    *
    * @param v        the newly discovered node.
    * @param cameFrom the node from which `v` was reached.
    * @return the updated `Visitor`; by default, this one unchanged.
    */
  def discover(v: V, cameFrom: V): Visitor[V, E, J] = this

  /**
    * Public accessor for the traversal result. Defaults to `journal`.
    * Override to apply post-processing before returning results to callers.
    */
  def result: J = journal

  /**
    * Protected accessor for the journal `J` that accumulates entries of type `E` during a
    * traversal.
    *
    * @return the journal of type `J` containing the accumulated data.
    */
  protected def journal: J

/**
  * Canonical immutable implementation of [[Visitor]] that evaluates each node as it is visited,
  * recording `Mapped[V, R]` entries.
  *
  * Optionally carries a [[CameFromJournal]] for tracking came-from relationships.
  * When `cameFromJournal` is non-empty, `discover` records `(v, cameFrom)` pairs.
  * The start node is absent from the came-from map — it has no predecessor.
  *
  * Use the factory methods on the companion object to construct instances; they take the
  * [[Evaluable]] from the implicit scope.
  *
  * @param journal         the visit journal (ListJournal or QueueJournal).
  * @param evaluable       extracts the result recorded alongside each node.
  * @param cameFromJournal optionally, a journal recording came-from relationships.
  */
case class JournaledVisitor[V, R, J <: Appendable[Mapped[V, R], J]](
                                                                  protected val journal: J,
                                                                  evaluable: Evaluable[V, R],
                                                                  cameFromJournal: Option[CameFromJournal[V]] = None
                                                                ) extends Visitor[V, Mapped[V, R], J]:

  def visit(v: V): JournaledVisitor[V, R, J] =
    copy(journal = journal.append(v -> evaluable.evaluate(v)))

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
    * The resulting visitor employs a `ListJournal` to record visited nodes in a
    * last-in-first-out (LIFO) order. This is suitable for scenarios requiring LIFO journaling
    * of visited nodes, such as depth-first traversal.
    *
    * @tparam V the type of nodes being visited
    * @tparam R the result type associated with each node
    * @return a `JournaledVisitor` configured with a `ListJournal`
    */
  def withListJournal[V, R](using ev: Evaluable[V, R]): JournaledVisitor[V, R, ListJournal[Mapped[V, R]]] =
    JournaledVisitor(ListJournal.empty, ev)

  /**
    * Creates a journaled visitor using a queue-based journal for tracking visited nodes.
    *
    * The resulting visitor employs a `QueueJournal` to record visited nodes in a first-in-
    * first-out (FIFO) order. This is particularly suitable for breadth-first traversal
    * scenarios or any situation requiring FIFO journaling of visited nodes.
    *
    * @tparam V the type of nodes being visited
    * @tparam R the result type associated with each node
    * @return a `JournaledVisitor` configured with a `QueueJournal`
    */
  def withQueueJournal[V, R](using ev: Evaluable[V, R]): JournaledVisitor[V, R, QueueJournal[Mapped[V, R]]] =
    JournaledVisitor(QueueJournal.empty, ev)

  /**
    * Creates a journaled visitor with a queue-based journal and an empty came-from journal.
    *
    * @tparam V the type of nodes being visited
    * @tparam R the result type associated with each node
    * @return a `JournaledVisitor` configured with a `QueueJournal` and a came-from journal
    */
  def withQueueJournalAndCameFrom[V, R](using ev: Evaluable[V, R]): JournaledVisitor[V, R, QueueJournal[Mapped[V, R]]] =
    JournaledVisitor(QueueJournal.empty, ev, Some(CameFromJournal.empty))

  /**
    * Creates a journaled visitor with a list-based journal and an empty came-from journal.
    *
    * @tparam V the type of nodes being visited
    * @tparam R the result type associated with each node
    * @return a `JournaledVisitor` configured with a `ListJournal` and a came-from journal
    */
  def withListJournalAndCameFrom[V, R](using ev: Evaluable[V, R]): JournaledVisitor[V, R, ListJournal[Mapped[V, R]]] =
    JournaledVisitor(ListJournal.empty, ev, Some(CameFromJournal.empty))

/**
  * A [[Visitor]] that records the nodes themselves, with no evaluation.
  *
  * This is the visitor to reach for when the traversal order *is* the answer — printing each
  * node, collecting a visit sequence, checking reachability. It needs no [[Evaluable]], and its
  * journal is an ordinary `Appendable[V]`, so there is no pair and no `Option` allocated per
  * visit.
  *
  * It does not override `discover`, so came-from relationships are not recorded. Use
  * [[JournaledVisitor]] with a came-from journal if you need those.
  *
  * @param journal the visit journal (ListJournal or QueueJournal of `V`).
  * @tparam V the node type
  * @tparam J the journal type (must be an `Appendable` of `V`)
  */
case class SimpleVisitor[V, J <: Appendable[V, J]](
                                                 protected val journal: J
                                               ) extends Visitor[V, V, J]:

  def visit(v: V): SimpleVisitor[V, J] =
    copy(journal = journal.append(v))

object SimpleVisitor:
  /**
    * Creates a simple visitor with a queue-based journal, so the journal reads in visit order.
    *
    * @tparam V the type of nodes being visited
    * @return a `SimpleVisitor` configured with a `QueueJournal`
    */
  def withQueueJournal[V]: SimpleVisitor[V, QueueJournal[V]] =
    SimpleVisitor(QueueJournal.empty)

  /**
    * Creates a simple visitor with a list-based journal, which prepends, so the journal reads in
    * reverse visit order.
    *
    * @tparam V the type of nodes being visited
    * @return a `SimpleVisitor` configured with a `ListJournal`
    */
  def withListJournal[V]: SimpleVisitor[V, ListJournal[V]] =
    SimpleVisitor(ListJournal.empty)
