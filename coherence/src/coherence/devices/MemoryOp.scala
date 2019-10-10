package coherence.devices

sealed trait MemoryOp

object MemoryOp {
  case object Read extends MemoryOp
  case object Write extends MemoryOp
}
