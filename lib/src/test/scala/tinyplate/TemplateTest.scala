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

    it("changes the context object when starting an `Option` section") {
      val template = Template("{{a}}{{start b}} and {{a}}{{end b}}")

      assert(template(Map("a" -> "Something", "b" -> None)) == "Something")
      assert(template(Map("a" -> "Something", "b" -> Some(Map("a" -> "another thing")))) == "Something and another thing")
    }

    it("keeps the same context object when starting a `Boolean` section") {
      val template = Template("{{start p}}{{v}}{{end p}}")

      assert(template(Map("p" -> false, "v" -> "value from outer context")) == "")
      assert(template(Map("p" -> true, "v" -> "value from outer context")) == "value from outer context")
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

    it("fails if it doesn't know how to handle the value for a `start`/`end` block") {
      // TODO custom exception
      assertThrows[RuntimeException](Template("{{start foo}}{{end foo}}")(Map("foo" -> "whoops")))
    }

    it("allows the regular expression used for tags to be parameterised") {
      val template = Template("This [->word<-] if you want", tagPattern = "\\[->([^<]+)<-]".r.pattern)

      assert(template(Map("word" -> "works")) == "This works if you want")
    }
  }
}
