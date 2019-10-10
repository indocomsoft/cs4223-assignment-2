package coherence.devices

sealed trait CacheOp

object CacheOp {
  case class Load(address: Long) extends CacheOp
  case class Store(address: Long) extends CacheOp
}
