package tinyplate

class TemplateException(tag: String, message: String, cause: Throwable = null)
  extends RuntimeException(s"Error at tag $tag: $message", cause)
