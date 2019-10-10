package coherence.devices

import coherence.bus.BusDelegate
import coherence.cache.LRUCache

abstract class Cache[State, Message](cacheSize: Int,
                                     associativity: Int,
                                     blockSize: Int)
    extends Device
    with BusDelegate[Message]
    with MemoryDelegate {
  val numBlocks = cacheSize / blockSize
  val numSets = numBlocks / associativity
  val offsetBits = Integer.numberOfLeadingZeros(blockSize)
  val setIndexOffsetBits = Integer.numberOfTrailingZeros(numSets)
  val sets: Array[LRUCache[State]] =
    (1 to numSets).map(_ => new LRUCache[State](associativity)).toArray

  def request(op: CacheOp): Unit
}
