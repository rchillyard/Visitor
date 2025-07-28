package com.phasmidsoftware.visitor

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
trait Dfs[K, R <: Visitor[_]] {

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
