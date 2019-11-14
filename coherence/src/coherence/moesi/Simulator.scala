package coherence.moesi

import coherence.Debug._
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
  private[this] val bus: Bus[Message, Reply] =
    new Bus[Message, Reply](new BusStatistics())
  private[this] val memory: Memory = new Memory(bus, blockSize)
  private[this] val caches: Array[Cache] =
    (0 until Simulator.NumProcessors).map { id =>
      new Cache(
        id,
        cacheSize,
        associativity,
        blockSize,
        bus,
        new CacheStatistics()
      )
    }.toArray
  private[this] val processors: Array[Processor[State, Message, Reply]] =
    caches
      .zip(sources)
      .map { case (cache, source) => Processor(cache.id, cache, source) }
      .toArray

  def run(): Unit = {
    var currentCycle: Long = 0
    while (processors.exists(!_.isFinished())) {
      currentCycle += 1
      println_debug(currentCycle)
      processors.foreach(_.cycle())
      memory.cycle()
      bus.cycle()
    }
    require(currentCycle == processors.map(_.totalCycles).max)
    println("Summary")
    println("======")
    println(s"Overall execution cycle = $currentCycle")
    println("Total execution cycles per core:")
    processors.foreach { processor =>
      println(s"- $processor = ${processor.totalCycles}")
    }
    println("======")
    println("Number of compute cycles per core:")
    processors.foreach { processor =>
      println(s"- $processor = ${processor.totalComputeCycles}")
    }
    println("======")
    println("Number of load/store instructions per core:")
    processors.foreach { processor =>
      println(
        s"- $processor: Load = ${processor.numLoadInstructions}, Store = ${processor.numStoreInstructions}"
      )
    }
    println("======")
    println("Number of idle cycles")
    processors.foreach { processor =>
      println(s"- $processor = ${processor.numIdleCycles}")
    }
    println("======")
    println("Data cache miss rate for each core")
    processors.foreach { processor =>
      println(
        s"- $processor = ${processor.cache.cacheMissRate} (${processor.cache.numCacheMisses}/${processor.cache.totalRequests})"
      )
    }
    println("======")
    println("Amount of Data traffic in bytes on the bus")
    println(s"= ${bus.stats.dataTraffic}")
    println("======")
    println("Number of invalidations or updates on the bus")
    println(s"= ${bus.stats.numInvalidationUpdate}")
    println("======")
    println("Distribution of accesses to private data versus shared data")
    caches.foreach { cache =>
      println(
        s"- $cache: numPrivateAccess = ${cache.stats.numPrivateAccess}, numSharedAccess = ${cache.stats.numSharedAccess}"
      )
    }
    println("======")
    println("Number of times each state is entered")
    caches.foreach { cache =>
      println(
        s"- $cache: numO = ${cache.stats.asInstanceOf[CacheStatistics[State]].numO}"
      )
    }
  }
}
