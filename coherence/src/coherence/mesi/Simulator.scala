package coherence.mesi

import coherence.bus.Bus
import coherence.devices.Processor

import scala.io.Source

object Simulator {
  val NumProcessors = 4

  def apply(prefix: String,
            cacheSize: Int,
            associativity: Int,
            blockSize: Int): Simulator = {
    val sources =
      (0 until NumProcessors)
        .map(i => Source.fromFile(s"${prefix}_$i.data"))
        .toList
    new Simulator(sources, cacheSize, associativity, blockSize)
  }
}

class Simulator(sources: List[Source],
                cacheSize: Int,
                associativity: Int,
                blockSize: Int) {
  private[this] val bus: Bus[Message] = new Bus[Message]()
  private[this] val memory: Memory = new Memory(bus, blockSize)
  private[this] val caches: Array[Cache] =
    (0 until Simulator.NumProcessors).map { _ =>
      new Cache(cacheSize, associativity, blockSize, bus)
    }.toArray
  private[this] val processors: Array[Processor[State, Message]] =
    caches
      .zip(sources)
      .map { case (cache, source) => Processor(cache, source) }
      .toArray
}
