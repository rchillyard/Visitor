package newvisitor

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class VisitorSpec extends AnyFlatSpec with Matchers {

  behavior of "Visitor"
  
  case class Person(name: String, age: Int)

  given Visitable[Person] with {
    def visit[Z](v: Person, visitor: Visitor[Person, Z]): (Z, Visitor[Person, Z]) = visitor.visit(v)

    def subvisits[Y](v: Person)(using Visitable[Y]): Iterator[Y] = 
      Iterator.empty
  }
  
  class PrintVisitor[V] extends Visitor[V, Unit] {
    def visit(v: V)(using visitable: Visitable[V]): (Unit, Visitor[V, Unit]) =
      println(v.toString) -> this
  }

  it should "visit" in {
    // Create a visitor that has the behavior of writing a visited element to standard output
    val visitor: Visitor[Person, Unit] = new PrintVisitor[Person]
    val robin = Person("Robin Hillyard", 99)
    visitor.visit(robin)
  }

}
