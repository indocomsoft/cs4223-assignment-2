package coherence.cache

import scala.collection.mutable

class LRUCache[State](val capacity: Int) {

  /**
    * Provides mapping between tag to the cache line.
    * Use initialCapacity and load factor from java.util.HashMap
    */
  private[this] val data =
    new mutable.LinkedHashMap[Int, CacheLine[State]]()

  /**
    * Checks whether the given tag is located in this cache. Counts as a "use" in the LRU block replacement policy
    */
  def get(tag: Int): Option[CacheLine[State]] =
    if (data.contains(tag)) {
      data.remove(tag).map(v => data.update(tag, v))
      data.get(tag)
    } else {
      None
    }

  /**
    * Adds a tag to the cache. If a cache line needs to be evicted, it is returned.
    */
  def add(tag: Int, cacheLine: CacheLine[State]): Option[CacheLine[State]] = {
    get(tag) match {
      case Some(_) => None
      case None =>
        data.update(tag, cacheLine)
        data.headOption match {
          case Some((key, _)) if data.size > capacity => data.remove(key)
          case _                                      => None
        }
    }
  }
}
