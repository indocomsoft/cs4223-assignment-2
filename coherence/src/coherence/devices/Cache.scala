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

  sealed trait CacheState
  sealed trait ActiveCacheState extends CacheState {
    val sender: CacheDelegate
    val op: CacheOp
  }
  sealed trait EvictCacheState extends ActiveCacheState {
    val address: Address
  }

  object CacheState {
    case class Ready() extends CacheState
    case class WaitingForBus(sender: CacheDelegate, op: CacheOp)
        extends ActiveCacheState
    case class WaitingForReplies(sender: CacheDelegate, op: CacheOp)
        extends ActiveCacheState
    case class WaitingForWriteback(sender: CacheDelegate, op: CacheOp)
        extends ActiveCacheState
    case class WaitingForResult(sender: CacheDelegate,
                                op: CacheOp,
                                finishedCycle: Long)
        extends ActiveCacheState
    case class WaitingForBusUpgrPropagation(sender: CacheDelegate, op: CacheOp)
        extends ActiveCacheState
    case class EvictWaitingForBus(address: Address,
                                  sender: CacheDelegate,
                                  op: CacheOp)
        extends EvictCacheState
    case class EvictWaitingForWriteback(address: Address,
                                        sender: CacheDelegate,
                                        op: CacheOp)
        extends EvictCacheState
  }

  val numBlocks = cacheSize / blockSize
  val numSets = numBlocks / associativity
  val offsetBits = Integer.numberOfTrailingZeros(blockSize)
  val setIndexOffsetBits = Integer.numberOfTrailingZeros(numSets)
  val sets: Array[LRUCache[State]] =
    (1 to numSets).map(_ => new LRUCache[State](associativity)).toArray

  protected var currentCycle: Long = 0
  protected var state: CacheState = CacheState.Ready()
  protected var maybeReply: Option[Reply] = None

  bus.addBusDelegate(this)

  def request(sender: CacheDelegate, op: CacheOp): Unit

  override def cycle(): Unit = {
    currentCycle += 1
    state match {
      case CacheState.WaitingForResult(sender, op, finishedCycle)
          if (finishedCycle == currentCycle) =>
        sender.requestCompleted(op)
        state = CacheState.Ready()
      case _ =>
        maybeReply match {
          case None => ()
          case Some(reply) =>
            if (bus.reply(this, ReplyMetadata(reply, blockSize))) {
              maybeReply = None
            }
        }
    }
  }

  override def onBusTransactionEnd(sender: BusDelegate[Message, Reply],
                                   address: Address,
                                   message: Message): Unit = {
    maybeReply = None
  }

  override def toString: String = s"Cache $id"

  protected def toAddress(address: Long): Address =
    Address(address, offsetBits, setIndexOffsetBits)
}
