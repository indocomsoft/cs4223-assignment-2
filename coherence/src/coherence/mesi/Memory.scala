package coherence.mesi

import coherence.bus.{Bus, BusDelegate, MessageMetadata}
import coherence.devices.{Memory => AbstractMemory}
import coherence.devices.Memory.{Op => MemoryOp}

import scala.collection.mutable

class Memory(bus: Bus[Message], blockSize: Int)
    extends AbstractMemory[State, Message](bus, blockSize) {
  private[this] val flushOptQueue = mutable.Queue[Long]()

  override def cycle(): Unit = {
    currentCycle += 1
    requests
      .collect {
        case (k @ MemoryOp.Read(_), finishedCycle)
            if currentCycle > finishedCycle =>
          k
      }
      .foreach {
        case k @ MemoryOp.Read(address) =>
          requests.remove(k)
          flushOptQueue.enqueue(address)
      }
    if (flushOptQueue.nonEmpty) bus.requestAccess(this)
  }

  override def message(): Option[MessageMetadata[Message]] = {
    var maybeAddress: Option[Long] = None
    while (flushOptQueue.nonEmpty && maybeAddress.isEmpty) {
      val address = flushOptQueue.dequeue()
      if (!bus.isShared(address)) maybeAddress = Some(address)
    }
    maybeAddress.map { address =>
      MessageMetadata(Message.FlushOpt(), address, blockSize)
    }
  }

  override def onCompleteMessage(sender: BusDelegate[Message],
                                 address: Long,
                                 message: Message): Unit = {
    if (sender.eq(this)) {
      ()
    } else {
      message match {
        case Message.BusUpgr()                  => ()
        case Message.BusRd() | Message.BusRdX() => read(address)
        case Message.FlushOpt() | Message.Flush()
            if requests.contains(MemoryOp.Read(address)) =>
          requests.remove(MemoryOp.Read(address))
      }
    }
  }
}
