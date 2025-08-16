package com.phasmidsoftware.visitor

/**
 * Represents a generic visitation strategy.
 *
 * This trait defines the contract for a strategy that operates on a
 *
 * Implementations of this trait should define specific strategies for processing keys and
 * other logic in collaboration with the provided visitor.
 *
 * CONSIDER removing the parametric types K and R as we don't currently actually need them but we might need them in the future.
 *
 * @tparam K the type of the key parameter handled by this strategy
 * @tparam R the type of the visitor that this strategy operates with, constrained to a subtype of `Visitor[_]`
 */
trait Strategy[K, R <: Visitor[_]]

/**
 * Represents a generic Depth-First Search (DFS) traversal strategy for processing elements of type `K`
 * and updating the state of a visitor of type `R`.
 *
 * This trait is designed to encapsulate the DFS traversal logic, where each element of type `K` is processed
 * recursively. The processing flow supports state changes in the visitor of type `R`, allowing for pre-, in-,
 * and post-visit behavior to be implemented as part of the visitor pattern.
 *
 * @tparam K the type of elements to be traversed using DFS.
 * @tparam R the type of the visitor, which must extend the `Visitor` trait and defines the logic
 *           for state updates during traversal.
 */
trait Dfs[K, R <: Visitor[_]] extends Strategy[K, R] {

  /**
   * Executes a Depth-First Search (DFS) starting from the specified element `k`.
   * The traversal follows a recursive strategy, where each visited element may invoke
   * updates to the visiting state encapsulated in type `R`.
   *
   * @param k the starting element of type `K` for the DFS traversal
   * @return an updated instance of type `R` that reflects the visitor's state after the traversal
   */
  def dfs(k: K): R
}

/**
 * Represents a breadth-first search (BFS) strategy that operates on a key of type `K` and
 * interacts with a visitor of type `R`, where `R` is a subtype of `Visitor[_]`.
 *
 * This trait extends the `Strategy` trait, inheriting its contract for key and visitor-based processing.
 * It defines methods for performing a BFS traversal and evaluating whether a goal condition
 * is satisfied based on the result of the traversal.
 *
 * @tparam K the type of the key parameter used in the BFS traversal
 * @tparam R the type of the visitor utilized by the BFS strategy, which is constrained to a subtype of `Visitor[_]`
 */
trait Bfs[K, R <: Visitor[_]] extends Strategy[K, R] {
  /**
   * Performs a breadth-first search (BFS) starting with the given key `k`.
   *
   * @param k the starting key of type `K` to begin the BFS traversal
   * @return a result of type `R` which is a subtype of `Visitor[_]`, representing the outcome of the BFS traversal
   */
  def bfs(k: K): (R, Option[K])
}

/**
 * A trait representing goal-oriented behavior for generic types.
 *
 * The `GoalOriented` trait is meant to define a goal function that can evaluate
 * whether a given result satisfies a specific goal condition.
 *
 * @tparam K the type of the input to the goal function
 */
trait GoalOriented[K] {

  /**
   * Evaluates whether the given result `r` satisfies the goal condition.
   *
   * @return a goal function
   */
  val goal: K => Boolean
}
