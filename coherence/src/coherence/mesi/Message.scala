package coherence.mesi

sealed trait Message

object Message {
  case class BusRd() extends Message
  case class BusRdX() extends Message
  case class FlushOpt() extends Message
}
