package coherence

import scala.collection.mutable

object Helper {
  def isPowerOf2(n: Int): Boolean = n != 0 && (n & (n - 1)) == 0
}