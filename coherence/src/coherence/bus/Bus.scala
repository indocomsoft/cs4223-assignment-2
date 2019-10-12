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
  /* Things to change every time we get to a new message */
  private[this] var maybeMessageMetadata: Option[MessageMetadata[Message]] =
    None
  private[this] var currentBusDelegate: Option[BusDelegate[Message]] = None

  private[this] var expires_in = 0

  private[this] val devices = mutable.Set[BusDelegate[Message]]()
  private[this] val requests = mutable.Queue[BusDelegate[Message]]()

  def addBusDelegate(device: BusDelegate[Message]): Unit = {
    devices.add(device)
  }

  def requestAccess(device: BusDelegate[Message]): Unit = {
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
      (maybeMessageMetadata, currentBusDelegate) match {
        case (Some(MessageMetadata(message, address, _)), Some(device)) =>
          // Send the message to everyone in the bus
          var hasCopy = devices.map(_.hasCopy(address)).reduce(_ || _)
          devices.foreach(
            _.onCompleteMessage(device, address, message, hasCopy)
          )
          currentBusDelegate = None
          maybeMessageMetadata = None
        case _ =>
          ()
      }
      loadNewMessage()
    }
  }

  private[this] def loadNewMessage(): Unit =
    while (maybeMessageMetadata.isEmpty && requests.nonEmpty) {
      val device = requests.dequeue()
      device.message() match {
        case messageMetadata @ Some(MessageMetadata(_, _, size)) =>
          maybeMessageMetadata = messageMetadata
          currentBusDelegate = Some(device)
          expires_in = size * Bus.PerWordLatency
        case _ =>
          ()
      }
    }
}
