package coherence.mesi

sealed trait Reply

object Reply {
  case class Flush() extends Reply
  case class FlushOpt() extends Reply
  case class MemoryRead() extends Reply
  case class WriteBackOk() extends Reply
}
