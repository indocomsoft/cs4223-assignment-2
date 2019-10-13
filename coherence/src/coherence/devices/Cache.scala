package coherence.devices

import coherence.bus.{Bus, BusDelegate}
import coherence.cache.LRUCache

object Cache {
  val HitLatency = 1
}

abstract class Cache[State, Message](val id: Int,
                                     cacheSize: Int,
                                     associativity: Int,
                                     blockSize: Int,
                                     val bus: Bus[Message])
    extends Device
    with BusDelegate[Message] {

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
          if finishedCycle != -1 && currentCycle > finishedCycle =>
        sender.requestCompleted(op)
        this.op = None
      case _ =>
        ()
    }
    debug
  }

  private[this] def debug: Unit = op match {
    case Some(opm @ OpMetadata(_, op, _)) =>
      val (tag, setIndex, _) = calculateAddress(op.address)
      println(s"Cache $id: tag $tag, setIndex $setIndex, op $opm")
    case v @ None => println(s"Cache $id: op $v")
  }

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

  override def toString: String = s"Cache $id"
}
