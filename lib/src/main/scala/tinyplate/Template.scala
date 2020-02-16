package tinyplate

import java.util.regex.Pattern

import scala.util.matching.Regex

object Template {
  val DefaultTagPattern: Pattern = "\\{\\{([^}]+)\\}\\}".r.pattern
  val KeywordValue: Regex = "([^} ]+) ([^}]+)".r

  def literal(value: String): Template = _ => value

  def dynamic(accessor: Accessor, format: PartialFunction[Any, String]): Template =
    value => format.applyOrElse(accessor(value), (v: Any) => v.toString)

  def apply(template: String, format: PartialFunction[Any, String] = PartialFunction.empty, tagPattern: Pattern = DefaultTagPattern): Template = {
    val matcher = tagPattern.matcher(template)
    var position = 0

    def buildTemplate(innermostSection: Option[String]): Template = {
      val chunks = Seq.newBuilder[Template]
      while (matcher.find()) {
        chunks += literal(template.substring(position, matcher.start()))
        matcher.group(1) match {
          case KeywordValue(keyword, value) => keyword match {
            case "start" =>
              position = matcher.end()
              val innerTemplate = buildTemplate(Some(value))
              chunks += dynamic(Accessor.chain(value), {
                case i: Iterable[_] => i.iterator.map(innerTemplate(_)).mkString
              })
            case "end" =>
              if (innermostSection.contains(value)) return v => chunks.result().map(_(v)).mkString
              else throw new IllegalArgumentException(s"""Expected to encounter ${innermostSection.fold("the end of the template")(s => s"{{end $s}}")}, but encountered ${matcher.group()}""")
          }
          case accessor => chunks += dynamic(Accessor.chain(accessor), format)
        }
        position = matcher.end()
      }
      innermostSection.foreach(s => throw new IllegalArgumentException(s"""The end of the template was reached, but expected to find {{end $s}}"""))
      chunks += literal(template.substring(position, template.length))
      v => chunks.result().map(_(v)).mkString
    }
    buildTemplate(None)
  }
}
