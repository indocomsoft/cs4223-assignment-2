package coherence.mesi

sealed trait Message

case class BusRd(address: Int) extends Message
case class BusRdX(address: Int) extends Message
case class FlushOpt(address: Int) extends Message
