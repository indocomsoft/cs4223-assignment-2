package coherence.mesi

import coherence.bus.{ReplyMetadata, BusStatistics => AbstractBusStatistics}

class BusStatistics[Message, Reply]
    extends AbstractBusStatistics[Message, Reply] {
  private[this] var _dataTraffic: Long = 0
  private[this] var _numInvalidationUpdate: Long = 0

  override def dataTraffic: Long = _dataTraffic
  override def numInvalidationUpdate: Long = _numInvalidationUpdate

  override def logMessage(message: Message): Unit =
    message match {
      case Message.BusUpgr() | Message.BusRdX() => _numInvalidationUpdate += 1
      case Message.Flush() | Message.BusRd()    => ()
    }
  override def logReply(reply: ReplyMetadata[Reply]): Unit =
    reply match {
      case ReplyMetadata(Reply.WriteBackOk(), _) => ()
      case ReplyMetadata(_, size)                => _dataTraffic += size
    }
}
