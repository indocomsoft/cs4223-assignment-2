package coherence.devices

import coherence.bus.{Bus, BusDelegate}
import coherence.cache.LRUCache

abstract class Cache[State, Message](cacheSize: Int,
                                     associativity: Int,
                                     blockSize: Int,
                                     val memory: Memory,
                                     val bus: Bus[Message])
    extends Device
    with BusDelegate[Message]
    with MemoryDelegate {
  val numBlocks = cacheSize / blockSize
  val numSets = numBlocks / associativity
  val offsetBits = Integer.numberOfLeadingZeros(blockSize)
  val setIndexOffsetBits = Integer.numberOfTrailingZeros(numSets)
  val sets: Array[LRUCache[State]] =
    (1 to numSets).map(_ => new LRUCache[State](associativity)).toArray

  bus.addBusDelegate(this)

  def request(sender: CacheDelegate, op: CacheOp): Unit

  /**
    * @return (tag, setIndex, offset)
    */
  def calculateAddress(address: Long): (Int, Int, Int) = {
    var tmp = address
    val offset = tmp & ((1 << offsetBits) - 1)
    tmp = tmp >> offsetBits
    val setIndex = tmp & ((1 << setIndexOffsetBits) - 1)
    tmp = tmp >> setIndexOffsetBits
    (tmp.toInt, setIndex.toInt, offset.toInt)
  }
}
