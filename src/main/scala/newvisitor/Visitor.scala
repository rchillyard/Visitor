package newvisitor

/**
 * Represents a generic Visitor in the Visitor pattern capable of traversing
 * and processing objects of type `V` within a visitable context. The processing
 * results in an output of type `Z` and potentially updates the Visitor’s state.
 *
 * @tparam V the type of objects this Visitor can process
 * @tparam Z the type of result produced by the Visitor for each processed object
 */
trait Visitor[V, Z] {
  /**
   * Visits the given instance of type `V` using the provided `Visitable` context and returns the 
   * result of the visitation process along with a potentially updated `Visitor`.
   *
   * This method allows for processing of an entity of type `V` through the Visitor pattern, 
   * enabling operations defined in the `Visitor` implementation to be applied to the entity.
   *
   * @param v         the instance of type `V` to be visited
   * @param visitable the implicit evidence of the `Visitable` context for type `V`
   *                  that provides traversal and visitation behavior
   * @return a tuple containing the result of the visitation process of type `Z`
   *         and the updated `Visitor` instance
   */
  def visit(v: V)(using visitable: Visitable[V]): (Z, Visitor[V, Z])

  //  def dfs[X, Y](x: X)(using Visitable[X]): Seq[Y] = implicitly[Visitable[X]].subvisits(x).foldLeft(Seq.empty){
  //    c => implicitly[Visitable[X]].visit(c, this)
  //  }
}