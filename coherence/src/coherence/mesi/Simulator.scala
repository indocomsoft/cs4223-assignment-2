package coherence.mesi

import coherence.bus.Bus

object Simulator {
  private[this] val bus = new Bus[Message]()
}
