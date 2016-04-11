package uk.ac.cam.cl.arg58.mphil.compression

import java.io._

import uk.ac.cam.cl.arg58.mphil.utf8._
import uk.ac.cam.eng.ml.tcs27.compression._

case class Config(compress: Boolean = true,
                  algorithm: String => Unit = null,
                  params: String = "",
                  inFile: Option[File] = None,
                  outFile: Option[File] = None,
                  outputTokens: Boolean = false)

object Compressor {
  val arith: Coder = new Arith()

  var model: AdaptiveCode[Token] = null

  var inStream: InputStream = null
  var outStream: OutputStream = null

  val compressors: Map[String, String => Unit] = Map(
    "uniform" -> uniform,
    "categorical" -> categorical,
    "crp_uniform" -> crpUniform,
    "crp_categorical" -> crpCategorical,
    "ppm_uniform" -> ppmUniform
  )

  val parser = new scopt.OptionParser[Config]("Compressor") {
    opt[Unit]("tokens") action { (_, c) => c.copy(outputTokens = true)
      } text("write serialised representation of Unicode tokens (decompress mode only)")
    opt[String]("params") action { (x,c) => c.copy(params = x)
      } text("parameters to be passed to the compression algorithm (implementation specific)")
    arg[String]("<algorithm>") action { (x, c) =>
      c.copy(algorithm = compressors(x)) } validate { x =>
        if (compressors.contains(x)) success else failure("unknown algorithm " + x)
      } text("method to compress with")
    arg[String]("<mode>") action { (x, c) => x match {
        case "compress" => c.copy(compress = true)
        case "decompress" => c.copy(compress = false)
      } } validate { x => x match {
        case "compress" => success
        case "decompress" => success
        case x => failure("Unrecognised argument " + x + ", should be compress or decompress.")
      } } text("mode, either compress or decompress")
    arg[File]("input") optional() action { (x,c) =>
      c.copy(inFile = Some(x)) } text("input file (default: stdin)")
    arg[File]("output") optional() action { (x,c) =>
      c.copy(outFile = Some(x)) } text("output file (default: stdout)")
  }

  def uniform(params: String): Unit = {
    model = new UniformToken()
  }

  def categorical(params: String): Unit = {
    model = new CategoricalToken()
  }

  def crpUniform(params: String): Unit = {
    val prior = new UniformToken()
    model = new CRPU[Token](1, 1, 0, 1, prior)
  }

  def crpCategorical(params: String): Unit = {
    val prior = new CategoricalToken()
    model = new CRPU[Token](1, 1, 0, 1, prior)
  }

  def ppmUniform(params: String): Unit = {
    val prior = new UniformToken()
    model = new PPM(params, prior)
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
        config.algorithm(config.params)

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