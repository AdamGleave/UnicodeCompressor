package uk.ac.cam.cl.arg58.mphil.compression

import java.io._

import uk.ac.cam.cl.arg58.mphil.utf8._
import uk.ac.cam.eng.ml.tcs27.compression._

import scala.collection.JavaConversions

case class Config(compress: Boolean = true,
                  debug: Boolean = false,
                  algorithm: String => Unit = null,
                  params: String = "",
                  inFile: Option[File] = None,
                  outFile: Option[File] = None,
                  outputTokens: Boolean = false)

object Compressor {
  final val byteEOF = 256

  val arith: Coder = new Arith()

  var tokenPrior: Distribution[Token] = null
  var bytePrior: Distribution[Integer] = null
  var tokenModel: AdaptiveCode[Token] = null
  var byteModel: AdaptiveCode[Integer] = null

  var inStream: InputStream = null
  var outStream: OutputStream = null

  val compressors: Map[String, String => Unit] = Map(
    "none_uniform_token" -> uniformToken,
    "none_categorical_token" -> categoricalToken,
    "none_polya_token" -> polyaToken,
    "crp_uniform_token" -> crpUniformToken,
    "crp_categorical_token" -> crpCategoricalToken,
    "crp_polya_token" -> crpPolyaToken,
    "ppm_uniform_byte" -> ppmUniformByte,
    "ppm_uniform_token" -> ppmUniformToken,
    "ppm_polya_token" -> ppmPolyaToken
  )

  val parser = new scopt.OptionParser[Config]("Compressor") {
    opt[Unit]("debug") action { (_, c) => c.copy(debug = true)
      } text("write debugging output to stderr")
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
    tokenModel = FlatToken.UniformToken
  }

  def categoricalToken(params: String): Unit = {
    tokenModel = new CategoricalToken()
  }

  def polyaToken(params: String): Unit = {
    tokenModel = FlatToken.PolyaToken()
  }

  def crpUniformToken(params: String): Unit = {
    tokenPrior = FlatToken.UniformToken
    tokenModel = new CRPU[Token](1, 1, 0, 1, tokenPrior)
  }

  def crpCategoricalToken(params: String): Unit = {
    tokenPrior = new CategoricalToken()
    tokenModel = new CRPU[Token](1, 1, 0, 1, tokenPrior)
  }

  def crpPolyaToken(params: String): Unit = {
    tokenPrior = FlatToken.PolyaToken()
    tokenModel = new CRPU[Token](1, 1, 0, 1, tokenPrior)
  }

  def ppmUniformByte(params: String): Unit = {
    bytePrior = new UniformInteger(0, byteEOF)
    byteModel = new PPM(params, bytePrior)
  }

  def ppmUniformToken(params: String): Unit = {
    tokenPrior = FlatToken.UniformToken
    tokenModel = new PPM(params, tokenPrior)
  }

  def ppmPolyaToken(params: String): Unit = {
    tokenPrior = FlatToken.PolyaToken()
    tokenModel = new PPM(params, tokenPrior)
  }

  def compress(debug: Boolean): Unit = {
    val out = new OutputStreamBitWriter(outStream)
    arith.start_encode(out)

    val ec =
      if (debug)
        new DebugEncoder(arith)
      else
        arith

    if (tokenModel != null) {
      val utf8Decoder = new UTF8Decoder(inStream)
      for (token <- utf8Decoder) {
        tokenModel.encode(token, ec)
        if (tokenPrior != null)
          tokenPrior.learn(token)
        tokenModel.learn(token)
      }
      tokenModel.encode(EOF(), ec)
    } else {
      val byteIterable = JavaConversions.iterableAsScalaIterable(
        IOTools.byteSequenceFromInputStream(inStream))
      for (byte <- byteIterable) {
        val c = byte.toInt & 0xff // get its unsigned value
        byteModel.encode(c, ec)
        if (bytePrior != null)
          bytePrior.learn(c)
        byteModel.learn(c)
      }
      byteModel.encode(byteEOF, ec)
    }

    arith.finish_encode()
    out.close()
  }

  def decompress(debug: Boolean, outputTokens: Boolean): Unit = {
    val in = new InputStreamBitReader(inStream)

    arith.start_decode(in)
    val dc =
      if (debug)
        new DebugDecoder(arith)
      else
        arith

    if (tokenModel != null) {
      def decompress(): Unit = {
        val token = tokenModel.decode(dc)
        if (!token.equals(EOF())) {
          if (tokenPrior != null)
            tokenPrior.learn(token)
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
        val byte = byteModel.decode(dc)
        if (byte != byteEOF) {
          if (bytePrior != null)
            bytePrior.learn(byte)
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
          compress(config.debug)
        } else {
          // decompress
          decompress(config.debug, config.outputTokens)
        }
      case None =>
        // invalid arguments, terminate
        System.exit(1)
    }
  }
}