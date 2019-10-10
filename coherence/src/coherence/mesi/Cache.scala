package coherence.mesi

import coherence.bus.{BusDelegate, MessageMetadata}
import coherence.devices.{CacheOp, MemoryOp, Cache => CacheTrait}

class Cache(cacheSize: Int, associativity: Int, blockSize: Int)
    extends CacheTrait[Message, State](cacheSize, associativity, blockSize) {
  private[this] var currentCycle: Long = 0

  override def cycle(): Unit = {
    currentCycle += 1
  }

  override def memoryOpCompleted(op: MemoryOp): Unit = ???

  override def request(op: CacheOp): Unit = ???

  override def message(): Option[MessageMetadata[State]] = ???

  override def onCompleteMessage(sender: BusDelegate[State],
                                 address: Long,
                                 message: State): Unit = ???
}
