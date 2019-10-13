package coherence.bus

/**
  * @param size in bytes
  */
case class ReplyMetadata[Reply](reply: Reply, size: Int)
