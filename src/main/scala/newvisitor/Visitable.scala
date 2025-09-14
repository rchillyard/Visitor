package newvisitor

import com.phasmidsoftware.visitor.Message

import scala.collection.immutable.Queue

/**
 * A trait representing a type that can be visited using the Visitor design pattern.
 *
 * The `Visitable` trait defines objects of type `V` that can be processed by a `Visitor`. 
 * It provides an abstraction for implementing custom visitation logic, allowing the 
 * separation of object processing from the objects themselves.
 *
 * @tparam V the type of elements that this `Visitable` can accept for visitation
 */
trait Visitable[V] {

  /**
   * Visits a given object of type `V` using the specified `Visitor` and returns the resulting `Visitor` instance.
   *
   * This method delegates the visitation logic to the provided `Visitor`, enabling type-safe and customizable
   * processing of the object `v` as part of the Visitor pattern. The visitor may modify its internal state 
   * or perform actions based on the visited object's context.
   *
   * @param visitor the `Visitor` instance of type `Visitor[V, Z]` used to visit and process the object
   * @param v       the object of type `V` to be visited
   * @return the updated `Visitor[V, Z]` instance after processing the object `v`
   */
  def visit[Z](visitor: Visitor[V, Z])(v: V): Visitor[V, Z]
}

/**
 * A trait that extends the `Visitable` trait and adds functionality for revisiting.
 *
 * The `Revisitable` trait allows checking whether objects of type `V` have been
 * visited in the context of the Visitor design pattern. This trait is useful when
 * managing traversal or visitation of objects where state tracking of visited elements
 * is necessary.
 *
 * It leverages the `Visitable` functionality while introducing the ability to query
 * the visitation status of specific objects.
 *
 * @tparam V the type of elements that this `Revisitable` can accept for visitation
 */
trait Revisitable[V] extends Visitable[V] {

  /**
   * Determines whether the specified object of type `V` has been visited.
   *
   * This method checks if the given object `v` has already been processed or traversed
   * as part of the visitation context.
   *
   * @param v the object of type `V` to check for visitation status
   * @return true if the object has been visited, false otherwise
   */
  def isVisited(v: V): Boolean
}

/**
 * A trait representing a hierarchical structure that can be visited and traversed.
 *
 * The `HierarchicalVisitable` trait defines an abstraction for structures where elements 
 * of type `V` can be iterated over within a parent context of type `H`. It provides a 
 * mechanism for accessing child elements while leveraging the visitor pattern through the 
 * implicit `Visitable` evidence. It also supports revisitation functionality through the 
 * implicit `Revisitable[V]`.
 *
 * @tparam H the type representing the hierarchical structure to be visited
 * @tparam V the type representing the elements within the hierarchical structure
 */
trait HierarchicalVisitable[H, V](using Revisitable[V]) {

  /**
   * Retrieves an iterator over the child elements of a hierarchical structure.
   *
   * This method provides access to the child elements of type `V` within a given 
   * hierarchical context of type `H`, enabling iteration through the elements based 
   * on the implicit `Visitable` evidence.
   *
   * @param h         the hierarchical structure of type `H` whose child elements are to be retrieved
   * @param visitable the implicit `Visitable[V]` that provides context for visiting 
   *                  and processing the child elements
   * @return an iterator over the child elements of type `V` within the given hierarchical structure
   */
  def children(h: H)(using visitable: Visitable[V]): Iterator[V]
}

/**
 * A trait representing an iterable collection that supports visitation of its elements.
 *
 * `VisitableIterable` extends the `HierarchicalVisitable` trait, providing functionality
 * for traversing and visiting elements of an iterable collection. This trait allows elements
 * of type `A` within a collection of type `Iterable[A]` to be accessed and processed using
 * the visitor pattern, leveraging implicit evidence of a `Visitable[A]` context.
 *
 * @tparam A the type of elements contained in the iterable collection
 */
trait VisitableIterable[A] extends HierarchicalVisitable[Iterable[A], A] {
  /**
   * Retrieves an iterator over the elements of a given iterable.
   *
   * This method provides access to the elements of type `A` contained in the provided
   * iterable, enabling iteration through them while leveraging implicit evidence of
   * the `Visitable` context.
   *
   * @param h         the iterable of type `Iterable[A]` whose elements are to be retrieved
   * @param visitable the implicit `Visitable[A]` that provides context for visiting and
   *                  processing the elements
   * @return an iterator over the elements of type `A` within the given iterable
   */
  def children(h: Iterable[A])(using visitable: Visitable[A]): Iterator[A] = h.iterator
}

/**
 * Companion object for the `Visitable` trait. It provides specialized implementations of visitable behavior.
 *
 * This object includes predefined implementations of the `Visitable` trait for specific types, ensuring consistency and reusability.
 */
object Visitable {
    trait VisitableString extends Visitable[String] {
      /**
       * Processes a `Visitable` entity of type `V` using a `Visitor` and a specified `Message`.
       *
       * This method represents the core operation in the Visitor pattern, allowing the `Visitor`
       * to interact with and potentially modify the `Visitable` entity. It facilitates the delegation
       * of processing logic to the `Visitor` based on the semantics of the provided `Message`.
       *
       * @param message a `Message` instance defining the context or type of visitation
       *                that should be performed by the `Visitor`
       * @param visitor a `Visitor` implementation that processes the visited entity and
       *                may retain or alter its own internal state during this interaction
       * @param v       the current state of the `Visitable` entity being processed by the `Visitor`
       * @tparam Z the type of result produced by the `Visitor` as part of its state management
       * @return a tuple containing the updated state of the `Visitable` entity (type `V`) and
       *         the updated `Visitor` instance (type `Visitor[V, Z]`) after the visitation
       */
      def visit[Z](visitor: Visitor[String, Z])(v: String): Visitor[String, Z] =
        visitor.visit(v)
    }
    implicit object VisitableString extends VisitableString
}
