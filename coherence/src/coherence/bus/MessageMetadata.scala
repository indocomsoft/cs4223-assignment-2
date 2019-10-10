package coherence.bus

import coherence.Unit.Word

case class MessageMetadata[Message](message: Message, address: Long, size: Word)
