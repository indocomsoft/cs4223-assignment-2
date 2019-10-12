package coherence.bus

/**
  * @param size in bytes
  */
case class MessageMetadata[Message](message: Message, address: Long, size: Int)
