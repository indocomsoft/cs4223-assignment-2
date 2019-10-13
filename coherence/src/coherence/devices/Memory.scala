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
  // Tuple of Address to when read is finished
  protected var maybeAddress: Option[(Address, Long)] = None

  // Always return false so as not to affect the OR line
  override def hasCopy(address: Address): Boolean = false
}
