package coherence.devices

import scala.io.Source

import coherence.Debug._

object Processor {
  def apply[State, Message, Reply](
    id: Int,
    cache: Cache[State, Message, Reply],
    source: Source
  ): Processor[State, Message, Reply] =
    new Processor(id, cache, source.getLines().map(ProcessorOp(_)))

  sealed trait Status { val finished: Boolean = false }
  object Status {
    case class Ready() extends Status
    case class Operation(finishedCycle: Long) extends Status
    case class Cache() extends Status
    case class Finished() extends Status {
      override val finished: Boolean = true
    }
  }
}

class Processor[Message, State, Reply](
  val id: Int,
  val cache: Cache[Message, State, Reply],
  private[this] val ops: Iterator[ProcessorOp]
) extends CacheDelegate
    with Device {
  private[this] var currentCycle: Long = 0
  private[this] var computeCycles: Long = 0
  private[this] var numLoad: Long = 0
  private[this] var numStore: Long = 0
  private[this] var numIdle: Long = 0

  private[this] var currentOp: Option[ProcessorOp] = None
  private[this] var status: Processor.Status = Processor.Status.Ready()

  def totalCycles: Long = currentCycle
  def totalComputeCycles: Long = computeCycles
  def numLoadInstructions: Long = numLoad
  def numStoreInstructions: Long = numStore
  def numIdleCycles: Long = numIdle

  def isFinished(): Boolean = status match {
    case Processor.Status.Finished() => true
    case _                           => false
  }

  override def cycle(): Unit = {
    if (!status.finished) currentCycle += 1
    status match {
      case Processor.Status.Finished() => ()
      case Processor.Status.Cache() =>
        numIdle += 1
      case Processor.Status.Operation(finishedCycle) =>
        if (currentCycle == finishedCycle) {
          status = Processor.Status.Ready()
          currentOp = None
        } else {
          computeCycles += 1
        }
      case Processor.Status.Ready() =>
        performInstruction()
    }
    cache.cycle()
  }

  private[this] def performInstruction(): Unit = {
    if (currentOp.isEmpty) loadInstruction()
    if (currentOp.isDefined) println_debug(s"Processor $id: ${currentOp.get}")
    currentOp match {
      case None => status = Processor.Status.Finished()
      case Some(ProcessorOp.Other(numCycles)) =>
        computeCycles += 1
        status =
          Processor.Status.Operation(finishedCycle = currentCycle + numCycles)
      case Some(ProcessorOp.Load(address)) =>
        numIdle += 1
        numLoad += 1
        status = Processor.Status.Cache()
        cache.request(this, CacheOp.Load(address))
      case Some(ProcessorOp.Store(address)) =>
        numIdle += 1
        numStore += 1
        status = Processor.Status.Cache()
        cache.request(this, CacheOp.Store(address))
    }
  }

  private[this] def loadInstruction(): Unit = {
    if (ops.hasNext) currentOp = Some(ops.next())
    else currentOp = None
  }

  override def requestCompleted(op: CacheOp): Unit = {
    println_debug(s"Processor $id: cache request completed, op $op")

    cache.log(op.address)

    currentOp match {
      case Some(ProcessorOp.Load(_)) | Some(ProcessorOp.Store(_)) =>
        numIdle += 1
        status = Processor.Status.Ready()
        currentOp = None
      case _ =>
        throw new RuntimeException("Got response from cache without request")
    }
  }

  override def toString: String = s"Processor $id"
}
