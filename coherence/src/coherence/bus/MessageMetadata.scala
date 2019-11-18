package coherence.bus

import coherence.Address

/**
  * Size in bytes
  */
case class MessageMetadata[Message](message: Message,
                                    address: Address,
                                    size: Int = 1)
