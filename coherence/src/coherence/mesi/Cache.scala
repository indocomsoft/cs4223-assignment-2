package coherence.mesi

import coherence.Address
import coherence.bus.{Bus, BusDelegate, MessageMetadata}
import coherence.devices.{CacheDelegate, CacheOp, Cache => CacheTrait}
import coherence.cache.CacheLine

import scala.collection.mutable

class Cache(id: Int,
            cacheSize: Int,
            associativity: Int,
            blockSize: Int,
            bus: Bus[Message, Reply])
    extends CacheTrait[State, Message, Reply](
      id,
      cacheSize,
      associativity,
      blockSize,
      bus
    ) {
  override def hasCopy(address: Address): Boolean = {
    val Address(tag, setIndex) = address
    sets(setIndex).immutableGet(tag) match {
      case None | Some(CacheLine(State.I)) => false
      case _                               => true
    }
  }

  override def request(sender: CacheDelegate, op: CacheOp): Unit = {
    state match {
      case CacheState.Ready() =>
        val Address(tag, setIndex) = toAddress(op.address)
        op match {
          case CacheOp.Load(_) =>
            sets(setIndex).get(tag) match {
              case Some(CacheLine(_)) =>
                state = CacheState.WaitingForResult(
                  sender,
                  op,
                  currentCycle + CacheTrait.HitLatency
                )
              case None | Some(CacheLine(State.I)) =>
                state = CacheState.WaitingForBus(sender, op)
                bus.requestAccess(this)
            }
          case CacheOp.Store(_) =>
            sets(setIndex).get(tag) match {
              case Some(CacheLine(State.M)) =>
                state =
                  CacheState.WaitingForResult(sender, op, currentCycle + 1)
              case Some(CacheLine(State.E)) =>
                sets(setIndex).update(tag, CacheLine(State.M))
                state =
                  CacheState.WaitingForResult(sender, op, currentCycle + 1)
              case None | Some(CacheLine(State.I)) | Some(CacheLine(State.S)) =>
                state = CacheState.WaitingForBus(sender, op)
                bus.requestAccess(this)
            }
        }
      case _ =>
        throw new RuntimeException(s"$this: request when the state is $state")
    }
  }

  override def busAccessGranted(): MessageMetadata[Message] = {
    state match {
      case CacheState.WaitingForBus(sender, op) =>
        val address = toAddress(op.address)
        state = CacheState.WaitingForReplies(sender, op)
        op match {
          case CacheOp.Load(_) =>
            MessageMetadata(Message.BusRd(), address)
          case CacheOp.Store(_) =>
            val Address(tag, setIndex) = address
            sets(setIndex).immutableGet(tag) match {
              case Some(CacheLine(State.S)) =>
                state = CacheState.WaitingForBusUpgrPropagation(sender, op)
                MessageMetadata(Message.BusUpgr(), address)
              case None | Some(CacheLine(State.I)) =>
                MessageMetadata(Message.BusRdX(), address)
              case Some(state) =>
                throw new RuntimeException(
                  s"$this: busAccessGranted called on $address whose state is $state"
                )
            }
        }
      case _ =>
        throw new RuntimeException(
          s"$this: busAccessGranted called when state is $state"
        )
    }
  }

  override def onBusCompleteMessage(sender: BusDelegate[Message, Reply],
                                    address: Address,
                                    message: Message): Unit = {
    require(maybeReply.isEmpty)
    val Address(tag, setIndex) = address
    val result = sets(setIndex).immutableGet(tag)
    println(s"$this: address = $address, result = $result")
    if (sender.eq(this)) {
      (state, result, message) match {
        case (
            CacheState.WaitingForBusUpgrPropagation(cacheDelegate, op),
            Some(CacheLine(State.S)),
            Message.BusUpgr()
            ) =>
          cacheDelegate.requestCompleted(op)
          bus.relinquishAccess(this)
          state = CacheState.Ready()
        case _ => ()
      }
    } else {
      result match {
        case Some(CacheLine(State.M)) =>
          message match {
            case Message.BusRd() =>
              sets(setIndex).update(tag, CacheLine(State.S))
              maybeReply = Some(Reply.Flush())
            case Message.BusRdX() =>
              sets(setIndex).update(tag, CacheLine(State.I))
              maybeReply = Some(Reply.Flush())
            case Message.BusUpgr() =>
              throw new RuntimeException(
                s"$this: Received $message when state is M"
              )
          }
        case Some(CacheLine(State.E)) =>
          message match {
            case Message.BusRd() =>
              sets(setIndex).update(tag, CacheLine(State.S))
              maybeReply = Some(Reply.FlushOpt())
            case Message.BusRdX() =>
              sets(setIndex).update(tag, CacheLine(State.I))
              maybeReply = Some(Reply.FlushOpt())
            case Message.BusUpgr() =>
              throw new RuntimeException(
                s"$this: Received $message when state is E"
              )
          }
        case Some(CacheLine(State.S)) =>
          message match {
            case Message.BusRd() =>
              maybeReply = Some(Reply.FlushOpt())
            case Message.BusRdX() =>
              sets(setIndex).update(tag, CacheLine(State.I))
              maybeReply = Some(Reply.FlushOpt())
            case Message.BusUpgr() =>
              maybeReply = Some(Reply.FlushOpt())
          }
        case None | Some(CacheLine(State.I)) => ()
      }
    }
  }

  override def onBusCompleteResponse(
    sender: BusDelegate[Message, Reply],
    address: Address,
    reply: Reply,
    originalSender: BusDelegate[Message, Reply],
    originalMessage: Message
  ): Unit =
    if (originalSender.eq(this)) {
      val Address(tag, setIndex) = address
      state match {
        case CacheState.WaitingForReplies(sender, op) =>
          reply match {
            case Reply.FlushOpt() | Reply.MemoryRead() =>
              bus.relinquishAccess(this)
              sender.requestCompleted(op)
              state = CacheState.Ready()
              commitChange(op, address, reply)
            case Reply.Flush() =>
              state = CacheState.WaitingForWriteback(sender, op)
            case Reply.WriteBackOk() =>
              throw new RuntimeException(
                s"$this: got $reply when state is $state"
              )
          }
        case CacheState.WaitingForWriteback(sender, op) =>
          reply match {
            case Reply.WriteBackOk() =>
              bus.relinquishAccess(this)
              sender.requestCompleted(op)
              state = CacheState.Ready()
              commitChange(op, address, reply)
            case _ =>
              throw new RuntimeException(
                s"$this: got $reply when state is $state"
              )
          }
        case _ =>
          throw new RuntimeException(
            s"$this: got reply addressed to me when state is $state"
          )
      }
    }

  private[this] def commitChange(op: CacheOp,
                                 address: Address,
                                 reply: Reply): Unit = {
    val Address(tag, setIndex) = address
    (op, sets(setIndex).immutableGet(tag)) match {
      case (CacheOp.Load(_), None | Some(CacheLine(State.I))) =>
        if (bus.isShared(address))
          sets(setIndex).update(tag, CacheLine(State.S))
        else sets(setIndex).update(tag, CacheLine(State.E))
      case (CacheOp.Store(_), None | Some(CacheLine(State.I))) =>
        sets(setIndex).update(tag, CacheLine(State.M))
      case (op, cacheLine) =>
        throw new RuntimeException(
          s"$this: got $reply when op = $op, cacheLine = $cacheLine"
        )
    }

  }

}
