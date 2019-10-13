package coherence.bus

import coherence.Address

import scala.collection.immutable.Queue
import scala.collection.mutable

object Bus {
  val PerWordLatency = 2
}

class Bus[Message, Reply] {
  sealed trait BusState
  sealed trait Active {
    val messageMetadata: MessageMetadata[Message]
    val sender: BusDelegate[Message, Reply]
  }
  sealed trait AfterRequestSent {
    val replied: Set[BusDelegate[Message, Reply]]
  }
  sealed trait ToSend {
    val finishedCycle: Long
  }

  object BusState {
    case class Ready() extends BusState
    case class RequestReceived(messageMetadata: MessageMetadata[Message],
                               sender: BusDelegate[Message, Reply],
                               finishedCycle: Long)
        extends BusState
        with Active
        with ToSend
    case class WaitingForReply(messageMetadata: MessageMetadata[Message],
                               sender: BusDelegate[Message, Reply],
                               replied: Set[BusDelegate[Message, Reply]])
        extends BusState
        with Active
        with AfterRequestSent
    case class ReplyReceived(
      messageMetadata: MessageMetadata[Message],
      sender: BusDelegate[Message, Reply],
      replied: Set[BusDelegate[Message, Reply]],
      replies: Queue[(ReplyMetadata[Reply], BusDelegate[Message, Reply])],
      activeReply: (Reply, BusDelegate[Message, Reply]),
      finishedCycle: Long
    ) extends BusState
        with Active
        with AfterRequestSent
        with ToSend
  }

  private[this] var state: BusState = BusState.Ready()

  private[this] var currentCycle: Long = 0

  private[this] val devices = mutable.Set[BusDelegate[Message, Reply]]()
  private[this] val requests = mutable.Queue[BusDelegate[Message, Reply]]()

  def addBusDelegate(device: BusDelegate[Message, Reply]): Unit = {
    devices.add(device)
  }

  def requestAccess(device: BusDelegate[Message, Reply]): Unit = {
    requests.enqueue(device)
  }

  def reply(device: BusDelegate[Message, Reply],
            replyMetadata: ReplyMetadata[Reply]): Unit = state match {
    case BusState.WaitingForReply(messageMetadata, sender, replied) =>
      val numWords = (replyMetadata.size - 1) / 8 + 1
      state = BusState.ReplyReceived(
        messageMetadata,
        sender,
        replied + device,
        Queue(),
        (replyMetadata.reply, device),
        currentCycle + numWords * Bus.PerWordLatency
      )
    case BusState.ReplyReceived(
        messageMetadata,
        sender,
        replied,
        replies,
        activeReply,
        finishedCycle
        ) =>
      state = BusState.ReplyReceived(
        messageMetadata,
        sender,
        replied + device,
        replies.enqueue((replyMetadata, device)),
        activeReply,
        finishedCycle
      )
    case BusState.Ready() | BusState.RequestReceived(_, _, _) =>
      throw new RuntimeException(
        s"Bus: Unexpected call to reply during state $state"
      )
  }

  def cycle(): Unit = {
    println("Bus cycle")
    currentCycle += 1
    state match {
      case BusState.RequestReceived(
          messageMetadata @ MessageMetadata(message, address),
          sender,
          finishedCycle
          ) =>
        if (currentCycle == finishedCycle) {
          state = BusState.WaitingForReply(messageMetadata, sender, Set())
          (devices - sender).foreach(
            _.onCompleteMessage(sender, address, message)
          )
        }
      case BusState.ReplyReceived(
          messageMetadata @ MessageMetadata(_, address),
          sender,
          replied,
          replies,
          (reply, device),
          finishedCycle
          ) =>
        if (currentCycle == finishedCycle) {
          (devices - sender - device).foreach(_.onReply(device, address, reply))
          if (replies.isEmpty) {
            if (replied.size + 1 == devices.size) {
              // All have replied and no more replies to send
              state = BusState.Ready()
              loadNewRequest()
            } else {
              // Some still yet to reply
              state = BusState.WaitingForReply(messageMetadata, sender, replied)
            }
          } else {
            // We've some replies pending to be sent
            val ((ReplyMetadata(reply, size), device), newReplies) =
              replies.dequeue
            val numWords = (size - 1) / 8 + 1
            state = BusState.ReplyReceived(
              messageMetadata,
              sender,
              replied,
              newReplies,
              (reply, device),
              currentCycle + numWords * Bus.PerWordLatency
            )
          }
        }
      case BusState.WaitingForReply(_, _, _) => ()
      case BusState.Ready()                  => loadNewRequest()
    }
  }

  private[this] def loadNewRequest(): Unit = {
    if (requests.nonEmpty) {
      val device = requests.dequeue()
      val message = device.message()
      state = BusState.RequestReceived(
        message,
        device,
        currentCycle + Bus.PerWordLatency
      )
    }
  }

  def isShared(address: Address): Boolean =
    devices.map(_.hasCopy(address)).reduce(_ || _)
}
