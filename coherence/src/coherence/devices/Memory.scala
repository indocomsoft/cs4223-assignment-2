package coherence.devices

import coherence.Address
import coherence.bus.{Bus, BusDelegate, ReplyMetadata}

import scala.collection.mutable

object Memory {
  val Latency = 100
}

abstract class Memory[State, Message, Reply](bus: Bus[Message, Reply],
                                             protected val blockSize: Int)
    extends Device
    with BusDelegate[Message, Reply] {

  bus.addBusDelegate(this)

  protected var currentCycle: Long = 0
  // Tuple of reply and when operation is finished
  protected var maybeReply: Option[(Reply, Long)] = None

  override def cycle(): Unit = {
    currentCycle += 1
    maybeReply match {
      case Some((reply, finishedCycle)) =>
        if (currentCycle == finishedCycle) {
          bus.reply(this, ReplyMetadata(reply, blockSize))
          maybeReply = None
        }
      case None =>
        ()
    }
  }

  // Always return false so as not to affect the OR line
  override def hasCopy(address: Address): Boolean = false

  override def onBusTransactionEnd(sender: BusDelegate[Message, Reply],
                                   address: Address,
                                   message: Message): Unit = {
    maybeReply = None
  }
}
