//package newvisitor
//
//import com.phasmidsoftware.visitor.{Appendable, Message, SelfVisit}
//
//import scala.reflect.ClassTag
//
///**
// * A trait representing a Visitor in the context of the Visitor design pattern.
// *
// * This trait defines the contract for visiting objects of type `V`, allowing users
// * to process and potentially modify the internal state of the `Visitor` while interacting
// * with `Visitable` entities. The `visit` method enables semantic processing based on the
// * specific implementation of the `Visitor` and the provided `Visitable` context.
// *
// * @tparam K the type of the objects that the `Visitor` can visit
// * @tparam X the type representing the internal or resulting state of the `Visitor`
// */
//trait Visitor[K, +X] {
//  /**
//   * Checks whether the visitor is in an open state.
//   *
//   * This method represents the state of the visitor, indicating whether
//   * it is open for processing or interaction. The specific definition of
//   * "open" may vary depending on the implementation context.
//   *
//   * @return true if the visitor is in an open state, false otherwise
//   */
//  def open: Boolean
//  
//  /**
//   * Visits the given object of type `K` using the provided implicit `Visitable` context 
//   * and returns the resulting `Visitor` instance.
//   *
//   * This method allows processing of an object `h` in conjunction with a `Visitable` instance.
//   * The visitation logic, defined by the implicit `Visitable`, enables customizable interaction 
//   * with the object of type `K` and may result in modifications to the internal state of the 
//   * `Visitor` or other side effects as defined by the specific implementation.
//   *
//   * @param k         the object of type `K` to be visited
//   * @param visitable an implicit `Visitable` instance for the type `K` providing the logic for visitation
//   * @return the updated `Visitor` instance resulting from the visitation of the object `h`
//   */
//  def visit(k: K)(using visitable: Visitable[K]): Visitor[K, X]
//
//  /**
//   * Retrieves the result of the visitor's state after visiting.
//   *
//   * This method returns the internal or resulting state of type `Z` that has been
//   * accumulated or computed by the visitor as a result of processing various elements.
//   *
//   * @return the internal or resulting state of the visitor of type `Z`
//   */
//  def visited: (K, X)
//}
//
///**
// * A specialized type of `Visitor` that processes a single element.
// *
// * This trait aligns with the Visitor design pattern, enabling the traversal
// * and processing of elements represented as key-value pairs, where the value 
// * component is encapsulated in an `Option`. The concrete implementation defines 
// * the specific behavior for handling these optional values during visitation.
// *
// * @tparam K the type of the key associated with the elements to be visited
// * @tparam V the type of the optional value associated with the elements to be visited
// */
//trait ElementVisitor[K, +V] extends Visitor[K, Option[V]]:
//  def visit(k: K)(using visitable: Visitable[K]): ElementVisitor[K, V]
//
///**
// * Represents a visited element, characterized by a key and an optional value, within the context of the Visitor design pattern.
// *
// * This case class implements the `ElementVisitor` trait, enabling it to process elements represented
// * as key-value pairs. The optional value encapsulates the possibility of the value being absent or present.
// *
// * @tparam K the type of the key associated with the visited element
// * @tparam V the type of the optional value associated with the visited element
// * @param k  the key representing the unique identifier of the visited element
// * @param vo an optional value associated with the visited element 
// */
//case class VisitedElement[K, +V](k: K, vo: Option[V]) extends ElementVisitor[K, V] {
//  /**
//   * Checks whether the visitor is in an open state.
//   *
//   * This method represents the state of the visitor, indicating whether
//   * it is open for processing or interaction. The specific definition of
//   * "open" may vary depending on the implementation context.
//   *
//   * @return true if the visitor is in an open state, false otherwise
//   */
//  def open: Boolean = false
//    
//  /**
//   * Retrieves the result of the visitor's state after visiting.
//   *
//   * This method returns the internal or resulting state of type `Z` that has been
//   * accumulated or computed by the visitor as a result of processing various elements.
//   *
//   * @return the internal or resulting state of the visitor of type `Z`
//   */
//  def visited: (K, Option[V]) = (k, vo)
//
//  /**
//   * Visits an element identified by the given key.
//   *
//   * This method attempts to visit an element using the key of type `K` within the context of the 
//   * `Visitable` typeclass. If an element cannot be visited, it throws an exception with an appropriate 
//   * error message.
//   *
//   * @param k         the key representing the element to be visited
//   * @param visitable a given instance of the `Visitable` typeclass for processing elements of type `K`
//   * @return an updated `ElementVisitor[K, V]` instance after attempting to visit the specified key
//   * @throws IllegalArgumentException if the specified key cannot be visited
//   */
//  def visit(k: K)(using visitable: Visitable[K]): ElementVisitor[K, V] = 
//    throw new IllegalArgumentException(s"cannot visit $k")
//}
//
///**
// * A case class representing an unvisited element in the visitor pattern implementation.
// *
// * The `UnvisitedElement` class extends `ElementVisitor` and represents the state of an unvisited
// * element in a processing sequence. It provides mechanisms to track and transition from the 
// * unvisited state, typically transitioning to a visited state upon invocation of certain methods.
// *
// * @tparam K the type of the key associated with the unvisited element
// * @tparam V the type of the optional value associated with the unvisited element
// */
//case class UnvisitedElement[K, +V]() extends ElementVisitor[K, V] {
//  /**
//   * Checks whether the visitor is in an open state.
//   *
//   * This method represents the state of the visitor, indicating whether
//   * it is open for processing or interaction. The specific definition of
//   * "open" may vary depending on the implementation context.
//   *
//   * @return true if the visitor is in an open state, false otherwise
//   */
//  def open: Boolean = true
//
//  /**
//   * Visits the given object of type `V` using a `Visitable` context, and processes it
//   * within the underlying `Visitor` implementation. This method modifies and updates
//   * the internal state of the `Visitor` when interacting with the object.
//   *
//   * @param k         the object of type `V` to be visited and processed by the Visitor
//   * @param visitable an implicit `Visitable[V]` that provides the visitation context
//   * @return the updated `Visitor` instance of type `Visitor[V, Z]` after processing
//   */
//  def visit(k: K)(using visitable: Visitable[K]): ElementVisitor[K, V] =
//    VisitedElement.apply[K,V].tupled(visitable.evaluate(k))
//
//  /**
//   * Retrieves the result of the visitor's state after visiting.
//   *
//   * This method returns the internal or resulting state of type `Z` that has been
//   * accumulated or computed by the visitor as a result of processing various elements.
//   *
//   * @return the internal or resulting state of the visitor of type `Z`
//   */
//  def visited: (K, Option[V]) = 
//    throw new IllegalStateException("cannot visit unvisited")
//}
//
///**
// * A trait representing a specialized visitor that operates on hierarchical structures with iterable children.
// *
// * `IterableVisitor` extends the visitor pattern functionality by integrating the ability to process iterable
// * child elements of a given hierarchical structure. The visited results are maintained in an internal journal that 
// * appends information from visited elements, enabling the visitor to accumulate and track the state throughout the 
// * traversal process.
// *
// * This trait depends on an `Appendable` instance to facilitate the journaling mechanism, as well as implicit evidence 
// * provided by `Visitable` for the visitation logic and `HierarchicalVisitable` for iterating and accessing children of 
// * the hierarchical structure.
// *
// * @tparam H the type of the hierarchical structure being visited
// * @tparam K the type of the elements that are iterable within the hierarchical structure
// * @tparam V the type of the value associated with the visited elements
// * @tparam J the type representing the state or result accumulated by the visitor
// *
// */
//trait HVisitor[H, K, V, J <: Appendable[(K, Option[V])]]
//(journal: J)
//(using visitable: Visitable[K]) 
//  extends Visitor[H, J] {
//
//  /**
//   * Retrieves the result of the visitor's state after visiting.
//   *
//   * This method returns the internal or resulting state of type `Z` that has been
//   * accumulated or computed by the visitor as a result of processing various elements.
//   *
//   * @return the internal or resulting state of the visitor of type `Z`
//   */
//  def visited: (H, J)
//}
//
//case class IterableVisitor[H, K, V, J <: Appendable[(K, Option[V])]]
//  (journal: J)
//  (using visitable: Visitable[K])
//    extends HVisitor[H, K, V, J](journal) {
//  /**
//   * Retrieves the result of the visitor's state after visiting.
//   *
//   * This method returns the internal or resulting state of type `Z` that has been
//   * accumulated or computed by the visitor as a result of processing various elements.
//   *
//   * @return the internal or resulting state of the visitor of type `Z`
//   */
//  def visited: (H, J) = ???
//
//  /**
//   * Checks whether the visitor is in an open state.
//   *
//   * This method represents the state of the visitor, indicating whether
//   * it is open for processing or interaction. The specific definition of
//   * "open" may vary depending on the implementation context.
//   *
//   * @return true if the visitor is in an open state, false otherwise
//   */
//  def open: Boolean = true
//
//  /**
//   * Visits the given object of type `K` using the provided implicit `Visitable` context 
//   * and returns the resulting `Visitor` instance.
//   *
//   * This method allows processing of an object `h` in conjunction with a `Visitable` instance.
//   * The visitation logic, defined by the implicit `Visitable`, enables customizable interaction 
//   * with the object of type `K` and may result in modifications to the internal state of the 
//   * `Visitor` or other side effects as defined by the specific implementation.
//   *
//   * @param h         the object of type `K` to be visited
//   * @param hv an implicit `Visitable` instance for the type `K` providing the logic for visitation
//   * @return the updated `Visitor` instance resulting from the visitation of the object `h`
//   */
//  def visit(h: H)(using hv: HierarchicalVisitable[H, K], visitable: Visitable[K]): IterableVisitor[H, K, V, J] = {
//    val w: Iterator[K] = hv.children(h)
//    val q: (K, Option[V]) = visitable.evaluate(h)
//  }
//
//  /**
//   * Visits the child elements of the given hierarchical structure using a visitor pattern
//   * and updates the state of the visitor with the results.
//   *
//   * This method processes the iterable child elements of the hierarchical structure `ai`
//   * by applying the given implicit `HierarchicalVisitable` instance, which defines how 
//   * to access and iterate over the child elements. The results of the visitation are appended 
//   * to the internal journal within the visitor.
//   *
//   * @param h          the hierarchical structure of type `H` to be visited
//   * @param hVisitable an implicit `HierarchicalVisitable` instance providing the logic 
//   *                   to retrieve and traverse the child elements of `ai`
//   * @return a new `Visitor[H, J]` instance where the visitation results have been updated
//   */
//  def visitIterable(h: H)(using hVisitable: HierarchicalVisitable[H, K]): Visitor[H, J] = {
//    val visitor: ElementVisitor[K, V] = UnvisitedElement[K, V]()
//    val result: ElementVisitor[K, V] = hVisitable.children(h).foldLeft[ElementVisitor[K, V]](visitor) {
//      (accum, x) => accum.visit(x)
//    }
//    copy(journal = journal.append(visitor.visited))
//  }
//}
///**
// * A trait for performing hierarchical traversals, such as depth-first search (DFS)
// * and breadth-first search (BFS), on structures of type `H` with elements of type `V`.
// *
// * The `HierarchicalVisitor` provides abstract traversal methods that operate on a
// * hierarchical structure using an implicit `HierarchicalVisitable` implementation,
// * defining how child elements can be accessed within the hierarchy. The result
// * produced from the traversal is of type `Z`.
// *
// * @tparam H the type representing the hierarchical structure to traverse
// * @tparam V the type of the individual elements within the hierarchical structure
// * @tparam Z the type of the result produced from the traversal
// */
//trait HierarchicalVisitor[H, V, Z] {
//  /**
//   * Performs a depth-first traversal (DFS) of a hierarchical structure.
//   *
//   * @param h         the hierarchical structure of type `H` to be traversed
//   * @param visitable the implicit evidence of `HierarchicalVisitable` that defines
//   *                  how the hierarchical structure and its elements can be visited
//   * @return a result of type `Z` produced by the DFS traversal
//   */
//  def dfs(h: H)(using visitable: HierarchicalVisitable[H, V]): Z
//
//  /**
//   * Performs a breadth-first search (BFS) traversal on a hierarchical structure of type `H`.
//   * It operates using the provided implicit `HierarchicalVisitable` to define how
//   * sub-elements of type `V` within the hierarchical structure should be visited.
//   *
//   * @param h         the hierarchical structure of type `H` to be traversed
//   * @param visitable the implicit evidence of `HierarchicalVisitable` that provides
//   *                  the context for iterating over and visiting sub-elements
//   * @return a result of type `Z` derived from traversing the hierarchical structure
//   */
//  def bfs(h: H)(using visitable: HierarchicalVisitable[H, V]): Z
//}