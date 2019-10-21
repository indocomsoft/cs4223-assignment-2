package coherence.bus

trait BusStatistics[Message, Reply] {
  def dataTraffic: Long
  def numInvalidationUpdate: Long

  def logMessage(message: Message): Unit
  def logReply(reply: ReplyMetadata[Reply]): Unit
}
