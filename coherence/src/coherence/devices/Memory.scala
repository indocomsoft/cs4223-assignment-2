package coherence.devices

import scala.collection.mutable

class Memory extends Device {
  private[this] var currentCycle: Int = 0

  // A map from (op, address) to finishedCycle
  private[this] val requests =
    mutable.Map[(MemoryDelegate, MemoryOp, Int), Int]()

  def cycle(): Unit = {
    currentCycle += 1
    requests
      .filter { case (_, finishedCycle) => finishedCycle == currentCycle }
      .foreach {
        case (k @ (sender, op, address), _) =>
          requests.remove(k)
          sender.memoryOpCompleted(op, address)
      }
  }

  def operate(sender: MemoryDelegate, op: MemoryOp, address: Int): Unit = {
    requests.put((sender, op, address), currentCycle + Memory.Latency)
  }

  def cancel(sender: MemoryDelegate, op: MemoryOp, address: Int): Unit = {
    requests.remove((sender, op, address))
  }
}

object Memory {
  val Latency = 100
}
