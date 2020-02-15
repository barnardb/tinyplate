Tinyplate
=========

A tiny Scala template engine.

TL;DR
-----

```scala
import tinyplate.Template

val template: Template = Template("This is a {{subject.name}} {{subject.type}}")
// template: Any => String = tinyplate.Template$$$Lambda$1289/0x0000000801833840@604f376a

val result = template(Map(
  "object" -> "success",
  "subject" -> Map(
    "name" -> "tinyplate",
    "type" -> "template"
  )
))
// result: String = "This is a tinyplate template"

case class Foo(description: String, subject: Animal)
case class Animal(name: String, `type`: Type)
sealed trait Type
case object Cat extends Type
case object Dog extends Type

val anotherResult = template(Foo("yep", Animal("Snuggles", Cat)))
// anotherResult: String = "This is a Snuggles Cat"
```

Story
-----

You're working on a Scala project and need to render a template from within your code. You've read the template from a file or resource or database or something: 

```scala
import java.nio.file._
val templateString = new String(Files.readAllBytes(Paths.get("release.template.html")))
```
 
Let's say it looks like this:

```html
<h1>{{metadata.title}}</h1>
<p>We're releasing version {{version}}.</p>
<p>It comes with the following changes:
  <ul>
  {{start changes}}
    <li class="{{type}}">{{description}}</li>
  {{end changes}}
  </ul>
</p>
```

How will you render this? Maybe you've done a quick search and found bloated libraries like [scalate](https://github.com/scalate/scalate) that you couldn't quickly get working out of the box. Or you tried some classics like [StringTemplate](https://www.stringtemplate.org/) or [mustache.java](https://github.com/spullara/mustache.java), but their behaviour of silently ignoring typos in tags like `{{versoin}}` (that aren't in your model) makes you cringe. You want something simple. You want it to fail when you've messed up, rather than silently move on.

You want Tinyplate:

```scala
val template = tinyplate.Template(templateString)
```

Now you're ready to render your model.

Let's say your model type is a mix of ADTs and `Map[String, _]`s:

```scala
case class Release(metadata: Map[String, Any], version: Int, changes: Seq[Change])

case class Change(`type`: Change.Type, description: String)

object Change {
  sealed trait Type
  case object Feature extends Type
  case object Fix extends Type
}
```

You've got a model value (having generated it, or loaded it from JSON or YAML or something) equivalent to this:

```scala
import Change._

val model = Release(
  metadata = Map(
    "timestamp" -> new java.sql.Timestamp(0),
    "title" -> "Feature-Complete!"
  ),
  version = 42,
  changes = Seq(
    Change(Feature, "Foobar compatible whatsit processing"),
    Change(Feature, "Automatic mome rath detection"),
    Change(Fix, "Crash-on-startup bug introduced in version 41 now only happens on blue moons")
  )
)
// model: Release = Release(
//   Map("timestamp" -> 1970-01-01 01:00:00.0, "title" -> "Feature-Complete!"),
//   42,
//   List(
//     Change(Feature, "Foobar compatible whatsit processing"),
//     Change(Feature, "Automatic mome rath detection"),
//     Change(
//       Fix,
//       "Crash-on-startup bug introduced in version 41 now only happens on blue moons"
//     )
//   )
// )
```

You render it:

```scala
val rendered = template(model)
// rendered: String = """<h1>Feature-Complete!</h1>
// <p>We're releasing version 42.</p>
// <p>It comes with the following changes:
//   <ul>
//   
//     <li class="Feature">Foobar compatible whatsit processing</li>
//   
//     <li class="Feature">Automatic mome rath detection</li>
//   
//     <li class="Fix">Crash-on-startup bug introduced in version 41 now only happens on blue moons</li>
//   
//   </ul>
// </p>
// """
```

Done!

Oh, but what if you make a mistake?

```scala
tinyplate.Template("This is version {{meta.versoin}}.")(Map(
  "meta" -> Map(
    "version" -> 7
  )
))
// java.lang.RuntimeException: Error using accessor: meta.versoin
// 	at tinyplate.Accessor$.$anonfun$wrapExceptions$1(Accessor.scala:15)
// 	at tinyplate.Template$.$anonfun$dynamic$1(Template.scala:14)
// 	at tinyplate.Template$.$anonfun$apply$7(Template.scala:42)
// 	at scala.collection.immutable.List.map(List.scala:223)
// 	at scala.collection.immutable.List.map(List.scala:79)
// 	at tinyplate.Template$.$anonfun$apply$6(Template.scala:42)
// 	at repl.Session$App0$$anonfun$13.apply(README.md:119)
// 	at repl.Session$App0$$anonfun$13.apply(README.md:117)
// Caused by: java.util.NoSuchElementException: key not found: versoin
// 	at scala.collection.immutable.Map$Map1.apply(Map.scala:240)
// 	at tinyplate.Accessor$.$anonfun$apply$1(Accessor.scala:7)
// 	at scala.Function1.$anonfun$andThen$1(Function1.scala:85)
// 	... 8 more
```

Phew, safe!
