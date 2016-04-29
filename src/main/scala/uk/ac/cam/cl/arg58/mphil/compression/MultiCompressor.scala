package uk.ac.cam.cl.arg58.mphil.compression

/**
  * Created by adam on 29/04/16.
  */
object MultiCompressor {
  def main(args: Array[String]): Unit = {
    if (args.length != 0) {
      System.err.println("MultiCompressor does not take any arguments on the CLI.")
    }

    for (ln <- io.Source.stdin.getLines) {
      val c = new Compressor()
      val args = ln.split(" ")
      c.parse(args)
    }
  }
}
