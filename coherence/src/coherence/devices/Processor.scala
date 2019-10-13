package coherence.devices

import scala.io.Source

object Processor {
  def apply[State, Message](id: Int,
                            cache: Cache[State, Message],
                            source: Source): Processor[State, Message] =
    new Processor(id, cache, source.getLines().map(ProcessorOp(_)))

  sealed trait Status
  object Status {
    case class Ready() extends Status
    case class Operation(untilCycle: Long) extends Status
    case class Cache() extends Status
    case class Finished() extends Status
  }
}

class Processor[Message, State](val id: Int,
                                val cache: Cache[Message, State],
                                private[this] val ops: Iterator[ProcessorOp])
    extends CacheDelegate
    with Device {
  private[this] var currentCycle: Long = 0

  private[this] var currentOp: Option[ProcessorOp] = None
  private[this] var status: Processor.Status = Processor.Status.Ready()

  def isFinished(): Boolean = status match {
    case Processor.Status.Finished() => true
    case _                           => false
  }

  override def cycle(): Unit = {
    currentCycle += 1
    status match {
      case Processor.Status.Finished() | Processor.Status.Cache() => ()
      case Processor.Status.Operation(untilCycle) =>
        if (currentCycle > untilCycle) {
          status = Processor.Status.Ready()
          currentOp = None
          performInstruction()
        }
      case Processor.Status.Ready() =>
        performInstruction()
    }
    cache.cycle()
  }

  private[this] def performInstruction(): Unit = {
    if (currentOp.isEmpty) loadInstruction()
    if (currentOp.isDefined) println(s"Processor $id: ${currentOp.get}")
    currentOp match {
      case None => status = Processor.Status.Finished()
      case Some(ProcessorOp.Other(numCycles)) =>
        status =
          Processor.Status.Operation(untilCycle = currentCycle + numCycles)
      case Some(ProcessorOp.Load(address)) =>
        status = Processor.Status.Cache()
        cache.request(this, CacheOp.Load(address))
      case Some(ProcessorOp.Store(address)) =>
        status = Processor.Status.Cache()
        cache.request(this, CacheOp.Store(address))
    }
  }

  private[this] def loadInstruction(): Unit = {
    if (ops.hasNext) currentOp = Some(ops.next())
    else currentOp = None
  }

  override def requestCompleted(op: CacheOp): Unit = {
    println(s"Processor $id: cache request completed, op $op")

    currentOp match {
      case Some(ProcessorOp.Load(_)) | Some(ProcessorOp.Store(_)) =>
        status = Processor.Status.Ready()
        currentOp = None
      case _ =>
        throw new RuntimeException("Got response from cache without request")
    }
  }

  override def toString: String = s"Processor $id"
}
