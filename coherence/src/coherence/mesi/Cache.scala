package coherence.mesi

import coherence.bus.{Bus, BusDelegate, MessageMetadata}
import coherence.devices.{CacheDelegate, CacheOp, Cache => CacheTrait}
import coherence.cache.CacheLine

import scala.collection.mutable

class Cache(id: Int,
            cacheSize: Int,
            associativity: Int,
            blockSize: Int,
            bus: Bus[Message])
    extends CacheTrait[State, Message](
      id,
      cacheSize,
      associativity,
      blockSize,
      bus
    ) {
  // Queue of addresses to flushOpt or flush
  private[this] var flushOptQueue = mutable.Queue[Long]()
  private[this] var flushQueue = mutable.Queue[Long]()
  // A queue of message and address
  private[this] var pendingMessage = false

  override def request(sender: CacheDelegate, op: CacheOp): Unit = {
    require(this.op.isEmpty)
    val (tag, setIndex, _) = calculateAddress(op.address)
    op match {
      case CacheOp.Load(_) =>
        sets(setIndex).get(tag) match {
          case None | Some(CacheLine(State.I)) =>
            this.op = Some(OpMetadata(sender, op, -1))
            bus.requestAccess(this)
            pendingMessage = true
          case Some(CacheLine(_)) =>
            this.op = Some(
              OpMetadata(sender, op, currentCycle + 1 + CacheTrait.HitLatency)
            )
        }
      case CacheOp.Store(_) =>
        val result = sets(setIndex).get(tag)
        println(s"Cache $id: request $op mine $result")
        result match {
          case None | Some(CacheLine(State.I)) =>
            this.op = Some(OpMetadata(sender, op, -1))
            bus.requestAccess(this)
            pendingMessage = true
          case Some(CacheLine(State.E)) =>
            sets(setIndex).update(tag, CacheLine(State.M))
            this.op = Some(OpMetadata(sender, op, currentCycle + 1))
          case Some(CacheLine(State.M)) =>
            this.op = Some(OpMetadata(sender, op, currentCycle + 1))
          case Some(CacheLine(State.S)) =>
            this.op = Some(OpMetadata(sender, op, -1))
            bus.requestAccess(this)
            pendingMessage = true
        }
    }
  }

  override def message(): Option[MessageMetadata[Message]] =
    if (pendingMessage) {
      op match {
        case Some(OpMetadata(_, CacheOp.Load(address), -1)) =>
          pendingMessage = false
          Some(MessageMetadata(Message.BusRd(), address, 1))
        case Some(OpMetadata(_, CacheOp.Store(address), -1)) =>
          val (tag, setIndex, _) = calculateAddress(address)
          sets(setIndex).immutableGet(tag) match {
            case None | Some(CacheLine(State.I)) =>
              pendingMessage = false
              Some(MessageMetadata(Message.BusRdX(), address, 1))
            case Some(CacheLine(State.S)) =>
              pendingMessage = false
              Some(MessageMetadata(Message.BusUpgr(), address, 1))
            case _ =>
              throw new RuntimeException("Unexpected request for message")
          }
      }
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
                                 message: Message): Unit = {
    val (tag, setIndex, _) = calculateAddress(address)
    if (sender.eq(this)) {
      println(
        s"Cache $id: Ownself got message $message on address $address (tag $tag setIndex $setIndex), mine ${sets(setIndex)
          .immutableGet(tag)}"
      )
      (message, op) match {
        case (
            Message.BusUpgr(),
            Some(OpMetadata(sender, op @ CacheOp.Store(_), -1))
            ) =>
          sets(setIndex).update(tag, CacheLine(State.M))
          this.op = Some(OpMetadata(sender, op, -1))
        case _ =>
          ()
      }
    } else {
      val result = sets(setIndex).get(tag)
      println(
        s"Cache $id: Got message $message on address $address (tag $tag setIndex $setIndex), mine $result"
      )
      result match {
        case Some(CacheLine(State.M)) =>
          message match {
            case Message.BusRd() =>
              sets(setIndex).update(tag, CacheLine(State.S))
              flushQueue.enqueue(address)
            case Message.BusRdX() =>
              println("before" + sets(setIndex).get(tag))
              sets(setIndex).update(tag, CacheLine(State.I))
              println("after" + sets(setIndex).get(tag))
              flushQueue.enqueue(address)
            case Message.FlushOpt() if sender.isInstanceOf[Memory] =>
              // Getting FlushOpt from memory if we have exclusive access is alright
              ()
            case Message.FlushOpt() | Message.BusUpgr() | Message.Flush() =>
              throw new RuntimeException(
                s"Cache $id: Encountered $message on address $address (tag $tag, setIndex $setIndex) whose state is M"
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
            case Message.FlushOpt() if sender.isInstanceOf[Memory] =>
              // Ignoring FlushOpt from memory if we have exclusive access is alright
              ()
            case Message.FlushOpt() | Message.Flush() | Message.BusUpgr() =>
              throw new RuntimeException(
                s"Cache $id: Encountered $message on address $address whose state is E"
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
                s"Cache $id: Encountered $message on address $address whose state is S"
              )
          }
        case Some(CacheLine(State.I)) | None =>
          (message, this.op) match {
            case (
                Message.FlushOpt() | Message.FlushOpt(),
                Some(OpMetadata(sender, CacheOp.Load(loadAddress), -1))
                ) =>
              val (loadTag, loadSetIndex, _) = calculateAddress(loadAddress)
              if (tag == loadTag && setIndex == loadSetIndex) {
                val state = if (bus.isShared(address)) State.S else State.E
                sets(setIndex).update(tag, CacheLine(state))
                this.op = Some(
                  OpMetadata(
                    sender,
                    CacheOp.Load(loadAddress),
                    currentCycle + 1
                  )
                )
                println(
                  s"Cache $id: Got data for address $address, assumed after BusRd"
                )
              } else {
                ()
              }
            case (
                Message.FlushOpt() | Message.Flush(),
                Some(OpMetadata(sender, CacheOp.Store(storeAddress), -1))
                ) =>
              val (storeTag, storeSetIndex, _) = calculateAddress(storeAddress)
              if (tag == storeTag && setIndex == storeSetIndex) {
                sets(setIndex).update(tag, CacheLine(State.M))
                this.op = Some(
                  OpMetadata(
                    sender,
                    CacheOp.Store(storeAddress),
                    currentCycle + 1
                  )
                )
                println(
                  s"Cache $id: Got data for address $address, assumed after BusRdX"
                )
              } else {
                ()
              }
            case _ => ()
          }
      }
    }
  }

  override def hasCopy(address: Long): Boolean = {
    val (tag, setIndex, _) = calculateAddress(address)
    sets(setIndex).get(tag) match {
      case None | Some(CacheLine(State.I)) => false
      case _                               => true
    }
  }

}
