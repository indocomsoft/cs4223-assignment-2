package coherence.cache

import scala.collection.mutable

class LRUCache[State](val capacity: Int) {

  /**
    * Provides mapping between tag to the cache line.
    * Use initialCapacity and load factor from java.util.HashMap
    */
  private[this] val data =
    new mutable.LinkedHashMap[Int, CacheLine[State]]()

  var numCacheMisses: Long = 0

  /**
    * Checks whether the given tag is located in this cache. Counts as a "use" in the LRU block replacement policy
    */
  def get(tag: Int): Option[CacheLine[State]] =
    if (data.contains(tag)) {
      data.remove(tag).map(v => data.update(tag, v))
      data.get(tag)
    } else {
      numCacheMisses += 1
      None
    }

  def immutableGet(tag: Int): Option[CacheLine[State]] = data.get(tag)

  /**
    * Adds a tag to the cache if it doesn't exist, otherwise updates a tag.
    * If a cache line needs to be evicted, it is returned.
    */
  def update(tag: Int,
             cacheLine: CacheLine[State]): Option[CacheLine[State]] = {
    data.update(tag, cacheLine)
    get(tag) match {
      case Some(_) => None
      case None =>
        data.headOption match {
          case Some((key, _)) if data.size > capacity => data.remove(key)
          case _                                      => None
        }
    }
  }

}
