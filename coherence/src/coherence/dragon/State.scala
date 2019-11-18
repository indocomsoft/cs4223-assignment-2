package coherence.dragon

sealed trait State

object State {
  case object E extends State
  case object SC extends State
  case object SM extends State
  case object M extends State
  case object I extends State
}
