package coherence

case class Address(tag: Int, setIndex: Int)

object Address {
  def apply(address: Long,
            offsetBits: Int,
            setIndexOffsetBits: Int): Address = {
    var tmp = address
    tmp = tmp >> offsetBits
    val setIndex = tmp & ((1 << setIndexOffsetBits) - 1)
    tmp = tmp >> setIndexOffsetBits
    Address(tmp.toInt, setIndex.toInt)
  }
}
