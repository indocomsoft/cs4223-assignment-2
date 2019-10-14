package coherence.bus

import coherence.Address

trait BusDelegate[Message, Reply] {

  /**
    * To be called by the bus when access is granted.
    * Device to return what message they want to send
    */
  def busAccessGranted(): MessageMetadata[Message]

  /**
    * To be called by the bus when a complete message is on the bus.
    */
  def onBusCompleteMessage(sender: BusDelegate[Message, Reply],
                           address: Address,
                           message: Message): Unit

  /**
    * To be called by the bus when a complete response was placed on the bus.
    */
  def onBusCompleteResponse(sender: BusDelegate[Message, Reply],
                            address: Address,
                            reply: Reply,
                            originalSender: BusDelegate[Message, Reply],
                            originalMessage: Message): Unit

  /**
    * To be called by the bus when the device relinquished access to the bus.
    */
  def onBusTransactionEnd(sender: BusDelegate[Message, Reply],
                          address: Address,
                          message: Message): Unit

  /**
    * To be called by the bus to update the "SHARED" OR line
    */
  def hasCopy(address: Address): Boolean
}
