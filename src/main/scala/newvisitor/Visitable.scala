package newvisitor

/**
 * Represents an entity that can be visited by a `Visitor` as part of the Visitor pattern.
 *
 * This trait defines a contract for classes that want to implement visitable behavior.
 * It relies on a generic type parameter `V`, which must itself extend `Visitable[V]`.
 * This ensures type-safety and allows flexibility in defining visitable entities, while
 * enabling compile-time checks for Visitor pattern implementations.
 *
 * The `visit` method is central to the operation of the Visitor pattern, delegating processing
 * responsibility to a provided `Visitor`. Implementers of this trait specify the behavior of how
 * a `Visitor` interacts with their objects.
 *
 * @tparam V the type of `Visitable` that conforms to this trait, ensuring compatibility
 *           with visitors and enabling type-safe processing
 */
trait Visitable[V] {

  /**
   * Delegates the visitation of a `Visitable` entity to a specified `Visitor`.
   *
   * This method implements a central component of the Visitor pattern by allowing
   * the `Visitor` to operate on the current instance, processing it and returning
   * both a new state of the `Visitable` entity and the updated `Visitor`.
   *
   * @param v the `Visitor` instance that performs operations on this `Visitable`
   *          and may maintain or alter its own state during the visitation process
   * @return a tuple containing the updated state of the `Visitable` (of type `V`)
   *         as well as the updated `Visitor` instance after processing
   */
  def visit[Z](v: V, visitor: Visitor[V, Z]): (Z, Visitor[V, Z])

  /**
   * Returns an iterator over a collection of sub-elements of type `Y` obtained from the visitable entity `v`.
   * This method uses the implicit evidence of `Visitable[Y]` to determine how to traverse and extract elements.
   *
   * @param v the visitable entity of type `V` from which the sub-elements are derived
   * @return an iterator containing the sub-elements of type `Y`
   */
  def subvisits[Y: Visitable](v: V): Iterator[Y]
}

/**
 * Companion object for the `Visitable` trait. It provides specialized implementations of visitable behavior.
 *
 * This object includes predefined implementations of the `Visitable` trait for specific types, ensuring consistency and reusability.
 */
object Visitable {
  //  trait VisitableString extends Visitable[String] {
  //    def visit[Z](v: String, visitor: Visitor): (Z, Visitor) = visitor.visit(this)
  //
  //    def subvisits[Y](v: String)(using Visitable[Y]): Iterator[Y] = Iterator.empty
  //
  //  }
  //  implicit object VisitableString extends VisitableString
}
