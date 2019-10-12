package coherence.devices

import scala.io.Source

object Processor {
  def apply[Message, State](cache: Cache[Message, State],
                            source: Source): Processor[Message, State] =
    new Processor(cache, source.getLines().map(ProcessorOp(_)))
}

class Processor[Message, State](private[this] val cache: Cache[Message, State],
                                private[this] val ops: Iterator[ProcessorOp])
    extends CacheDelegate
    with Device {
  private[this] var currentCycle: Long = 0

  override def cycle(): Unit = {
    currentCycle += 1
  }

  override def requestCompleted(op: CacheOp): Unit = ???
}
