//package newvisitor
//
//import org.scalatest.flatspec.AnyFlatSpec
//import org.scalatest.matchers.should.Matchers
//
//class VisitorSpec extends AnyFlatSpec with Matchers {
//
//  behavior of "Visitor"
//  
//  case class Person(name: String, age: Int)
//
//  given Visitable[Person] with {
//
//    /**
//     * Processes a `Visitable` entity of type `V` using a `Visitor` and a specified `Message`.
//     *
//     * This method represents the core operation in the Visitor pattern, allowing the `Visitor`
//     * to interact with and potentially modify the `Visitable` entity. It facilitates the delegation
//     * of processing logic to the `Visitor` based on the semantics of the provided `Message`.
//     *
//     * @param message a `Message` instance defining the context or type of visitation
//     *                that should be performed by the `Visitor`
//     * @param visitor a `Visitor` implementation that processes the visited entity and
//     *                may retain or alter its own internal state during this interaction
//     * @param v       the current state of the `Visitable` entity being processed by the `Visitor`
//     * @tparam Z the type of result produced by the `Visitor` as part of its state management
//     * @return a tuple containing the updated state of the `Visitable` entity (type `V`) and
//     *         the updated `Visitor` instance (type `Visitor[V, Z]`) after the visitation
//     */
//    def visit[Z](visitor: Visitor[Person, Z])(v: Person): Visitor[Person, Z] = visitor.visit(v)
//    
//    def evaluate[Z](v: Person): Option[Z] = None
//  }
//  
//  class PrintVisitor[V] extends Visitor[V, Unit] {
//
//    /**
//     * Visits the given instance of type `V` using the provided `Visitable` context and returns the
//     * result of the visitation process along with a potentially updated `Visitor`.
//     *
//     * This method allows for processing of an entity of type `V` through the Visitor pattern,
//     * enabling operations defined in the `Visitor` implementation to be applied to the entity.
//     *
//     * @param k         the instance of type `V` to be visited
//     * @param visitable the implicit evidence of the `Visitable` context for type `V`
//     *                  that provides traversal and visitation behavior
//     * @return a tuple containing the result of the visitation process of type `Z`
//     *         and the updated `Visitor` instance
//     */
//    def visit(k: V)(using visitable: Visitable[V]): Visitor[V, Unit] = {
//      println(s"visiting $k")
//      this
//    }
//
//    /**
//     * Checks whether the visitor is in an open state.
//     *
//     * This method represents the state of the visitor, indicating whether
//     * it is open for processing or interaction. The specific definition of
//     * "open" may vary depending on the implementation context.
//     *
//     * @return true if the visitor is in an open state, false otherwise
//     */
//    def open: Boolean = true
//
//    /**
//     * Retrieves the result of the visitor's state after visiting.
//     *
//     * This method returns the internal or resulting state of type `Z` that has been
//     * accumulated or computed by the visitor as a result of processing various elements.
//     *
//     * @return the internal or resulting state of the visitor of type `Z`
//     */
//    def visited: (V, Unit) = ???
//  }
//
//  it should "visit" in {
//    // Create a visitor that has the behavior of writing a visited element to standard output
//    val visitor: Visitor[Person, Unit] = new PrintVisitor[Person]
//    val robin = Person("Robin Hillyard", 99)
//    visitor.visit(robin)
//  }
//
//}
