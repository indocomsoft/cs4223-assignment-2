package coherence.devices

sealed trait CacheOp {
  val address: Long
}

object CacheOp {
  case class Load(address: Long) extends CacheOp
  case class Store(address: Long) extends CacheOp
}
