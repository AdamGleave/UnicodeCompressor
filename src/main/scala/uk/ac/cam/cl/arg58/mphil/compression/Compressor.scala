package uk.ac.cam.cl.arg58.mphil.compression

import java.io._

import uk.ac.cam.cl.arg58.mphil.utf8._
import uk.ac.cam.eng.ml.tcs27.compression._

import scala.collection.JavaConversions

case class Config(compress: Boolean = true,
                  algorithm: String => Unit = null,
                  params: String = "",
                  inFile: Option[File] = None,
                  outFile: Option[File] = None,
                  outputTokens: Boolean = false)

object Compressor {
  final val byteEOF = 256

  val arith: Coder = new Arith()

  var tokenModel: AdaptiveCode[Token] = null
  var byteModel: AdaptiveCode[Integer] = null

  var inStream: InputStream = null
  var outStream: OutputStream = null

  val compressors: Map[String, String => Unit] = Map(
    "uniform_token" -> uniformToken,
    "categorical_token" -> categoricalToken,
    "crp_uniform_token" -> crpUniformToken,
    "crp_categorical_token" -> crpCategoricalToken,
    "ppm_uniform_byte" -> ppmUniformByte,
    "ppm_uniform_token" -> ppmUniformToken
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

  def uniformToken(params: String): Unit = {
    tokenModel = new UniformToken()
  }

  def categoricalToken(params: String): Unit = {
    tokenModel = new CategoricalToken()
  }

  def crpUniformToken(params: String): Unit = {
    val prior = new UniformToken()
    tokenModel = new CRPU[Token](1, 1, 0, 1, prior)
  }

  def crpCategoricalToken(params: String): Unit = {
    val prior = new CategoricalToken()
    tokenModel = new CRPU[Token](1, 1, 0, 1, prior)
  }

  def ppmUniformByte(params: String): Unit = {
    val prior = new UniformInteger(0, byteEOF)
    byteModel = new PPM(params, prior)
  }

  def ppmUniformToken(params: String): Unit = {
    val prior = new UniformToken()
    tokenModel = new PPM(params, prior)
  }

  def compress(): Unit = {
    val out = new OutputStreamBitWriter(outStream)
    arith.start_encode(out)

    if (tokenModel != null) {
      val utf8Decoder = new UTF8Decoder(inStream)
      for (token <- utf8Decoder) {
        tokenModel.encode(token, arith)
        tokenModel.learn(token)
      }
      tokenModel.encode(EOF(), arith)
    } else {
      val byteIterable = JavaConversions.iterableAsScalaIterable(
        IOTools.byteSequenceFromInputStream(inStream))
      for (byte <- byteIterable) {
        val c = byte.toInt & 0xff // get its unsigned value
        byteModel.encode(c, arith)
        byteModel.learn(c)
      }
      byteModel.encode(byteEOF, arith)
    }

    arith.finish_encode()
    out.close()
  }

  def decompress(outputTokens: Boolean): Unit = {
    val in = new InputStreamBitReader(inStream)

    arith.start_decode(in)

    if (tokenModel != null) {
      def decompress(): Unit = {
        val token = tokenModel.decode(arith)
        if (!token.equals(EOF())) {
          tokenModel.learn(token)
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
    } else {
      def decompress(): Unit = {
        val byte = byteModel.decode(arith)
        if (byte != byteEOF) {
          byteModel.learn(byte)
          outStream.write(byte)
          decompress()
        }
      }
      decompress()
    }

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