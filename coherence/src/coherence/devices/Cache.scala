package coherence.devices

import coherence.Address
import coherence.bus.{Bus, BusDelegate}
import coherence.cache.LRUCache

object Cache {
  val HitLatency = 1
}

abstract class Cache[State, Message, Reply](val id: Int,
                                            cacheSize: Int,
                                            associativity: Int,
                                            blockSize: Int,
                                            val bus: Bus[Message, Reply])
    extends Device
    with BusDelegate[Message, Reply] {

  /**
    * @param finishedCycle -1 if still pending
    */
  case class OpMetadata(sender: CacheDelegate, op: CacheOp, finishedCycle: Long)

  val numBlocks = cacheSize / blockSize
  val numSets = numBlocks / associativity
  val offsetBits = Integer.numberOfLeadingZeros(blockSize)
  val setIndexOffsetBits = Integer.numberOfTrailingZeros(numSets)
  val sets: Array[LRUCache[State]] =
    (1 to numSets).map(_ => new LRUCache[State](associativity)).toArray

  protected var currentCycle: Long = 0
  // Long is finishedCycle, -1 if still pending
  protected var op: Option[OpMetadata] = None

  bus.addBusDelegate(this)

  def request(sender: CacheDelegate, op: CacheOp): Unit

  override def cycle(): Unit = {
    currentCycle += 1
    op match {
      case Some(OpMetadata(sender, op, finishedCycle))
          if currentCycle == finishedCycle =>
        sender.requestCompleted(op)
        this.op = None
      case _ =>
        ()
    }
  }

  override def toString: String = s"Cache $id"
}
