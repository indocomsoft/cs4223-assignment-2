package coherence.mesi

import coherence.Address
import coherence.bus.{Bus, BusDelegate, MessageMetadata, ReplyMetadata}
import coherence.devices.{Memory => AbstractMemory}

class Memory(bus: Bus[Message, Reply], blockSize: Int)
    extends AbstractMemory[State, Message, Reply](bus, blockSize) {
  override def cycle(): Unit = {
    currentCycle += 1
    maybeAddress match {
      case Some((address, finishedCycle)) =>
        if (currentCycle == finishedCycle) {
          bus.reply(this, ReplyMetadata(Reply.FlushOpt(), blockSize))
          maybeAddress = None
        }
      case None =>
        ()
    }
  }

  override def busAccessGranted(): MessageMetadata[Message] =
    throw new RuntimeException("Memory: unexpected busAccessGranted()")

  override def onBusCompleteMessage(sender: BusDelegate[Message, Reply],
                                    address: Address,
                                    message: Message): Unit = {
    require(maybeAddress.isEmpty)
    message match {
      case Message.BusUpgr() => ()
      case Message.BusRd() | Message.BusRdX() =>
        maybeAddress = Some((address, currentCycle + AbstractMemory.Latency))
    }
  }

  override def onBusCompleteResponse(
    sender: BusDelegate[Message, Reply],
    address: Address,
    reply: Reply,
    originalSender: BusDelegate[Message, Reply],
    originalMessage: _root_.coherence.mesi.Message
  ): Unit = reply match {
    case Reply.FlushOpt() | Reply.Flush() =>
      maybeAddress match {
        case Some((currentAddress, _)) =>
          require(currentAddress == address)
          maybeAddress = None
        case None =>
          throw new RuntimeException(
            "Memory: received FlushOpt() while maybeAddress is None"
          )
      }
    case Reply.MemoryRead() | Reply.WriteBackOk() =>
      throw new RuntimeException(s"Memory: unexpected reply $reply")
  }

}
