package coherence.devices

trait CacheDelegate {
  def requestCompleted(op: CacheOp)
}
