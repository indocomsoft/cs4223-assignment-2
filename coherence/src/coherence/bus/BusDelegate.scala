package coherence.bus

import coherence.Address

trait BusDelegate[Message, Reply] {

  /**
    * To be called by the bus after the device requests access.
    * Device to return what message they want to send
    */
  def message(): MessageMetadata[Message]

  /**
    * To be called by the bus when there is a complete message on the bus.
    */
  def onCompleteMessage(sender: BusDelegate[Message, Reply],
                        address: Address,
                        message: Message): Unit

  /**
    * To be called by the bus when a device replied.
    */
  def onReply(sender: BusDelegate[Message, Reply],
              address: Address,
              reply: Reply): Unit

  /**
    * To be called by the bus to update the "SHARED" OR line
    */
  def hasCopy(address: Address): Boolean
}
