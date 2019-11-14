package coherence.moesi

sealed trait State

object State {
  case object M extends State
  case object O extends State
  case object E extends State
  case object S extends State
  case object I extends State
}
