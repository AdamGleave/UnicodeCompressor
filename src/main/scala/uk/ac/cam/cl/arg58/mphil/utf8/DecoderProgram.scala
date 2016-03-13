package uk.ac.cam.cl.arg58.mphil.utf8

import java.io.{FileInputStream, InputStream}

/**
  * Created by adam on 11/03/16.
  */
object DecoderProgram {
  private def decode(in : InputStream, encode_after_decode : Boolean) = {
    val decoder = new Decoder(in)
    def parse() : Unit = {
      decoder.nextToken() match {
        case None =>
        case Some (token) =>
          if (encode_after_decode) {
            System.out.write(Encoder.tokenToBytes(token))
          } else {
            System.out.print(token)
          }
          parse()
      }
    }
    parse()
  }

  private def usage(): Unit = {
    System.err.println("USAGE: <IDENTITY|INFO> [fname1] [fname2] ...")
    System.exit(1)
  }

  def main(args: Array[String]) {
    if (args.length == 0) {
      usage()
    } else {
      var encode_after_decode : Boolean = args(0) match {
        case "IDENTITY" => true
        case "INFO" => false
        case _ => usage(); assert(false); false
      }
      val fnames = args.drop(1)
      if (fnames.length == 0) {
        decode(System.in, encode_after_decode)
      } else {
        for (fname <- fnames) {
          val in: InputStream = new FileInputStream(fname)
          decode(in, encode_after_decode)
        }
      }
    }
  }
}
