package coherence.devices

import scala.io.Source

object Processor {
  def apply[Message](cache: Cache[Message],
                     source: Source): Processor[Message] =
    new Processor(cache, source.getLines().map(ProcessorOp(_)))
}

class Processor[Message](private[this] val cache: Cache[Message],
                         private[this] val ops: Iterator[ProcessorOp])
    extends Device {
  private[this] var currentCycle: Long = 0

  override def cycle(): Unit = {
    currentCycle += 1
  }
}
