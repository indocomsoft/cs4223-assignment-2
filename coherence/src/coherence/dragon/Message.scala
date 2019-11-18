package coherence.dragon

sealed trait Message

object Message {
  case class BusRd() extends Message
  case class BusUpt() extends Message
  case class Flush() extends Message
}
