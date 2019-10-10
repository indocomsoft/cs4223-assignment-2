package coherence.mesi

import coherence.bus.Bus
import coherence.devices.Processor

object Simulator {
  val NumProcessors = 4
  private[this] val bus = new Bus[Message]()
}
