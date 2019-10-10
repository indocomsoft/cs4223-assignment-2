package coherence

case class Input(protocol: Protocol.Value,
                 prefix: String,
                 cacheSize: Int,
                 associativity: Int,
                 blockSize: Int)

object Main extends App {
  override def main(args: Array[String]): Unit =
    args.toList match {
      case protocolInput :: prefixInput :: cacheInput :: assocInput :: blockInput :: Nil =>
        val eitherInput = for {
          protocol <- parseProtocol(protocolInput)
          prefix <- parsePrefix(prefixInput)
          cacheSize <- parseCacheSize(cacheInput)
          associativity <- parseAssociativity(assocInput)
          blockSize <- parseBlockSize(blockInput)
        } yield
          Input(
            protocol = protocol,
            prefix = prefix,
            cacheSize = cacheSize,
            associativity = associativity,
            blockSize = blockSize
          )
        eitherInput match {
          case Right(input) => ???
          case Left(errorMessage) =>
            printUsage(errorMessage)
        }
      case _ =>
        printUsage("Wrong number of arguments")
    }

  private[this] def parseProtocol(
    input: String
  ): Either[String, Protocol.Value] = {
    try {
      Right(Protocol.withName(input))
    } catch {
      case _: NoSuchElementException => Left("Invalid protocol")
    }
  }

  private[this] def parsePrefix(input: String): Either[String, String] =
    Right(input)

  private[this] def parseCacheSize(input: String): Either[String, Int] =
    try {
      Right(Integer.parseInt(input, 10))
    } catch {
      case _: NumberFormatException => Left("cache_size is not a number")
    }

  private[this] def parseAssociativity(input: String): Either[String, Int] =
    try {
      Right(Integer.parseInt(input, 10))
    } catch {
      case _: NumberFormatException => Left("associativity is not a number")
    }

  private[this] def parseBlockSize(input: String): Either[String, Int] =
    try {
      Right(Integer.parseInt(input, 10))
    } catch {
      case _: NumberFormatException => Left("block_size is not a number")
    }

  private[this] def printUsage(error: String): Unit = {
    println(s"Error: $error")
    println("---------")
    println(
      "Arguments: <protocol> <input_file_prefix> <cache_size> <associativity> <block_size>"
    )
    println("")
    println("protocol = MESI or Dragon")
    println("The sizes are in bytes")
    println("")
    println("Assumptions:")
    println("block_size must be a power of 2")
    println("cache_size is divisible by block_size")
    println("number of blocks is divisible by associativity")
    println("number of sets is a power of 2")
    System.exit(1)
  }
}
