package coherence.bus

import coherence.Address

/**
  * Assume all message are 1 word long
  */
case class MessageMetadata[Message](message: Message, address: Address)
