package coherence.devices

import scala.collection.mutable

class Memory extends Device {
  private[this] var currentCycle: Long = 0

  // A map to finishedCycle
  private[this] val requests =
    mutable.Map[(MemoryDelegate, MemoryOp), Long]()

  def cycle(): Unit = {
    currentCycle += 1
    requests
      .filter { case (_, finishedCycle) => finishedCycle == currentCycle }
      .foreach {
        case (k @ (sender, op), _) =>
          requests.remove(k)
          sender.memoryOpCompleted(op)
      }
  }

  def operate(sender: MemoryDelegate, op: MemoryOp): Unit = {
    requests.put((sender, op), currentCycle + Memory.Latency)
  }

  def cancel(sender: MemoryDelegate, op: MemoryOp): Unit = {
    requests.remove((sender, op))
  }
}

object Memory {
  val Latency = 100
}
