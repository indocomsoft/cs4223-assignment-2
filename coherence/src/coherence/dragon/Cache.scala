package coherence.dragon

import coherence.Address
import coherence.Debug._
import coherence.bus.{Bus, BusDelegate, MessageMetadata}
import coherence.cache.CacheLine
import coherence.devices.{CacheDelegate, CacheOp, Cache => CacheTrait}

class Cache(id: Int,
            cacheSize: Int,
            associativity: Int,
            blockSize: Int,
            bus: Bus[Message, Reply],
            stats: CacheStatistics[State])
    extends CacheTrait[State, Message, Reply](
      id,
      cacheSize,
      associativity,
      blockSize,
      bus,
      stats
    ) {
  override def hasCopy(address: Address): Boolean = {
    val Address(tag, setIndex) = address
    sets(setIndex).immutableGet(tag) match {
      case None | Some(CacheLine(State.I)) => false
      case _                               => true
    }
  }

  private[this] def maybeEvict(sender: CacheDelegate, op: CacheOp): Unit = {
    val Address(tag, setIndex) = toAddress(op.address)
    sets(setIndex).update(tag, CacheLine(State.I)) match {
      case None =>
        state = CacheState.WaitingForBus(sender, op)
        bus.requestAccess(this)
      case Some((tag, CacheLine(State.M) | CacheLine(State.SM))) =>
        val address = Address(tag, setIndex)
        println_debug(s"$this: Evicting $address requires flush")
        state = CacheState.EvictWaitingForBus(address, sender, op)
        bus.requestAccess(this)
      case Some(
          (tag, CacheLine(State.I) | CacheLine(State.E) | CacheLine(State.SC))
          ) =>
        println_debug(s"$this: Evicting ${Address(tag, setIndex)}")
        state = CacheState.WaitingForBus(sender, op)
        bus.requestAccess(this)
    }
  }

  override def request(sender: CacheDelegate, op: CacheOp): Unit = {
    state match {
      case CacheState.Ready() =>
        val Address(tag, setIndex) = toAddress(op.address)
        numRequests += 1
        val result = sets(setIndex).get(tag)
        op match {
          case CacheOp.Load(_) =>
            result match {
              case None =>
                maybeEvict(sender, op)
              case Some(CacheLine(State.I)) =>
                throw new RuntimeException(
                  s"$this: request when the state is $state"
                )
              case Some(CacheLine(currentState)) =>
                numHits += 1
                state = CacheState.WaitingForResult(
                  sender,
                  op,
                  currentCycle + CacheTrait.HitLatency
                )
            }
          case CacheOp.Store(_) =>
            result match {
              case None =>
                maybeEvict(sender, op)
              case Some(CacheLine(State.M)) =>
                state =
                  CacheState.WaitingForResult(sender, op, currentCycle + 1)
              case Some(CacheLine(State.E)) =>
                sets(setIndex).update(tag, CacheLine(State.M))
                state =
                  CacheState.WaitingForResult(sender, op, currentCycle + 1)
              case Some(CacheLine(State.SC)) | Some(CacheLine(State.SM)) =>
                state = CacheState.WaitingForBus(sender, op)
                bus.requestAccess(this)
              case Some(CacheLine(State.I)) =>
                throw new RuntimeException(
                  s"$this: request when the state is $state"
                )
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
        println_debug(
          s"$this: op = $op, cacheLine = ${sets(address.setIndex).immutableGet(address.tag)}"
        )
        op match {
          case CacheOp.Load(_) =>
            MessageMetadata(Message.BusRd(), address)
          case CacheOp.Store(_) =>
            val Address(tag, setIndex) = address
            sets(setIndex).immutableGet(tag) match {
              case Some(CacheLine(State.SM)) | Some(CacheLine(State.SC)) =>
                state = CacheState.WaitingForBusPropagation(sender, op)
                MessageMetadata(Message.BusUpt(), address, blockSize)
              case None | Some(CacheLine(State.I)) =>
                MessageMetadata(Message.BusRd(), address)
              case Some(state) =>
                throw new RuntimeException(
                  s"$this: busAccessGranted called on $address whose state is $state"
                )
            }
        }
      case CacheState.EvictWaitingForBus(address, sender, op) =>
        state = CacheState.EvictWaitingForWriteback(address, sender, op)
        println_debug(s"$this: Eviction, flushing $address")
        MessageMetadata(Message.Flush(), address)
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
    println_debug(s"$this: address = $address, result = $result")
    if (sender.eq(this)) {
      (state, result, message) match {
        case (
            CacheState.WaitingForBusPropagation(cacheDelegate, op),
            Some(CacheLine(State.SC)),
            Message.BusUpt()
            ) =>
          bus.relinquishAccess(this)
          sets(setIndex).update(tag, CacheLine(State.SM))
          state = CacheState.Ready()
          cacheDelegate.requestCompleted(op)
        case (
            CacheState.WaitingForBusPropagation(cacheDelegate, op),
            Some(CacheLine(State.SM)),
            Message.BusUpt()
            ) =>
          bus.relinquishAccess(this)
          if (bus.isShared(address))
            sets(setIndex).update(tag, CacheLine(State.SM))
          else sets(setIndex).update(tag, CacheLine(State.M))
          state = CacheState.Ready()
          cacheDelegate.requestCompleted(op)
        case _ => ()
      }
    } else {
      result match {
        case Some(CacheLine(State.M)) =>
          message match {
            case Message.BusRd() =>
              sets(setIndex).update(tag, CacheLine(State.SM))
              maybeReply = Some(Reply.Flush())
            case Message.BusUpt() =>
              throw new RuntimeException(
                s"$this: Received $message when state is M"
              )
            case Message.Flush() => ()
          }
        case Some(CacheLine(State.E)) =>
          message match {
            case Message.BusRd() =>
              sets(setIndex).update(tag, CacheLine(State.SC))
            case Message.BusUpt() =>
              throw new RuntimeException(
                s"$this: Received $message when state is E"
              )
            case Message.Flush() => ()
          }
        case Some(CacheLine(State.SM)) =>
          message match {
            case Message.BusRd() =>
              maybeReply = Some(Reply.Flush())
            case Message.BusUpt() =>
              sets(setIndex).update(tag, CacheLine(State.SC))
            case Message.Flush() => ()
          }
        case Some(CacheLine(State.SC)) =>
          message match {
            case Message.Flush() | Message.BusRd() | Message.BusUpt() => ()
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
      println_debug(
        s"$this: Got reply $reply addressed to me from $sender on address $address"
      )
      val Address(tag, setIndex) = address
      state match {
        case CacheState.WaitingForReplies(sender, op) =>
          reply match {
            case Reply.MemoryRead() =>
              bus.relinquishAccess(this)
              state = CacheState.Ready()
              commitChange(op, address, reply)
              sender.requestCompleted(op)
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
              state = CacheState.Ready()
              commitChange(op, address, reply)
              sender.requestCompleted(op)
            case _ =>
              throw new RuntimeException(
                s"$this: got $reply when state is $state"
              )
          }
        case CacheState.EvictWaitingForWriteback(addressToEvict, sender, op) =>
          require(addressToEvict == address)
          reply match {
            case Reply.WriteBackOk() =>
              bus.relinquishAccess(this)
              state = CacheState.WaitingForBus(sender, op)
              bus.requestAccess(this)
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
    } else {
      reply match {
        case _ => ()
      }
    }

  private[this] def commitChange(op: CacheOp,
                                 address: Address,
                                 reply: Reply): Unit = {
    val Address(tag, setIndex) = address
    (op, sets(setIndex).immutableGet(tag)) match {
      case (CacheOp.Load(_), None | Some(CacheLine(State.I))) =>
        if (bus.isShared(address))
          sets(setIndex).update(tag, CacheLine(State.SC))
        else sets(setIndex).update(tag, CacheLine(State.E))
      case (CacheOp.Store(_), None | Some(CacheLine(State.I))) =>
        if (bus.isShared(address))
          sets(setIndex).update(tag, CacheLine(State.SM))
        else sets(setIndex).update(tag, CacheLine(State.M))
      case (op, cacheLine) =>
        throw new RuntimeException(
          s"$this: got $reply when op = $op, cacheLine = $cacheLine"
        )
    }

  }

}
