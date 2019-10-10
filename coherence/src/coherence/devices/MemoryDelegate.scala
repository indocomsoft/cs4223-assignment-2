package coherence.devices

trait MemoryDelegate {
  def memoryOpCompleted(op: MemoryOp, address: Long)
}
