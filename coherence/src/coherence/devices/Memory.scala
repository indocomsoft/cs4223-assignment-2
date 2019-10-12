package coherence.devices

import coherence.bus.{Bus, BusDelegate, MessageMetadata}

import scala.collection.mutable

object Memory {
  val Latency = 100

  sealed trait Op
  object Op {
    case class Read(address: Long) extends Op
    case class Write(address: Long) extends Op
  }

}

abstract class Memory[State, Message](bus: Bus[Message],
                                      protected val blockSize: Int)
    extends Device
    with BusDelegate[Message] {

  bus.addBusDelegate(this)

  protected var currentCycle: Long = 0
  // A map to finishedCycle
  protected val requests = mutable.Map[Memory.Op, Long]()

  // Always return false so as not to affect the OR line
  override def hasCopy(address: Long): Boolean = false

  protected def read(address: Long): Unit = {
    val op = Memory.Op.Read(address)
    if (!requests.contains(op))
      requests.put(Memory.Op.Read(address), currentCycle + Memory.Latency)
  }
}
