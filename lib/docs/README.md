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

```scala mdoc
import tinyplate.Template

val template: Template = Template("This is a {{subject.name}} {{subject.type}}")

val result = template(Map(
  "object" -> "success",
  "subject" -> Map(
    "name" -> "tinyplate",
    "type" -> "template"
  )
))

case class Foo(description: String, subject: Animal)
case class Animal(name: String, `type`: Type)
sealed trait Type
case object Cat extends Type
case object Dog extends Type

val anotherResult = template(Foo("yep", Animal("Snuggles", Cat)))
```


Story
-----

You're working on a Scala project and need to render a template from within your code. You've read the template from a file or resource or database or something: 

```scala mdoc:reset:silent
import java.nio.file._
val templateString = new String(Files.readAllBytes(Paths.get("release.template.html")))
```
 
Let's say it looks like this:

```scala mdoc:passthrough
println("```html")
print(templateString)
println("```")
```

How will you render this? Maybe you've done a quick search and found bloated libraries like [scalate](https://github.com/scalate/scalate) that you couldn't quickly get working out of the box. Or you tried some classics like [StringTemplate](https://www.stringtemplate.org/) or [mustache.java](https://github.com/spullara/mustache.java), but their behaviour of silently ignoring typos in tags like `{{versoin}}` (that aren't in your model) makes you cringe. You want something simple. You want it to fail when you've messed up, rather than silently move on.

You want Tinyplate:

```scala mdoc:silent
val template = tinyplate.Template(templateString)
```

Now you're ready to render your model.

Let's say your model type is a mix of ADTs and `Map[String, _]`s:

```scala mdoc
case class Release(metadata: Map[String, Any], version: Int, changes: Seq[Change])

case class Change(`type`: Change.Type, description: String)

object Change {
  sealed trait Type
  case object Feature extends Type
  case object Fix extends Type
}
```

You've got a model value (having generated it, or loaded it from JSON or YAML or something) equivalent to this:

```scala mdoc
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
```

You render it:

```scala mdoc
val rendered = template(model)
```

Wow, that was easy!

Oh, you forgot to use the timestamp though. Well, let's have another go at the first line of the release announcement:

```scala mdoc
tinyplate.Template("<h1>{{metadata.timestamp}} {{metadata.title}}</h1>")(model)
```

Hmm, you don't really want the full time in there, do you? No worries, you can specify how some types should be formatted by passing a partial function when you create the template:

```scala mdoc
tinyplate.Template("<h1>{{metadata.timestamp}} {{metadata.title}}</h1>", {
  case ts: java.sql.Timestamp => ts.toLocalDateTime.toLocalDate.toString
})(model)
```

Ah, much better!

But wait, what if you make a mistake in the template?

```scala mdoc:crash
tinyplate.Template("This is version {{meta.versoin}}.")(Map(
  "meta" -> Map(
    "version" -> 7
  )
))
```

Phew, safe!


Security
--------

Note that Tinyplate templates can invoke any method with no arguments accessible from your model object. Tinyplate should only be used with trusted templates that you write yourself. It should not be used with user-editable templates.
