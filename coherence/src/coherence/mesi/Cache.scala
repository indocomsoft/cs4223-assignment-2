package coherence.mesi

import coherence.bus.{Bus, BusDelegate, MessageMetadata}
import coherence.devices.{
  CacheDelegate,
  CacheOp,
  Memory,
  MemoryOp,
  Cache => CacheTrait
}
import coherence.Unit._

class Cache(cacheSize: Int,
            associativity: Int,
            blockSize: Int,
            memory: Memory,
            bus: Bus[Message])
    extends CacheTrait[State, Message](
      cacheSize,
      associativity,
      blockSize,
      memory,
      bus
    ) {
  private[this] var currentCycle: Long = 0
  // Long is finishedCycle, -1 if still pending
  private[this] var op: Option[(CacheDelegate, CacheOp, Long)] = None

  override def cycle(): Unit = {
    currentCycle += 1
    op match {
      case Some((sender, op, finishedCycle)) if finishedCycle == currentCycle =>
        sender.requestCompleted(op)
      case _ =>
        ()
    }
  }

  override def memoryOpCompleted(op: MemoryOp): Unit = ???

  override def request(sender: CacheDelegate, op: CacheOp): Unit = {
    require(this.op.isEmpty)
    val (tag, setIndex, _) = calculateAddress(op.address)
    op match {
      case CacheOp.Load(_) =>
        sets(setIndex).get(tag) match {
          case Some(_) => this.op = Some((sender, op, currentCycle + 1))
          case None =>
            memory.operate(this, MemoryOp.Read(op.address))
            bus.requestAccess(this)
            this.op = Some((sender, op, -1))
        }
      case CacheOp.Store(_) =>
        ???
    }
  }

  override def message(): Option[MessageMetadata[Message]] =
    this.op.map {
      case (_, CacheOp.Load(address), _) =>
        MessageMetadata(Message.BusRd(), address, 1.word())
      case (_, CacheOp.Store(_), _) => ???
    }

  override def onCompleteMessage(sender: BusDelegate[Message],
                                 address: Long,
                                 message: Message): Unit = ???

}
