package coherence.devices

trait CacheDelegate {

  /**
    * Call only after the state in the cache line is updated.
    */
  def requestCompleted(op: CacheOp)
}
