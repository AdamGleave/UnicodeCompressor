package uk.ac.cam.cl.arg58.mphil.compression

import java.io._

import uk.ac.cam.cl.arg58.mphil.utf8._
import uk.ac.cam.eng.ml.tcs27.compression._

case class Config(compress: Boolean = true,
                  algorithm: () => Unit = null,
                  inFile: Option[File] = None,
                  outFile: Option[File] = None,
                  outputTokens: Boolean = false)

object UTF8Compressor {
  val arith: Coder = new Arith()

  var prior: Distribution[Token] = null
  var model: AdaptiveCode[Token] = null

  var inStream: InputStream = null
  var outStream: OutputStream = null

  val compressors: Map[String, () => Unit] = Map(
    "crp_uniform" -> crpUniform,
    "crp_categorical" -> crpCategorical
  )

  val parser = new scopt.OptionParser[Config]("CRPUTF8Compressor") {
    arg[String]("<algorithm>") action { (x, c) =>
      c.copy(algorithm = compressors(x)) } validate { x =>
        if (compressors.contains(x)) success else failure("unknown algorithm " + x)
      } text("method to compress with")
    cmd("compress") action { (_, c) =>
      c.copy(compress = true) } text("compress") children(
      arg[File]("input") optional() action { (x,c) =>
        c.copy(inFile = Some(x)) } text("uncompressed input"),
      arg[File]("output") optional() action { (x,c) =>
        c.copy(outFile = Some(x)) } text("compressed output")
    )
    cmd("decompress") action { (_,c) =>
      c.copy(compress = false) } text("decompress") children (
      opt[Unit]("tokens") action { (_, c) =>
        c.copy(outputTokens = true) } text("write serialised representation of Unicode tokens"),
      arg[File]("input") optional() action { (x,c) =>
        c.copy(inFile = Some(x)) } text("compressed input"),
      arg[File]("output") optional() action { (x,c) =>
        c.copy(outFile = Some(x)) } text("decompressed output")
    )
  }

  def crpUniform(): Unit = {
    prior = new UniformToken()
    model = new CRPU[Token](1, 1, 0, 1, prior)
  }

  def crpCategorical(): Unit = {
    prior = new CategoricalToken()
    model = new CRPU[Token](1, 1, 0, 1, prior)
  }

  def compress(): Unit = {
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
  }

  def decompress(outputTokens: Boolean): Unit = {
    val in = new InputStreamBitReader(inStream)

    arith.start_decode(in)

    def decompress(): Unit = {
      val token = model.decode(arith)
      if (!token.equals(EOF())) {
        model.learn(token)
        val bytes =
          if (outputTokens) {
            token.toString().getBytes()
          } else {
            UTF8Encoder.tokenToBytes(token)
          }
        outStream.write(bytes)
        decompress()
      }
    }
    decompress()

    arith.finish_decode()
    outStream.close()
  }

  def main(args: Array[String]): Unit = {
    parser.parse(args, Config()) match {
      case Some(config) =>
        // input and output files
        inStream = config.inFile match {
          case Some(f) => new FileInputStream(f)
          case None => System.in
        }

        outStream = config.outFile match {
          case Some(f) => new FileOutputStream(f)
          case None => System.out
        }

        // initialise the model
        config.algorithm()

        // compress/decompress
        if (config.compress) {
          // compress
          compress()
        } else {
          // decompress
          decompress(config.outputTokens)
        }
      case None =>
        // invalid arguments, terminate
        System.exit(1)
    }
  }
}