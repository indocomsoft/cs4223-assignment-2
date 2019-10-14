package coherence.devices

import coherence.Address
import coherence.bus.{Bus, BusDelegate, ReplyMetadata}
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

  case class OpMetadata(sender: CacheDelegate,
                        op: CacheOp,
                        maybeFinishedCycle: Option[Long],
                        busAccess: Boolean)

  val numBlocks = cacheSize / blockSize
  val numSets = numBlocks / associativity
  val offsetBits = Integer.numberOfLeadingZeros(blockSize)
  val setIndexOffsetBits = Integer.numberOfTrailingZeros(numSets)
  val sets: Array[LRUCache[State]] =
    (1 to numSets).map(_ => new LRUCache[State](associativity)).toArray

  protected var currentCycle: Long = 0
  protected var op: Option[OpMetadata] = None
  protected var maybeReply: Option[Reply] = None

  bus.addBusDelegate(this)

  def request(sender: CacheDelegate, op: CacheOp): Unit

  override def cycle(): Unit = {
    currentCycle += 1
    op match {
      case Some(OpMetadata(sender, op, Some(finishedCycle), _)) =>
        if (currentCycle == finishedCycle) {
          sender.requestCompleted(op)
          this.op = None
        }
      case _ =>
        maybeReply match {
          case Some(reply) =>
            if (bus.reply(this, ReplyMetadata(reply, blockSize)))
              maybeReply = None
          case None => ()
        }
    }
  }

  override def onBusTransactionEnd(sender: BusDelegate[Message, Reply],
                                   address: Address,
                                   message: Message): Unit = {
    maybeReply = None
  }

  override def toString: String = s"Cache $id"
}
