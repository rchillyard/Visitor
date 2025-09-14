package newvisitor

/**
 * A trait representing a Visitor in the context of the Visitor design pattern.
 *
 * This trait defines the contract for visiting objects of type `V`, allowing users
 * to process and potentially modify the internal state of the `Visitor` while interacting
 * with `Visitable` entities. The `visit` method enables semantic processing based on the
 * specific implementation of the `Visitor` and the provided `Visitable` context.
 *
 * @tparam V the type of the objects that the `Visitor` can visit
 * @tparam Z the type representing the internal or resulting state of the `Visitor`
 */
trait Visitor[V, Z] {
  /**
   * Visits the given object of type `V` using a `Visitable` context, and processes it
   * within the underlying `Visitor` implementation. This method modifies and updates
   * the internal state of the `Visitor` when interacting with the object.
   *
   * @param v         the object of type `V` to be visited and processed by the Visitor
   * @param visitable an implicit `Visitable[V]` that provides the visitation context
   * @return the updated `Visitor` instance of type `Visitor[V, Z]` after processing
   */
  def visit(v: V)(using visitable: Visitable[V]): Visitor[V, Z]

  /**
   * Retrieves the result of the visitor's state after visiting.
   *
   * This method returns the internal or resulting state of type `Z` that has been
   * accumulated or computed by the visitor as a result of processing various elements.
   *
   * @return the internal or resulting state of the visitor of type `Z`
   */
  def visited: Z
}

//trait IterableVisitor[H, V, Z](using hVisitable: HierarchicalVisitable[H, V]) extends Visitor[H, Z] {
//  /**
//   * Visits the given object of type `V` using a `Visitable` context, and processes it
//   * within the underlying `Visitor` implementation. This method modifies and updates
//   * the internal state of the `Visitor` when interacting with the object.
//   *
//   * @param v         the object of type `V` to be visited and processed by the Visitor
//   * @param visitable an implicit `Visitable[V]` that provides the visitation context
//   * @return the updated `Visitor` instance of type `Visitor[V, Z]` after processing
//   */
//  def visit(v: H)(using visitable: Visitable[V]): Visitor[H, Z] =
//    hVisitable.children(v).foldLeft[Visitor[H, Z]](this){
//      (accum, x) => accum.visit(x)
//    }
//
//
//  /**
//   * Retrieves the result of the visitor's state after visiting.
//   *
//   * This method returns the internal or resulting state of type `Z` that has been
//   * accumulated or computed by the visitor as a result of processing various elements.
//   *
//   * @return the internal or resulting state of the visitor of type `Z`
//   */
//  def visited: Z =
//}

/**
 * A trait for performing hierarchical traversals, such as depth-first search (DFS)
 * and breadth-first search (BFS), on structures of type `H` with elements of type `V`.
 *
 * The `HierarchicalVisitor` provides abstract traversal methods that operate on a
 * hierarchical structure using an implicit `HierarchicalVisitable` implementation,
 * defining how child elements can be accessed within the hierarchy. The result
 * produced from the traversal is of type `Z`.
 *
 * @tparam H the type representing the hierarchical structure to traverse
 * @tparam V the type of the individual elements within the hierarchical structure
 * @tparam Z the type of the result produced from the traversal
 */
trait HierarchicalVisitor[H, V, Z] {
  /**
   * Performs a depth-first traversal (DFS) of a hierarchical structure.
   *
   * @param h         the hierarchical structure of type `H` to be traversed
   * @param visitable the implicit evidence of `HierarchicalVisitable` that defines
   *                  how the hierarchical structure and its elements can be visited
   * @return a result of type `Z` produced by the DFS traversal
   */
  def dfs(h: H)(using visitable: HierarchicalVisitable[H, V]): Z

  /**
   * Performs a breadth-first search (BFS) traversal on a hierarchical structure of type `H`.
   * It operates using the provided implicit `HierarchicalVisitable` to define how
   * sub-elements of type `V` within the hierarchical structure should be visited.
   *
   * @param h         the hierarchical structure of type `H` to be traversed
   * @param visitable the implicit evidence of `HierarchicalVisitable` that provides
   *                  the context for iterating over and visiting sub-elements
   * @return a result of type `Z` derived from traversing the hierarchical structure
   */
  def bfs(h: H)(using visitable: HierarchicalVisitable[H, V]): Z
}