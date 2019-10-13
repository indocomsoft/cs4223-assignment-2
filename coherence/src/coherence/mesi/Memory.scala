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

  override def message(): MessageMetadata[Message] =
    throw new RuntimeException("Unexpected message() on Memory")

  override def onCompleteMessage(sender: BusDelegate[Message, Reply],
                                 address: Address,
                                 message: Message): Unit = {
    require(maybeAddress.isEmpty)
    message match {
      case Message.BusUpgr() =>
        bus.reply(this, ReplyMetadata(Reply.Ok(), 1))
      case Message.BusRd() =>
        maybeAddress = Some((address, currentCycle + AbstractMemory.Latency))
      case Message.BusRdX() =>
        bus.reply(this, ReplyMetadata(Reply.Ok(), 1))
    }
  }

  override def onReply(sender: BusDelegate[Message, Reply],
                       address: Address,
                       reply: Reply): Unit =
    reply match {
      // Assume that memory has an unlimited write buffer
      case Reply.Flush() | Reply.FlushOpt() =>
        maybeAddress match {
          case Some((currentAddress, _)) =>
            require(currentAddress == address)
            maybeAddress = None
            bus.reply(this, ReplyMetadata(Reply.Ok(), 1))
          case None =>
            throw new RuntimeException(
              "Memory: Got FlushOpt() while maybeAddress is None"
            )
        }
      case Reply.Ok() => ()
    }

}
