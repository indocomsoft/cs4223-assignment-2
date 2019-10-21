package coherence.mesi

import coherence.devices.{CacheStatistics => AbstractCacheStatistics}

class CacheStatistics[State] extends AbstractCacheStatistics[State] {
  private[this] var _numPrivateAccess: Long = 0
  private[this] var _numSharedAccess: Long = 0

  override def numPrivateAccess: Long = _numPrivateAccess
  override def numSharedAccess: Long = _numSharedAccess

  override def logState(state: State): Unit =
    state match {
      case _: State.M.type                   => _numPrivateAccess += 1
      case _: State.E.type | _: State.S.type => _numSharedAccess += 1
      case _: State.I.type                   => ()
    }
}