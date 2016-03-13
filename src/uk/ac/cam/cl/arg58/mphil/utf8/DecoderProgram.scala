package uk.ac.cam.cl.arg58.mphil.utf8

import java.io.{FileInputStream, InputStream}

/**
  * Created by adam on 11/03/16.
  */
object DecoderProgram {
  def decode(in : InputStream) = {
    val decoder = new Decoder(in)
    def parse() : Unit = {
      decoder.nextToken() match {
        case None =>
        case Some (token) =>
          System.out.print(token)
          parse()
      }
    }
    parse()
  }

  def main(args: Array[String]) {
    if (args.length == 0) {
      decode(System.in)
    } else {
      for (fname <- args) {
        val in: InputStream = new FileInputStream(fname)
        decode(in)
      }
    }
  }
}
