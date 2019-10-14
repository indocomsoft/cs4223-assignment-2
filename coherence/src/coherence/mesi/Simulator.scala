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
  private[this] val bus: Bus[Message, Reply] = new Bus[Message, Reply]()
  private[this] val memory: Memory = new Memory(bus, blockSize)
  private[this] val caches: Array[Cache] =
    (0 until Simulator.NumProcessors).map { id =>
      new Cache(id, cacheSize, associativity, blockSize, bus)
    }.toArray
  private[this] val processors: Array[Processor[State, Message, Reply]] =
    caches
      .zip(sources)
      .map { case (cache, source) => Processor(cache.id, cache, source) }
      .toArray

  def run(): Unit = {
    var currentCycle = 0
    while (processors.exists(!_.isFinished())) {
      currentCycle += 1
      println(currentCycle)
      processors.foreach(_.cycle())
      memory.cycle()
      bus.cycle()
    }
  }
}
