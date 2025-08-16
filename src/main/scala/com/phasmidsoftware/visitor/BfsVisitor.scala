package com.phasmidsoftware.visitor

import scala.annotation.tailrec
import scala.collection.immutable.Queue

/**
 * Represents a visitor designed for performing breadth-first search (BFS) traversal
 * on a graph or tree-like structure. This trait extends the functionalities of the
 * generic `Visitor` trait, enabling stateful BFS traversal while managing visited nodes
 * and pending nodes to explore.
 *
 * The `BfsVisitor` trait supports the initialization, execution, and continuation of a BFS
 * traversal starting from a specified element. It maintains the traversal state, which
 * includes a queue of nodes to visit and an optional current element being processed.
 *
 * @tparam X the type of elements that the visitor operates on during the BFS traversal.
 */
trait BfsVisitor[X] extends Visitor[X] with Bfs[X, BfsVisitor[X]] {
  /**
   * Performs a breadth-first search (BFS) starting from the given element `x` and returns
   * a tuple containing the updated BFS visitor state and an optional element,
   * which will be defined if the goal was satisfied.
   *
   * @param x the starting element for the BFS traversal
   * @return a tuple where the first element is the updated `BfsVisitor[X]` instance
   *         representing the state after processing, and the second element is an optional `X`
   *         representing the goal, otherwise `None`.
   */
  def bfs(x: X): (BfsVisitor[X], Option[X])
}

/**
 * Abstract class that provides a framework for implementing breadth-first search (BFS) traversal
 * using the Visitor pattern. It combines the features of queue-based traversal and visitor behavior,
 * allowing customized handling of nodes during BFS.
 *
 * @tparam Q the higher-kinded type representing the queue structure used for BFS
 * @tparam X the type of elements being visited and traversed in the BFS
 * @constructor
 * Creates a new `AbstractBfsVisitor` with the provided queue, mapping of messages to appendable containers,
 * a function to determine children of an element in the traversal, and a goal function for termination.
 * @param queue     the initial queue instance to manage elements during the BFS traversal
 * @param map       a mapping of `Message` types to `Appendable` instances, allowing state to be appended
 *                  based on messages during the visitation
 * @param f         a function that generates child nodes from a given node being visited
 * @param goal      a predicate function that determines if the goal or termination condition is met
 * @param queueable an implicit `Queueable[Q]` typeclass instance providing operations for the queue
 */
abstract class AbstractBfsVisitor[Q[_], X](queue: Q[X], map: Map[Message, Appendable[X]], f: X => Seq[X], goal: X => Boolean)(using queueable: Queueable[Q]) extends AbstractMultiVisitor[X](map) with BfsVisitor[X] with GoalOriented[X] {
  /**
   * Executes a breadth-first search (BFS) starting from the given element `x`.
   *
   * This method initiates the BFS traversal by enqueuing the starting element,
   * processing the queue iteratively using the `inner` method, and returning
   * the updated visitor state and an optional result if a goal element is found.
   *
   * @param x the starting element for the BFS traversal
   * @return a tuple where the first element is the updated `BfsVisitor[X]` instance
   *         representing the visitor's state after traversal, and the second element
   *         is an optional `X` representing the goal element if found, otherwise `None`.
   */
  def bfs(x: X): (BfsVisitor[X], Option[X]) =
    unitQueue(queueable.offer(queue)(x)).inner

  /**
   * Make a visit, with the given message and `X` value, on this `Visitor` and return a new `Visitor`.
   *
   * This method defines the behavior for handling a `Message` in the context
   * of the Visitor pattern. The implementation of this method should use the provided
   * message and state to determine the next state and return the appropriate `Visitor`.
   *
   * @param msg the message to be processed by the visitor
   * @param x   the current state or context associated with the visitor
   * @return a new `Visitor[X]` instance that represents the updated state after processing the message
   */
  override def visit(msg: Message)(x: X): AbstractBfsVisitor[Q, X] = super.visit(msg)(x).asInstanceOf[AbstractBfsVisitor[Q, X]]

  /**
   * Constructs a new `AbstractBfsVisitor` instance with the provided queue.
   *
   * This method serves as a constructor or initializer for creating a visitor instance
   * that operates using the given `queue`. It is used to establish the visitor's
   * initial state and context for breadth-first search (BFS) traversal.
   *
   * @param queue the queue of type `Q[X]` to be used for managing BFS traversal elements
   * @return an instance of `AbstractBfsVisitor[Q, X]` initialized with the specified queue
   */
  def unitQueue(queue: Q[X]): AbstractBfsVisitor[Q, X]

  /**
   * Recursively processes a breadth-first search (BFS) by consuming elements from the queue and
   * applying the visitor pattern to track the traversal state and results.
   *
   * This method extracts elements from the queue using `doTake`, checks if the goal condition is met,
   * and either updates the traversal state or continues processing the queue. If a goal element is found,
   * it returns the current visitor state and the goal element wrapped in an `Option`. If the queue is
   * exhausted without finding a goal element, it returns the current visitor state and `None`.
   *
   * @return a tuple where the first element is an updated instance of `AbstractBfsVisitor[Q, X]`, 
   *         and the second element is an `Option[X]` that contains the goal element if found, otherwise `None`.
   */
  @tailrec
  private def inner: (AbstractBfsVisitor[Q, X], Option[X]) = doTake() match {
    case None =>
      (this, None)
    case Some((x, q)) if goal(x) =>
      (unitQueue(q), Some(x))
    case Some((x, q)) =>
      val visitor: AbstractBfsVisitor[Q, X] = unitQueue(q).visit(Pre)(x)
      f(x).foldLeft(visitor) { (v, x) => v.unitQueue(v.doOffer(x)) }.inner
  }

  /**
   * Offers the given element to the queue, returning a new queue with the element added.
   *
   * This method utilizes the `queueable.offer` function to enqueue the specified element
   * into the provided queue, ensuring a pseudo-immutable operation by returning a new
   * queue instance with the updated state.
   *
   * @param x the element of type `X` to be added to the queue
   * @return a new `Q[X]` instance representing the queue after adding the given element
   */
  private def doOffer(x: X): Q[X] =
    queueable.offer(queue)(x)

  /**
   * Extracts an element from the queue if it is not empty.
   *
   * This method checks if the queue is empty using the `queueable.isEmpty` function. If the queue
   * contains elements, it retrieves the first element and the remaining queue using the `queueable.take` function
   * and wraps the result in an `Option`. If the queue is empty, it returns `None`.
   *
   * @return an `Option` containing a tuple of the extracted element and the updated queue (`(X, Q[X])`)
   *         if the queue is not empty, or `None` if the queue is empty.
   */
  private def doTake(): Option[(X, Q[X])] =
    Option.when(!queueable.isEmpty(queue))(queueable.take(queue))
}

  /**
   * A breadth-first search (BFS) visitor that maintains a queue to traverse elements
   * and a mapping of messages to appendable structures for storing intermediary states.
   *
   * `BfsQueueableVisitor` extends `AbstractBfsVisitor` and uses a queue-based mechanism to
   * explore elements in a breadth-first manner. It allows customization of the traversal
   * logic, the goal condition, and the mapping used for processing messages.
   *
   * @param queue     the initial queue containing elements of type `X` to start the traversal process
   * @param map       a mapping of `Message` types to `Appendable[X]`, used for handling messages during traversal
   * @param f         a function that takes an element of type `X` and returns a sequence of new elements to be explored
   * @param goal      a function that evaluates whether a given element of type `X` satisfies the traversal goal
   * @param queueable a given instance of `Queueable[Q]` that provides operations for the queue data structure
   */
  case class BfsQueueableVisitor[Q[_], X](queue: Q[X], map: Map[Message, Appendable[X]], f: X => Seq[X], goal: X => Boolean)(using queueable: Queueable[Q]) extends AbstractBfsVisitor[Q, X](queue, map, f, goal) {

    /**
     * Constructs a new `AbstractBfsVisitor` with the given queue.
     *
     * This method provides a mechanism to update the visitor's state by replacing the
     * current queue with a new queue, enabling custom queue configurations for BFS traversal.
     *
     * @param queue the new queue of type `Q[X]` to replace the current queue in the visitor
     * @return a new `AbstractBfsVisitor[Q, X]` instance with the updated queue
     */
    def unitQueue(queue: Q[X]): AbstractBfsVisitor[Q, X] = copy(queue = queue)

    /**
     * Creates a new `Visitor` instance with the provided updated mapAppendables.
     *
     * This method is used to update the internal state of the Visitor by creating
     * a new instance with the modified mappings from `Message` to `Appendable`.
     *
     * @param map a map containing updated associations of `Message` to `Appendable[X]`
     * @return a new `Visitor[X]` instance that reflects the updated mapAppendables
     */
    def unit(map: Map[Message, Appendable[X]]): Visitor[X] = copy(map = map)
  }

/**
 * Contains utility methods for processing recursive logic within the `BfsVisitor` class.
 *
 * This object provides an internal helper function used to facilitate specific
 * recursive operations with handling of `In` messages during traversal.
 */
object BfsVisitor {
  /**
   * Constructs a new instance of `BfsQueueableVisitor[X]` to facilitate breadth-first search traversal.
   * It doesn't make a lot of sense to set up a Post-messaged BFS visitor, so `message` is not a parameter of this
   * `create` method.
   *
   * @param journal an instance of `Appendable[X]` that maintains a collection of traversed elements.
   * @param f       a function that accepts an element of type `X` and produces a sequence of child elements for traversal.
   * @param goal    a predicate function that determines whether a given element of type `X` satisfies the search goal.
   * @tparam X the type of elements that the `BfsVisitor[X]` is designed to traverse and process.
   * @return a newly created `BfsVisitor[X]` instance configured with an empty queue, a map containing the given message and journal, and the provided traversal logic.
   */
  def create[X](journal: Appendable[X], f: X => Seq[X], goal: X => Boolean): BfsVisitor[X] =
    BfsQueueableVisitor(Queue.empty, Map(Pre -> journal), f, goal)

  /**
   * Creates a `BfsQueueableVisitor` instance for breadth-first search traversal using a queue for the journal.
   *
   * @param f    a function that accepts an element of type `X` and returns a sequence of child elements for traversal
   * @param goal a predicate function that determines whether a given element of type `X` satisfies the search goal
   * @tparam X the type of elements that the visitor operates on during the BFS traversal
   * @return a `BfsQueueableVisitor[X]` instance initialized with an empty queue and provided traversal logic
   */
  def createWithQueue[X](f: X => Seq[X], goal: X => Boolean): BfsVisitor[X] =
    create(QueueJournal.empty[X], f, goal)

  /**
   * Creates a `BfsVisitor` instance for breadth-first search traversal using a priority queue
   * that processes elements by their minimum priority, as defined by the provided `Ordering` instance.
   *
   * @param journal an `Appendable[X]` instance that collects the elements visited during the traversal
   * @param f       a function that, given an element of type `X`, produces a sequence of child elements to be traversed
   * @param goal    a predicate function that determines whether a given element of type `X` satisfies the search goal
   * @tparam X the type of elements that the BFS traversal processes, which must have an implicit `Ordering`
   * @return a `BfsVisitor[X]` instance initialized for BFS traversal with a minimum priority queue
   */
  def createByMinPriority[X: Ordering](journal: Appendable[X], f: X => Seq[X], goal: X => Boolean): BfsVisitor[X] =
    BfsQueueableVisitor(MinPQ(), Map(Pre -> journal), f, goal)

  /**
   * Creates a `BfsVisitor` instance for breadth-first search traversal using a priority queue
   * with elements prioritized by their minimum value, as defined by the given ordering.
   *
   * @param f    a function that accepts an element of type `X` and returns a sequence of child elements for traversal
   * @param goal a predicate function that determines whether a given element of type `X` satisfies the search goal
   * @tparam X the type of elements handled by this visitor, which must have an implicit `Ordering` defined
   * @return a `BfsVisitor[X]` instance configured with a priority queue for traversal, using the provided child generation function and goal predicate
   */
  def createByMinPriorityWithQueue[X: Ordering](f: X => Seq[X], goal: X => Boolean): BfsVisitor[X] =
    createByMinPriority(QueueJournal.empty[X], f, goal)

  /**
   * Creates a `BfsQueueableVisitor` instance for breadth-first search traversal using a maximum priority queue.
   *
   * This method sets up a `BfsQueueableVisitor` that traverses elements according to their priority,
   * where elements with higher priorities (as determined by the provided `Ordering`) are processed first.
   *
   * @param journal an instance of `Appendable[X]` that maintains a collection of traversed elements.
   * @param f       a function that accepts an element of type `X` and produces a sequence of child elements for traversal.
   * @param goal    a predicate function that determines whether a given element of type `X` satisfies the search goal.
   * @tparam X the type of elements that the breadth-first search will operate on.
   * @return a `BfsVisitor[X]` instance configured with a maximum priority queue for traversal.
   */
  def createByMaxPriority[X: Ordering](journal: Appendable[X], f: X => Seq[X], goal: X => Boolean): BfsVisitor[X] =
    BfsQueueableVisitor(MaxPQ(), Map(Pre -> journal), f, goal)

  /**
   * Creates a `BfsVisitor` instance for breadth-first search traversal using a priority queue
   * where elements are ordered by a maximum-priority criterion.
   *
   * @param f    a function that accepts an element of type `X` and returns a sequence of child elements for traversal
   * @param goal a predicate function that determines whether a given element of type `X` satisfies the search goal
   * @tparam X the type of elements that the visitor operates on during the BFS traversal, requiring an implicit `Ordering` for priority comparison
   * @return a `BfsVisitor[X]` instance initialized with an empty priority queue (using maximum priority) and the provided traversal logic
   */
  def createByMaxPriorityWithQueue[X: Ordering](f: X => Seq[X], goal: X => Boolean): BfsVisitor[X] =
    createByMaxPriority(QueueJournal.empty[X], f, goal)
}

/**
 * An abstract class for implementing a visitor-based breadth-first search (BFS) traversal
 * mechanism with an associated queue and mapping capabilities.
 *
 * This class provides the foundational logic to perform goal-oriented BFS traversal,
 * where the state of traversal is maintained via a queue (`Q[K]`) and results
 * are stored in a `Map[Message, Appendable[(K, V)]]`. It integrates the Visitor and
 * Queueable patterns to support extensibility and separation of concerns.
 *
 * @tparam Q the type of the queue-like container used for BFS traversal, bound by the `Queueable` typeclass
 * @tparam K the type of the keys used during BFS traversal
 * @tparam V the type of the values mapped from the keys during the traversal
 * @constructor Creates an instance of `AbstractQueueableVisitorMapped` using the provided queue, map,
 *              key-value transformation function, child node generator, and goal condition.
 * @param queue     the initial queue instance used for traversing elements
 * @param map       a mapping from `Message` to `Appendable` collections for storing traversal states
 * @param f         a function that maps a key of type `K` to a value of type `V`
 * @param children  a function that generates the child elements of a given key `K`
 * @param goal      a function that determines if a given key `K` meets the traversal goal
 * @param queueable the typeclass instance describing queue-like operations for the `Q[_]` structure
 */
abstract class AbstractQueueableVisitorMapped[Q[_], K, V](queue: Q[K], map: Map[Message, Appendable[(K, V)]], f: K => V, children: K => Seq[K], goal: K => Boolean)(using queueable: Queueable[Q]) extends AbstractVisitorMappedWithChildren[K, K, V](map, f, children) with Bfs[K, AbstractQueueableVisitorMapped[Q, K, V]] with GoalOriented[K] {

  /**
   * Performs a breadth-first search (BFS) starting with the given key `k`.
   *
   * @param k the starting key of type `K` to begin the BFS traversal
   * @return a result of type `R` which is a subtype of `Visitor[_]`, representing the outcome of the BFS traversal
   */
  def bfs(k: K): (AbstractQueueableVisitorMapped[Q, K, V], Option[K]) =
    unitQueue(queueable.offer(queue)(k)).inner


  /**
   * Recursively processes a breadth-first search (BFS) by consuming elements from the queue and
   * applying the visitor pattern to track the traversal state and results.
   *
   * This method extracts elements from the queue using `doTake`, checks if the goal condition is met,
   * and either updates the traversal state or continues processing the queue. If a goal element is found,
   * it returns the current visitor state and the goal element wrapped in an `Option`. If the queue is
   * exhausted without finding a goal element, it returns the current visitor state and `None`.
   *
   * @return a tuple where the first element is an updated instance of `AbstractBfsVisitor[Q, X]`, 
   *         and the second element is an `Option[X]` that contains the goal element if found, othersise `None`.
   */
  @tailrec
  private def inner: (AbstractQueueableVisitorMapped[Q, K, V], Option[K]) = doTake() match {
    case None =>
      (this, None)
    case Some((x, q)) if goal(x) =>
      (unitQueue(q), Some(x))
    case Some((x, q)) =>
      val visitor: AbstractQueueableVisitorMapped[Q, K, V] = unitQueue(q).visit(Pre)(x -> f(x)).asInstanceOf[AbstractQueueableVisitorMapped[Q, K, V]]
      children(x).foldLeft(visitor) { (v, x) => v.unitQueue(v.doOffer(x)) }.inner
  }

  /**
   * Constructs a new `AbstractQueueableVisitorMapped` instance with the provided queue.
   *
   * This method serves as a constructor or initializer for creating a visitor instance
   * that operates using the given `queue`. It is used to establish the visitor's
   * initial state and context for breadth-first search (BFS) traversal.
   *
   * @param queue the queue of type `Q[X]` to be used for managing BFS traversal elements
   * @return an instance of `AbstractBfsVisitor[Q, X]` initialized with the specified queue
   */
  def unitQueue(queue: Q[K]): AbstractQueueableVisitorMapped[Q, K, V]

  /**
   * Offers the given element to the queue, returning a new queue with the element added.
   *
   * This method utilizes the `queueable.offer` function to enqueue the specified element
   * into the provided queue, ensuring a pseudo-immutable operation by returning a new
   * queue instance with the updated state.
   *
   * @param x the element of type `X` to be added to the queue
   * @return a new `Q[X]` instance representing the queue after adding the given element
   */
  private def doOffer(x: K): Q[K] =
    queueable.offer(queue)(x)

  /**
   * Extracts an element from the queue if it is not empty.
   *
   * This method checks if the queue is empty using the `queueable.isEmpty` function. If the queue
   * contains elements, it retrieves the first element and the remaining queue using the `queueable.take` function
   * and wraps the result in an `Option`. If the queue is empty, it returns `None`.
   *
   * @return an `Option` containing a tuple of the extracted element and the updated queue (`(X, Q[X])`)
   *         if the queue is not empty, or `None` if the queue is empty.
   */
  private def doTake(): Option[(K, Q[K])] =
    Option.when(!queueable.isEmpty(queue))(queueable.take(queue))
}

/**
 * A case class representing a breadth-first search (BFS) visitor with mapping capabilities.
 *
 * This visitor manages a queue for BFS traversal, a mapping between `Message` instances and
 * associated `Appendable` collections, and functions for deriving values, children,
 * and goals for traversal. It extends `AbstractQueueableVisitorMapped` to provide BFS-specific behavior.
 *
 * @param queue    the queue used for managing BFS traversal elements
 * @param map      a map defining associations between `Message` instances and `Appendable` collections
 * @param f        a function that transforms elements of type `K` into elements of type `V`
 * @param children a function that determines the children of an element for BFS traversal
 * @param goal     a function that evaluates whether the goal condition is met for an element
 * @tparam K the type of elements managed in the queue and traversed during BFS
 * @tparam V the type of values derived from elements of type `K` using the function `f`
 */
case class BfsQueueVisitorMapped[K, V](queue: Queue[K], map: Map[Message, Appendable[(K, V)]], f: K => V, children: K => Seq[K], goal: K => Boolean) extends AbstractQueueableVisitorMapped(queue, map, f, children, goal) {
  /**
   * Performs a breadth-first search (BFS) starting with the given key `k`.
   *
   * @param k the starting key of type `K` to begin the BFS traversal
   * @return a result of type `R` which is a subtype of `Visitor[_]`, representing the outcome of the BFS traversal
   */
  override def bfs(k: K): (BfsQueueVisitorMapped[K, V], Option[K]) = {
    val (q, ko) = super.bfs(k)
    q.asInstanceOf[BfsQueueVisitorMapped[K, V]] -> ko
  }

  /**
   * Make a visit, with the given message and `(K, V)` value, on this `Visitor` and return a new `Visitor`.
   *
   * This method defines the behavior for handling a `Message` in the context
   * of the Visitor pattern. The implementation of this method should use the provided
   * message and state to determine the next state and return the appropriate `Visitor`.
   *
   * @param msg the message to be processed by the visitor
   * @param kv  the current state or context associated with the visitor
   * @return a new `DfsVisitorMapped[K, V]` instance that represents the updated state after processing the message
   */
  override def visit(msg: Message)(kv: (K, V)): BfsQueueVisitorMapped[K, V] =
    super.visit(msg)(kv).asInstanceOf[BfsQueueVisitorMapped[K, V]]

  /**
   * Creates a new `Visitor` instance with the provided updated mapAppendables.
   *
   * This method is used to update the internal state of the Visitor by creating
   * a new instance with the modified mappings from `Message` to `Appendable`.
   *
   * @param map a map containing updated associations of `Message` to `Appendable[(K, V)]`
   * @return a new `DfsVisitorMapped[K, V]` instance that reflects the updated mapAppendables
   */
  def unit(map: Map[Message, Appendable[(K, V)]]): BfsQueueVisitorMapped[K, V] =
    copy(map = map)

  /**
   * Constructs a new `AbstractQueueableVisitorMapped` instance with the provided queue.
   *
   * This method serves as a constructor or initializer for creating a visitor instance
   * that operates using the given `queue`. It is used to establish the visitor's
   * initial state and context for breadth-first search (BFS) traversal.
   *
   * @param queue the queue of type `Q[X]` to be used for managing BFS traversal elements
   * @return an instance of `AbstractBfsVisitor[Q, X]` initialized with the specified queue
   */
  def unitQueue(queue: Queue[K]): AbstractQueueableVisitorMapped[Queue, K, V] = copy(queue = queue)
}
