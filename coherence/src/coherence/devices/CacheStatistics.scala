package coherence.devices

abstract class CacheStatistics[State] {
  def numPrivateAccess: Long
  def numSharedAccess: Long

  def logState(state: State): Unit
}
