package coherence.devices

sealed trait MemoryOp

object MemoryOp {
  case class Read(address: Long) extends MemoryOp
  case class Write(address: Long) extends MemoryOp
}
