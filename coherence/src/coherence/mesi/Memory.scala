package coherence.mesi

import coherence.Address
import coherence.bus.{Bus, BusDelegate, MessageMetadata, ReplyMetadata}
import coherence.devices.{Memory => AbstractMemory}

class Memory(bus: Bus[Message, Reply], blockSize: Int)
    extends AbstractMemory[State, Message, Reply](bus, blockSize) {
  override def busAccessGranted(): MessageMetadata[Message] =
    throw new RuntimeException("Memory: unexpected busAccessGranted()")

  override def onBusCompleteMessage(sender: BusDelegate[Message, Reply],
                                    address: Address,
                                    message: Message): Unit = {
    require(maybeReply.isEmpty)
    println(s"Memory: before, maybeReply = $maybeReply")
    message match {
      case Message.BusUpgr() => ()
      case Message.BusRd() | Message.BusRdX() =>
        maybeReply = Some(
          (Reply.MemoryRead(), currentCycle + AbstractMemory.Latency)
        )
    }
    println(s"Memory: after, maybeReply = $maybeReply")
  }

  override def onBusCompleteResponse(
    sender: BusDelegate[Message, Reply],
    address: Address,
    reply: Reply,
    originalSender: BusDelegate[Message, Reply],
    originalMessage: Message
  ): Unit = reply match {
    case Reply.FlushOpt() =>
      maybeReply match {
        case Some((Reply.MemoryRead(), _)) =>
          println("Memory: received FLushOpt(), so setting maybeReply to None")
          maybeReply = None
        case None | Some(_) =>
          throw new RuntimeException(
            s"Memory: received FlushOpt() while maybeReply is $maybeReply"
          )
      }
    case Reply.Flush() =>
      maybeReply match {
        case Some((Reply.MemoryRead(), _)) =>
          maybeReply = Some(
            (Reply.WriteBackOk(), currentCycle + AbstractMemory.Latency)
          )
        case None | Some(_) =>
          throw new RuntimeException(
            s"Memory: received Flush() while maybeReply is $maybeReply"
          )
      }
    case Reply.MemoryRead() | Reply.WriteBackOk() =>
      throw new RuntimeException(s"Memory: unexpected reply $reply")
  }

}
