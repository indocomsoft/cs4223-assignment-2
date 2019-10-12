package coherence.mesi

import coherence.bus.{Bus, BusDelegate, MessageMetadata}
import coherence.devices.{CacheDelegate, CacheOp, Cache => CacheTrait}
import coherence.cache.CacheLine

import scala.collection.mutable

class Cache(cacheSize: Int,
            associativity: Int,
            blockSize: Int,
            bus: Bus[Message])
    extends CacheTrait[State, Message](cacheSize, associativity, blockSize, bus) {
  // Queue of addresses to flushOpt or flush
  private[this] var flushOptQueue = mutable.Queue[Long]()
  private[this] var flushQueue = mutable.Queue[Long]()
  // A queue of message and address
  private[this] var pendingMessages = mutable.Queue[(Message, Long)]()

  override def request(sender: CacheDelegate, op: CacheOp): Unit = {
    require(this.op.isEmpty)
    val (tag, setIndex, _) = calculateAddress(op.address)
    op match {
      case CacheOp.Load(_) =>
        sets(setIndex).get(tag) match {
          case None | Some(CacheLine(State.I)) =>
            this.op = Some(OpMetadata(sender, op, -1))
            bus.requestAccess(this)
            pendingMessages.enqueue((Message.BusRd(), op.address))
          case Some(CacheLine(_)) =>
            this.op = Some(OpMetadata(sender, op, currentCycle + 1))
        }
      // Do not wait and just write straight away under I and M
      // Assuming that eviction to memory doesn't matter
      // (Memory has a large write queue)
      case CacheOp.Store(_) =>
        sets(setIndex).get(tag) match {
          case None | Some(CacheLine(State.I)) =>
            this.op = Some(OpMetadata(sender, op, currentCycle + 1))
            bus.requestAccess(this)
            pendingMessages.enqueue((Message.BusRdX(), op.address))
          case Some(CacheLine(State.E)) =>
            sets(setIndex).update(tag, CacheLine(State.M))
            this.op = Some(OpMetadata(sender, op, currentCycle + 1))
          case Some(CacheLine(State.M)) =>
            this.op = Some(OpMetadata(sender, op, currentCycle + 1))
          case Some(CacheLine(State.S)) =>
            sets(setIndex).update(tag, CacheLine(State.M))
            this.op = Some(OpMetadata(sender, op, currentCycle + 1))
            bus.requestAccess(this)
            pendingMessages.enqueue((Message.BusUpgr(), op.address))
        }
    }
  }

  override def message(): Option[MessageMetadata[Message]] =
    if (pendingMessages.nonEmpty) {
      val (message, address) = pendingMessages.dequeue()
      Some(MessageMetadata(message, address, 1))
    } else if (flushQueue.nonEmpty) {
      val address = flushQueue.dequeue()
      Some(MessageMetadata(Message.Flush(), address, blockSize))
    } else if (flushOptQueue.nonEmpty) {
      val address = flushOptQueue.dequeue()
      Some(MessageMetadata(Message.FlushOpt(), address, blockSize))
    } else {
      None
    }

  override def onCompleteMessage(sender: BusDelegate[Message],
                                 address: Long,
                                 message: Message,
                                 shared: Boolean): Unit = {
    if (sender.eq(this)) {
      ()
    } else {
      val (tag, setIndex, _) = calculateAddress(address)
      sets(setIndex).get(tag) match {
        case Some(CacheLine(State.M)) =>
          message match {
            case Message.BusRd() =>
              sets(setIndex).update(tag, CacheLine(State.S))
              flushQueue.enqueue(address)
            case Message.BusRdX() =>
              sets(setIndex).update(tag, CacheLine(State.I))
              flushQueue.enqueue(address)
            case Message.FlushOpt() | Message.BusUpgr() | Message.Flush() =>
              throw new RuntimeException(
                s"Encountered $message on address whose state is M"
              )
          }
        case Some(CacheLine(State.E)) =>
          message match {
            case Message.BusRd() =>
              sets(setIndex).update(tag, CacheLine(State.S))
              flushOptQueue.enqueue(address)
            case Message.BusRdX() =>
              sets(setIndex).update(tag, CacheLine(State.I))
              flushOptQueue.enqueue(address)
            case Message.FlushOpt() | Message.Flush() | Message.BusUpgr() =>
              throw new RuntimeException(
                s"Encountered $message on address whose state is E"
              )
          }
        case Some(CacheLine(State.S)) =>
          message match {
            case Message.BusRd() =>
              flushOptQueue.enqueue(address)
            case Message.BusRdX() =>
              sets(setIndex).update(tag, CacheLine(State.I))
              flushOptQueue.enqueue(address)
            case Message.BusUpgr() =>
              sets(setIndex).update(tag, CacheLine(State.I))
            case Message.FlushOpt() =>
              flushOptQueue = flushOptQueue.filter(_ != address)
            case Message.Flush() =>
              throw new RuntimeException(
                s"Encountered $message on address whose state is S"
              )
          }
        case Some(CacheLine(State.I)) | None =>
          (message, this.op) match {
            case (
                Message.FlushOpt() | Message.FlushOpt(),
                Some(OpMetadata(sender, CacheOp.Load(loadAddress), -1))
                ) if loadAddress == address =>
              val state = if (shared) State.S else State.E
              sets(setIndex).update(tag, CacheLine(state))
              this.op = Some(
                OpMetadata(sender, CacheOp.Load(loadAddress), currentCycle + 1)
              )
            case _ => ()
          }
      }
    }
  }

}
