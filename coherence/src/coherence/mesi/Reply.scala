package coherence.mesi

sealed trait Reply

object Reply {
  case class Flush() extends Reply
  case class FlushOpt() extends Reply
  case class Ok() extends Reply
}
