package coherence.moesi

import coherence.devices.{CacheStatistics => AbstractCacheStatistics}

class CacheStatistics[State] extends AbstractCacheStatistics[State] {
  private[this] var _numPrivateAccess: Long = 0
  private[this] var _numSharedAccess: Long = 0

  override def numPrivateAccess: Long = _numPrivateAccess
  override def numSharedAccess: Long = _numSharedAccess

  var numO: Long = 0

  override def logState(state: State): Unit = {
    state match {
      case _: State.O.type => numO += 1
      case _               => ()
    }

    state match {
      case _: State.M.type => _numPrivateAccess += 1
      case _: State.E.type | _: State.S.type | _: State.O.type =>
        _numSharedAccess += 1
      case _: State.I.type =>
        throw new RuntimeException("Unexpected state I logged")
    }
  }
}
