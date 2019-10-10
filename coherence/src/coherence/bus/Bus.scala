package coherence.bus

import scala.collection.mutable

object Bus {
  val PerWordLatency = 2
}

/**
  * How the bus works:
  * - Simulator will call `cycle()` at each cycle
  * - Inside cycle:
  *   - If there is one complete message, it will be delivered to each device
  *   - Afterwards, look at the queue of access requests, and get the message
  *     from the device at the head of the queue, and set-up the countdown again
  *   - The idea here is to allow each device to update their state from the message
  *     before asking one of them for their message.
  *     (Refer to CMU lecture notes why this precaution is needed)
  */
class Bus[Message] {
  var maybeMessage: Option[Message] = None

  private[this] val devices = mutable.Set[Device[Message]]()
  private[this] val requests = mutable.Queue[Device[Message]]()
  private[this] var expires_in = 0

  def addDevice(device: Device[Message]): Unit = {
    devices.add(device)
  }

  def requestAccess(device: Device[Message]): Unit = {
    requests.enqueue(device)
  }

  /**
    * Performs a cycle:
    * - Decrease the `expires_in` counter
    * - Broadcast the message if there is one and the deadline expired.
    * - Otherwise, if there is no message, dequeue a device who has requested access,
    *   and ask it for a message
    */
  def cycle(): Unit = {
    if (expires_in > 0) expires_in -= 1
    if (expires_in == 0) {
      maybeMessage match {
        case Some(message) =>
          // Send the message to everyone in the bus
          devices.foreach(_.onCompleteMessage(message))
        case None =>
          ()
      }
      loadNewMessage()
    }
  }

  private[this] def loadNewMessage(): Unit =
    while (maybeMessage.isEmpty && requests.nonEmpty) {
      requests.dequeue().message() match {
        case Some(MessageMetadata(message, size)) =>
          maybeMessage = Some(message)
          expires_in = size.word * Bus.PerWordLatency
        case _ =>
          ()
      }
    }
}
