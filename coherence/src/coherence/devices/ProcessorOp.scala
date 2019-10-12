package coherence.devices

sealed trait ProcessorOp

object ProcessorOp {
  def apply(line: String): ProcessorOp = {
    line.split(' ').toList match {
      case opString :: valueString :: Nil =>
        val value = java.lang.Long.decode(valueString)
        opString match {
          case "0" => Load(value)
          case "1" => Store(value)
          case "2" => Other(value)
          case _ =>
            throw new RuntimeException(s"Unexpected label $opString in $line")
        }
      case _ =>
        throw new RuntimeException(s"Unexpected format in $line")
    }
  }
  case class Load(address: Long) extends ProcessorOp
  case class Store(address: Long) extends ProcessorOp
  case class Other(numCycles: Long) extends ProcessorOp
}
