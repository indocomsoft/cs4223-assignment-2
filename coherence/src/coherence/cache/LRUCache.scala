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

  def size: Int = data.size

  /**
    * Checks whether the given tag is located in this cache. Counts as a "use" in the LRU block replacement policy
    */
  def get(tag: Int): Option[CacheLine[State]] =
    if (data.contains(tag)) {
      data.remove(tag).foreach(data.put(tag, _))
      data.get(tag)
    } else {
      numCacheMisses += 1
      None
    }

  def immutableGet(tag: Int): Option[CacheLine[State]] = data.get(tag)

  /**
    * Adds a tag to the cache if it doesn't exist, otherwise updates a tag.
    * If a cache line needs to be evicted, it is returned together with its tag.
    */
  def update(tag: Int,
             cacheLine: CacheLine[State]): Option[(Int, CacheLine[State])] = {
    if (data.contains(tag)) {
      data.remove(tag)
      data.put(tag, cacheLine)
      None
    } else {
      data.put(tag, cacheLine)
      if (data.size > capacity) {
        data.headOption match {
          case Some((key, _)) => data.remove(key).map((key, _))
          case None           => None
        }
      } else {
        None
      }
    }
  }

}
