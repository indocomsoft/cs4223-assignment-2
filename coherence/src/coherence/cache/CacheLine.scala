package coherence.cache

case class CacheLine[State](state: State, valid: Boolean)
