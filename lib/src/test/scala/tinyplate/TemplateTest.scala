package tinyplate

import org.scalatest.funspec.AnyFunSpec

class TemplateTest extends AnyFunSpec {
  describe("Template") {

    it("renders literal templates") {
      assert(Template("this is some literal text")(null) == "this is some literal text")
    }

    it("renders accessors") {
      assert(Template("{{foo.bar}}")(Map("foo" -> Map("bar" -> "baz"))) == "baz")
    }

    it("fails if an accessor can't be resolved") {
      // TODO custom exception
      assertThrows[RuntimeException](Template("{{whoops}}")(Map("foo" -> Map("bar" -> "baz"))))
    }

    it("allows literals and accessors to be interspersed") {
      assert(Template("Look {{foo}} at {{bar}}{{baz}} this")(Map("foo" -> 1, "bar" -> 3, "baz" -> 4)) == "Look 1 at 34 this")
    }

    it("allows sections to be repeated") {
      assert(Template("Repeated{{start words}} _{{toString}}_{{end words}}")(Map("words" -> Seq("cat", "bat", "schmoigen"))) == "Repeated _cat_ _bat_ _schmoigen_")
    }

    ignore("allows sections to be repeated with Java collections") {
      assert(Template("Repeated{{start words}} _{{toString}}_{{end words}}")(Map("words" -> java.util.Arrays.asList("cat", "bat", "schmoigen"))) == "Repeated _cat_ _bat_ _schmoigen_")
    }

    it("fails if a template has a `start` without a matching `end`") {
      // TODO custom exception
      assertThrows[RuntimeException](Template("{{start foo}}")(Map("foo" -> Seq(1))))
    }

    it("fails if a template has an `end` without a matching `start`") {
      // TODO custom exception
      assertThrows[RuntimeException](Template("{{start foo}}")(Map("foo" -> Seq(1))))
    }

    it("fails if a template's `start` and `end` blocks don't nest properly") {
      // TODO custom exception
      assertThrows[RuntimeException](Template("{{start foo}}{{start bar}}{{end foo}}{{end bar}}")(Map("foo" -> Seq(1), "bar" -> Seq(2))))
    }
  }
}
