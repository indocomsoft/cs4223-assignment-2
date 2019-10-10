package coherence.mesi

sealed trait Message

case class BusRd(address: Long) extends Message
case class BusRdX(address: Long) extends Message
case class FlushOpt(address: Long) extends Message
