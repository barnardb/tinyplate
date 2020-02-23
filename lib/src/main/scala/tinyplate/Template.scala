package tinyplate

import java.util.regex.Pattern

import scala.util.control.NonFatal
import scala.util.matching.Regex

object Template {
  private val DefaultTagPattern: Pattern = "\\{\\{([^}]+)\\}\\}".r.pattern
  private val KeywordValue: Regex = "([^} ]+) ([^}]+)".r

  private def literal(value: String): Template = _ => value

  private def dynamic(tag: String, accessor: Accessor, format: PartialFunction[Any, String]): Template =
    value => format.applyOrElse(
      try accessor(value)
      catch { case NonFatal(e) => throw new TemplateException(tag, "error invoking accessor", e) },
      (_: Any).toString
    )

  def apply(template: String, format: PartialFunction[Any, String] = PartialFunction.empty, tagPattern: Pattern = DefaultTagPattern): Template = {
    val matcher = tagPattern.matcher(template)
    var position = 0

    def buildTemplate(innermostSection: Option[String]): Template = {
      val chunks = Seq.newBuilder[Template]
      while (matcher.find()) {
        val tag = matcher.group()
        chunks += literal(template.substring(position, matcher.start()))
        matcher.group(1) match {
          case KeywordValue(keyword, expression) => keyword match {
            case "start" =>
              position = matcher.end()
              val accessor = Accessor.chain(expression)
              val innerTemplate = buildTemplate(Some(expression))
              chunks += (context => accessor(context) match {
                case i: Iterable[_] => i.iterator.map(innerTemplate(_)).mkString
                case o: Option[_] => o.map(innerTemplate(_)).getOrElse("")
                case p: Boolean => if (p) innerTemplate(context) else ""
                case null => throw new TemplateException(tag, "start expression evaluated to null")
                case v => throw new TemplateException(tag, s"start expression evaluated to value of class ${v.getClass}: $v")
              })
            case "end" =>
              if (innermostSection.contains(expression)) return v => chunks.result().map(_(v)).mkString
              else throw new TemplateException(tag, s"unexpected end tag: ${innermostSection.fold("currently at the top level of the template")(startExpression => s"innermost start tag expression is $startExpression")}")
          }
          case expression => chunks += dynamic(tag, Accessor.chain(expression), format)
        }
        position = matcher.end()
      }
      innermostSection.foreach(s => throw new TemplateException("EOF", s"""The end of the template was reached, but expected to find {{end $s}}"""))
      chunks += literal(template.substring(position, template.length))
      v => chunks.result().map(_(v)).mkString
    }
    buildTemplate(None)
  }
}
