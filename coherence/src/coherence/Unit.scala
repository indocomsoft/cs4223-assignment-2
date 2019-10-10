package coherence

object Unit {
  class Word(val word: Int) extends AnyVal

  implicit class RichUnitInt(int: Int) {
    def word(): Word = new Word(int)
  }
}
