package coherence.bus

trait Device[Message] {

  /**
    * To be called by the bus after the device requests access.
    * Returning None cancels the request
    */
  def message(): Option[MessageMetadata[Message]]

  /**
    * To be called by the bus when there is a complete message on the bus.
    */
  def onCompleteMessage(message: Message): Unit
}
