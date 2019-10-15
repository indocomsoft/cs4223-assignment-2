package coherence

import scala.annotation.elidable

object Debug {
  final val Debug = 1
  final val NoDebug = -1

  @elidable(NoDebug) @inline final def println_debug(x: Any): Unit =
    println(x)
}
