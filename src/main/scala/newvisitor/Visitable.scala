//package newvisitor
//
//import com.phasmidsoftware.visitor.Message
//
//import scala.collection.immutable.Queue
//import scala.reflect.ClassTag
//
///**
// * A trait representing a type that can be visited using the Visitor design pattern.
// *
// * The `Visitable` trait defines objects of type `V` that can be processed by a `Visitor`. 
// * It provides an abstraction for implementing custom visitation logic, allowing the 
// * separation of object processing from the objects themselves.
// *
// * @tparam V the type of elements that this `Visitable` can accept for visitation
// */
//trait Visitable[V] {
//
//  /**
//   * Evaluates the provided element of type `V` and returns a tuple containing the 
//   * input element and an optional value of type `Z`.
//   *
//   * @param v the element of type `V` to be evaluated
//   * @return a tuple consisting of the input element `V` and an optional result of type `Z`
//   */
//  def evaluate[Z](v: V): (V, Option[Z])
//}
//
///**
// * A trait that extends the `Visitable` trait and adds functionality for revisiting.
// *
// * The `Revisitable` trait allows checking whether objects of type `V` have been
// * visited in the context of the Visitor design pattern. This trait is useful when
// * managing traversal or visitation of objects where state tracking of visited elements
// * is necessary.
// *
// * It leverages the `Visitable` functionality while introducing the ability to query
// * the visitation status of specific objects.
// *
// * @tparam V the type of elements that this `Revisitable` can accept for visitation
// */
//trait Revisitable[V] extends Visitable[V] {
//
//  /**
//   * Determines whether the specified object of type `V` has been visited.
//   *
//   * This method checks if the given object `v` has already been processed or traversed
//   * as part of the visitation context.
//   *
//   * @param v the object of type `V` to check for visitation status
//   * @return true if the object has been visited, false otherwise
//   */
//  def isVisited(v: V): Boolean
//}
//
///**
// * A trait representing a hierarchical structure that can be visited and traversed.
// *
// * The `HierarchicalVisitable` trait defines an abstraction for structures where elements 
// * of type `V` can be iterated over within a parent context of type `H`. It provides a 
// * mechanism for accessing child elements while leveraging the visitor pattern through the 
// * implicit `Visitable` evidence. It also supports revisitation functionality through the 
// * implicit `Revisitable[V]`.
// *
// * @tparam H the type representing the hierarchical structure to be visited
// * @tparam V the type representing the elements within the hierarchical structure
// */
//trait HierarchicalVisitable[H, V] {
//
//  /**
//   * Retrieves an iterator over the child elements of a hierarchical structure.
//   *
//   * This method provides access to the child elements of type `V` within a given 
//   * hierarchical context of type `H`, enabling iteration through the elements based 
//   * on the implicit `Visitable` evidence.
//   *
//   * @param h         the hierarchical structure of type `H` whose child elements are to be retrieved
//   * @param visitable the implicit `Visitable[V]` that provides context for visiting 
//   *                  and processing the child elements
//   * @return an iterator over the child elements of type `V` within the given hierarchical structure
//   */
//  def children(h: H)(using visitable: Visitable[V]): Iterator[V]
//}
//
///**
// * A trait representing a hierarchical structure that can be visited and traversed.
// *
// * The `HierarchicalVisitable` trait defines an abstraction for structures where elements
// * of type `V` can be iterated over within a parent context of type `H`. It provides a
// * mechanism for accessing child elements while leveraging the visitor pattern through the
// * implicit `Visitable` evidence. It also supports revisitation functionality through the
// * implicit `Revisitable[V]`.
// *
// * @tparam H the type representing the hierarchical structure to be visited
// * @tparam V the type representing the elements within the hierarchical structure
// */
//trait HierarchicalRevisitableVisitable[H, V](using Revisitable[V]) extends HierarchicalVisitable[H, V]
//
///**
// * A trait representing an iterable collection that supports visitation of its elements.
// *
// * `VisitableIterable` extends the `HierarchicalVisitable` trait, providing functionality
// * for traversing and visiting elements of an iterable collection. This trait allows elements
// * of type `A` within a collection of type `Iterable[A]` to be accessed and processed using
// * the visitor pattern, leveraging implicit evidence of a `Visitable[A]` context.
// *
// * @tparam A the type of elements contained in the iterable collection
// */
//trait VisitableIterable[A] extends HierarchicalVisitable[Iterable[A], A] {
//  
//  /**
//   * CONSIDER does this belong more properly in Visitor?
//   * 
//   * Processes the elements of the given iterable using the provided `Visitable` context
//   * and returns an iterable of tuples, where each tuple contains the original element
//   * and an optional computed result.
//   *
//   * This method first retrieves the elements of the iterable using the `children` method.
//   * It then applies the `evaluate` method of the implicit `Visitable` instance to each
//   * element to produce the final iterable of tuples.
//   *
//   * @param ai the iterable of elements of type `A` to be processed
//   * @param av the implicit `Visitable[A]` instance that provides the evaluation logic
//   * @return an iterable of tuples, each containing an element of type `A` and an optional
//   *         result of type `Z`
//   */
//  def journal[Z](ai: Iterable[A])(using av: Visitable[A]): Iterable[(A, Option[Z])] = 
//    children(ai).toSeq map av.evaluate[Z]
//}
//
///**
// * Companion object for the `Visitable` trait. It provides specialized implementations of av behavior.
// *
// * This object includes predefined implementations of the `Visitable` trait for specific types, ensuring consistency and reusability.
// */
//object Visitable {
//    trait VisitableString extends Visitable[String] {
//      /**
//       * Processes a `Visitable` entity of type `V` using a `Visitor` and a specified `Message`.
//       *
//       * This method represents the core operation in the Visitor pattern, allowing the `Visitor`
//       * to interact with and potentially modify the `Visitable` entity. It facilitates the delegation
//       * of processing logic to the `Visitor` based on the semantics of the provided `Message`.
//       *
//       * @param message a `Message` instance defining the context or type of visitation
//       *                that should be performed by the `Visitor`
//       * @param visitor a `Visitor` implementation that processes the visited entity and
//       *                may retain or alter its own internal state during this interaction
//       * @param v       the current state of the `Visitable` entity being processed by the `Visitor`
//       * @tparam Z the type of result produced by the `Visitor` as part of its state management
//       * @return a tuple containing the updated state of the `Visitable` entity (type `V`) and
//       *         the updated `Visitor` instance (type `Visitor[V, Z]`) after the visitation
//       */
//      def visit[Z](visitor: Visitor[String, Z])(v: String): Visitor[String, Z] =
//        visitor.visit(v)
//
//      /**
//       * Attempts to process the given value of type `V` and return an optional result of type `Z`.
//       *
//       * This method provides a mechanism to optionally transform or extract a value from the input
//       * based on the implementation details, returning `None` if the operation is not applicable
//       * or fails to produce a result.
//       *
//       * @param v the input value of type `V` to be processed
//       * @return an `Option` containing a result of type `Z` if the transformation or extraction is successful, or `None` otherwise
//       */
//      def evaluate[Z](v: String): Option[Z] = None
//    }
//    implicit object VisitableString extends VisitableString
//    
//}
