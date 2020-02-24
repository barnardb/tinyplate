package tinyplate

object Accessor {
  def apply(name: String): Accessor = {
    case map: Map[_, _] => map.asInstanceOf[Map[String, _]](name)
    case o => o.getClass.getDeclaredMethod(name).invoke(o)
  }

  def chain(chain: String): Accessor =
    chain match {
      case "." => identity
      case _ => chain.split('.').map(apply).reduce(_ andThen _)
    }
}
