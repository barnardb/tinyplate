package tinyplate

import org.scalatest.diagrams.Diagrams
import org.scalatest.funspec.AnyFunSpec
import scala.collection.JavaConverters._

class AccessorTest extends AnyFunSpec with Diagrams {
  import JavaTestClasses._

  describe("""An accessor for "foo"""") {
    val foo = Accessor("foo")

    it("extracts values from Maps") {
      assert(foo(Map("faa" -> 4, "foo" -> 7, "fuu" -> 11)) == 7)
    }

    it("""Fails when the value is missing from a Map""") {
      assertThrows[NoSuchElementException](foo(Map("faa" -> 4, "fuu" -> 11)))
    }

    it("""extracts values from case class instances""") {
      case class Example(faa: Int, foo: Int, fuu: Int)

      assert(foo(Example(2, 3, 5)) == 3)
    }

    it("""Fails when class doesn't have appropriate member""") {
      case class Example(faa: Int, fuu: Int)

      assertThrows[NoSuchMethodException](foo(Example(2, 5)))
    }

    it("extracts values from normal instances of normal classes") {
      class Example(val faa: Int, val foo: Int, val fuu: Int)

      assert(foo(new Example(2, 3, 5)) == 3)
    }

    ignore("""should extract a value using an apply method—NEEDS IMPLEMENTATION""") {
      object Appliable {
        def apply(key: String): String = s"Someone applied [$key]"
      }

      assert(foo(Appliable) == "Someone applied [foo]")
    }

    ignore("""should extract a value from a java.util.Map—NEEDS IMPLEMENTATION""") {
      assert(foo(Map("faa" -> 4, "foo" -> 7, "fuu" -> 11).asJava) == 7)
    }

    ignore("""should extract a value from a Java bean—NEEDS IMPLEMENTATION""") {
      assert(foo(new Bean) == 5)
    }

    it("""should extract a value from a Java class with Scala-style method names""") {
      assert(foo(new JavaWithScalaNaming) == 5)
    }

    ignore("""should extract a value from a Java class with public fields—NEEDS IMPLEMENTATION""") {
      assert(foo(new Fields) == 5)
    }

    ignore("""should prefer X over Y and Z—What's the right behaviour?""") {
      assert(foo(new JavaDisambiguation) == ???)
    }

    it("Should not extract a value from a private field") {
      case class Example(private val foo: Int)

      intercept[IllegalAccessException](foo(Example(1)))
    }

    ignore("Should not extract a value from a protected field—NEEDS IMPLEMENTATION") {
      case class Example(protected val foo: Int)

      intercept[IllegalAccessException](foo(Example(1)))
    }

    ignore("Should not extract a value from a field with scoped privacy—NEEDS IMPLEMENTATION") {
      object Enclosing {
        case class Example(private[Enclosing] val foo: Int)
      }

      intercept[IllegalAccessException](foo(Enclosing.Example(1)))
    }
  }

  describe("Accessor chaining") {
    val model = Map(
      "foo" -> Map("bar" -> "baz"),
      "bar" -> "drinks",
      "foo.bar" -> "really?"
    )

    it("should not be used by default") {
      assert(Accessor("foo.bar")(model) == "really?")
    }

    it("""should work as normal with a trivial expression""") {
      assert(Accessor.chain("bar")(model) == "drinks")
    }

    it("""should allow the dot operator to chain accessors""") {
      assert(Accessor.chain("foo.bar")(model) == "baz")
    }
  }

}
