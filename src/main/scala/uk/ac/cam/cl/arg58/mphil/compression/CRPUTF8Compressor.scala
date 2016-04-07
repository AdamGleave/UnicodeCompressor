package uk.ac.cam.cl.arg58.mphil.compression

import java.io.{FileInputStream, FileOutputStream, OutputStream}

import uk.ac.cam.cl.arg58.mphil.utf8._
import uk.ac.cam.eng.ml.tcs27.compression._

/**
  * Created by adam on 07/04/16.
  */
object CRPUTF8Compressor {
  def main(args: Array[String]): Unit = {
    if (args.length < 1 || args.length > 3) {
      System.err.println("USAGE: <COMPRESS|DECOMPRESS> [in] [out]\n")
      System.exit(-1)
    }

    val arith : Coder = new Arith()
    val prior : Distribution[Token] = new UniformToken()
    val model : AdaptiveCode[Token] = new CRPU[Token](1, 1, 0, 1, prior)

    val inStream =
      if (args.length >= 2) {
        new FileInputStream(args(1))
      } else {
        System.in
      }

    val outStream =
      if (args.length >= 3) {
        new FileOutputStream(args(2))
      } else {
        System.out
      }

    args(0) match {
      case "COMPRESS" =>
        val out = new OutputStreamBitWriter(outStream)
        arith.start_encode(out)

        val utf8Decoder = new UTF8Decoder(inStream)
        for (token <- utf8Decoder) {
          model.encode(token, arith)
          model.learn(token)
        }
        model.encode(EOF(), arith)

        arith.finish_encode()
        out.close()
      case "DECOMPRESS" =>
        val in = new InputStreamBitReader(inStream)
        arith.start_decode(in)

        def decompress(): Unit = {
          val token = model.decode(arith)
          if (!token.equals(EOF())) {
            model.learn(token)
            val bytes = UTF8Encoder.tokenToBytes(token)
            outStream.write(bytes)
            decompress()
          }
        }
        decompress()

        arith.finish_decode()
        outStream.close()
    }
  }
}
