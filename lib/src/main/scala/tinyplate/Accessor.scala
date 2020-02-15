package tinyplate

import scala.util.control.NonFatal

object Accessor {
  def apply(name: String): Accessor = {
    case map: Map[_, _] => map.asInstanceOf[Map[String, _]](name)
    case o => o.getClass.getDeclaredMethod(name).invoke(o)
  }

  def parse(chain: String): Accessor =
    wrapExceptions(chain, chain.split('.').map(apply).reduce(_ andThen _))

  def wrapExceptions(repr: String, raw: Accessor): Accessor =
    v => try raw(v) catch { case NonFatal(e) => throw new RuntimeException(s"Error using accessor: $repr", e)}
}
