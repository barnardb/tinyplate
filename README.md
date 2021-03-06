[![License](https://img.shields.io/github/license/barnardb/tinyplate)](https://github.com/barnardb/tinyplate/blob/master/LICENSE)
[![Continuous Integration](https://github.com/barnardb/tinyplate/workflows/CI/badge.svg)](https://github.com/barnardb/tinyplate/actions?query=workflow%3ACI)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.barnardb/tinyplate_2.13)](https://search.maven.org/search?q=g:io.github.barnardb%20a:tinyplate_*)


Tinyplate
=========

A tiny Scala template engine.

- [TL;DR](#tldr)
- [Story](#story)
- [Security](#security)


TL;DR
-----

```scala
import tinyplate.Template

val template: Template = Template("This is a {{subject.name}} {{subject.type}}")
// template: Function1[Any, String] = tinyplate.Template$$$Lambda$2361/958030392@3a0f5141

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
{{if changes}}
<p>It comes with the following changes:
  <ul>
  {{start changes}}
    <li class="{{type}}">{{description}}</li>
  {{end changes}}
  </ul>
</p>
{{fi changes}}
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
// 
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
// 
// """
```

Wow, that was easy!

Oh, you forgot to use the timestamp though. Well, let's have another go at the first line of the release announcement:

```scala
tinyplate.Template("<h1>{{metadata.timestamp}} {{metadata.title}}</h1>")(model)
// res4: String = "<h1>1970-01-01 01:00:00.0 Feature-Complete!</h1>"
```

Hmm, you don't really want the full time in there, do you? No worries, you can specify how some types should be formatted by passing a partial function when you create the template:

```scala
tinyplate.Template("<h1>{{metadata.timestamp}} {{metadata.title}}</h1>", {
  case ts: java.sql.Timestamp => ts.toLocalDateTime.toLocalDate.toString
})(model)
// res5: String = "<h1>1970-01-01 Feature-Complete!</h1>"
```

Ah, much better!

But wait, what if you make a mistake in the template?

```scala
tinyplate.Template("This is version {{meta.versoin}}.")(Map(
  "meta" -> Map(
    "version" -> 7
  )
))
// tinyplate.TemplateException: Error at tag {{meta.versoin}}: error invoking accessor
// 	at tinyplate.Template$.liftedTree1$1(Template.scala:17)
// 	at tinyplate.Template$.dynamic$$anonfun$1(Template.scala:17)
// 	at tinyplate.Template$.buildTemplate$12$$anonfun$11$$anonfun$1(Template.scala:65)
// 	at scala.collection.immutable.List.map(List.scala:250)
// 	at scala.collection.immutable.List.map(List.scala:79)
// 	at tinyplate.Template$.buildTemplate$13$$anonfun$12(Template.scala:65)
// 	at repl.MdocSession$App0.$init$$$anonfun$1(README.md:142)
// Caused by: java.util.NoSuchElementException: key not found: versoin
// 	at scala.collection.immutable.Map$Map1.apply(Map.scala:245)
// 	at tinyplate.Accessor$.apply$$anonfun$1(Accessor.scala:5)
// 	at scala.Function1.$anonfun$andThen$1(Function1.scala:85)
// 	at tinyplate.Template$.liftedTree1$1(Template.scala:16)
// 	... 6 more
```

Phew, safe!


Security
--------

Note that Tinyplate templates can invoke any method with no arguments accessible from your model object. Tinyplate should only be used with trusted templates that you write yourself. It should not be used with user-editable templates.
