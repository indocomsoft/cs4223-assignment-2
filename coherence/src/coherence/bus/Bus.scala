package coherence.bus

import coherence.Address
import coherence.devices.Device

import coherence.Debug._

import scala.collection.mutable

object Bus {
  val PerWordLatency = 2
}

class Bus[Message, Reply](val stats: BusStatistics[Message, Reply])
    extends Device {
  sealed trait BusState
  sealed trait ActiveBusState extends BusState {
    val messageMetadata: MessageMetadata[Message]
    val owner: BusDelegate[Message, Reply]
  }

  object BusState {
    case class Ready() extends BusState
    case class ProcessingRequest(messageMetadata: MessageMetadata[Message],
                                 owner: BusDelegate[Message, Reply],
                                 finishedCycle: Long)
        extends ActiveBusState
    case class RequestSent(messageMetadata: MessageMetadata[Message],
                           owner: BusDelegate[Message, Reply])
        extends ActiveBusState
    case class ProcessingReply(messageMetadata: MessageMetadata[Message],
                               owner: BusDelegate[Message, Reply],
                               reply: Reply,
                               sender: BusDelegate[Message, Reply],
                               finishedCycle: Long)
        extends ActiveBusState
  }

  private[this] var state: BusState = BusState.Ready()

  case class MarkEndMetadata(owner: BusDelegate[Message, Reply],
                             message: Message,
                             address: Address)
  private[this] var needMarkEndMetadata: Option[MarkEndMetadata] = None

  private[this] var currentCycle: Long = 0

  private[this] val devices = mutable.Set[BusDelegate[Message, Reply]]()
  private[this] val requests = mutable.Queue[BusDelegate[Message, Reply]]()

  def addBusDelegate(device: BusDelegate[Message, Reply]): Unit = {
    devices.add(device)
  }

  def requestAccess(device: BusDelegate[Message, Reply]): Unit = {
    requests.enqueue(device)
  }

  def relinquishAccess(device: BusDelegate[Message, Reply]): Unit = {
    def helper(owner: BusDelegate[Message, Reply],
               message: Message,
               address: Address): Unit = {
      if (!owner.eq(device))
        throw new RuntimeException(
          s"Bus: relinquishAccess called by $device but current bus owner is $owner"
        )
      state = BusState.Ready()
      needMarkEndMetadata = Some(MarkEndMetadata(owner, message, address))
      println_debug(s"Bus: after relinquish, state = $state")
    }

    state match {
      case BusState.Ready() | BusState.ProcessingRequest(_, _, _) =>
        throw new RuntimeException(
          s"Bus: relinquishAccess called on state $state"
        )
      case BusState.RequestSent(MessageMetadata(message, address, _), owner) =>
        helper(owner, message, address)
      case BusState.ProcessingReply(
          MessageMetadata(message, address, _),
          owner,
          _,
          _,
          _
          ) =>
        helper(owner, message, address)
    }
  }

  override def cycle(): Unit = {
    currentCycle += 1
    needMarkEndMetadata match {
      case None => ()
      case Some(MarkEndMetadata(owner, message, address)) =>
        (devices - owner).foreach(
          _.onBusTransactionEnd(owner, address, message)
        )
        needMarkEndMetadata = None
    }
    println_debug(s"Bus: cycle $currentCycle before, state = $state")
    state match {
      case BusState.Ready() =>
        if (requests.nonEmpty) {
          val device = requests.dequeue()
          val messageMetadata = device.busAccessGranted()
          stats.logMessage(messageMetadata.message)
          val numWords = (messageMetadata.size - 1) / 8 + 1
          state = BusState.ProcessingRequest(
            messageMetadata,
            device,
            currentCycle + numWords * Bus.PerWordLatency
          )
        }
      case BusState.ProcessingRequest(
          messageMetadata @ MessageMetadata(message, address, _),
          owner,
          finishedCycle
          ) =>
        if (currentCycle == finishedCycle) {
          state = BusState.RequestSent(messageMetadata, owner)
          (devices - owner).foreach(
            _.onBusCompleteMessage(owner, address, message)
          )
          owner.onBusCompleteMessage(owner, address, message)
        }
      case BusState.ProcessingReply(
          messageMetadata @ MessageMetadata(message, address, _),
          owner,
          reply,
          sender,
          finishedCycle
          ) =>
        if (currentCycle == finishedCycle) {
          state = BusState.RequestSent(messageMetadata, owner)
          (devices - sender).foreach(
            _.onBusCompleteResponse(sender, address, reply, owner, message)
          )
        }
      case BusState.RequestSent(_, _) => ()
    }
    println_debug(s"Bus: cycle $currentCycle after, state = $state")
  }

  /**
    * Send a reply to the message received.
    * @return true if reply is received and queued for sending, else send false
    */
  def reply(sender: BusDelegate[Message, Reply],
            replyMetadata: ReplyMetadata[Reply]): Boolean =
    state match {
      case BusState.Ready() | BusState.ProcessingRequest(_, _, _) =>
        throw new RuntimeException(
          s"Bus: unexpected call to reply by $sender when state is $state"
        )
      case BusState.ProcessingReply(_, _, _, _, _) =>
        false
      case BusState.RequestSent(messageMetadata, owner) =>
        val ReplyMetadata(reply, size) = replyMetadata
        val numWords = (size - 1) / 8 + 1
        state = BusState.ProcessingReply(
          messageMetadata,
          owner,
          reply,
          sender,
          currentCycle + numWords * Bus.PerWordLatency
        )
        stats.logReply(replyMetadata)
        true
    }

  def isShared(address: Address): Boolean =
    devices.map(_.hasCopy(address)).reduce(_ || _)
}
