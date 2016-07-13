/* Copyright (C) 2016, Adam Gleave

  This program is free software: you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package uk.ac.cam.cl.arg58.mphil.compression

import java.io._

import uk.ac.cam.cl.arg58.mphil.utf8._
import uk.ac.cam.eng.ml.tcs27.compression._

import scala.collection.JavaConversions

object Modes extends Enumeration {
  val Compress, Measure, Decompress = Value
}

case class Config(mode: Modes.Value = Modes.Compress,
                  debug: Boolean = false,
                  base: (String => Unit, String) = null,
                  models: Seq[(String => Unit, String)] = Seq(),
                  params: String = "",
                  inFile: Option[File] = None,
                  outFile: Option[File] = None,
                  outputTokens: Boolean = false)

abstract class Model()
case class ByteModel(models: Seq[Distribution[Integer]]) extends Model
case class TokenModel(models: Seq[Distribution[Token]]) extends Model
case class NoModel() extends Model

class Compressor {
  final val ByteEOF = 256

  val arith: Coder = new Arith()

  var models : Model = NoModel()

  var inStream: InputStream = null
  var outStream: OutputStream = null

  val base: Map[String, String => Unit] = Map(
    "uniform_token" -> uniformToken,
    "categorical_token" -> categoricalToken,
    "polya_token" -> polyaToken,
    "polya_token_base" -> polyaTokenBase,
    "uniform_byte" -> uniformByte,
    "lzw_byte" -> lzwByte,
    "polya_byte" -> polyaByte
  )
  val compressors: Map[String, String => Unit] = Map(
    "crp" -> crp,
    "lzwEscape" -> lzwEscape,
    "ppm" -> ppm
  )

  private def lookup(table: Map[String, String => Unit],
                     config: String): Either[String, (String => Unit, String)] = {
    val kp = config.split(":", 2) match {
      case Array(key) => Right(key, "")
      case Array(key, params) => Right(key, params)
      case _ => Left("Illegal configuration string '" + config + "'")
    }
    kp match {
      case Left(err) => Left(err)
      case Right(x) =>
        val (key, params) = x
        table.get(key) match {
          case Some(compressor) => Right(compressor, params)
          case None => Left("Unknown algorithm '" + key + "'")
        }
    }
  }

  private def lookup_copy(table: Map[String, String => Unit],
                          config: String): (String => Unit, String) =
    lookup(table, config).right.get

  private def lookup_validate(table: Map[String, String => Unit],
                          config: String): Either[String, Unit] = lookup(table, config) match {
    case Left(err) => Left(err)
    case Right(_) => Right()
  }

  val parser = new scopt.OptionParser[Config]("Compressor") {
    opt[Unit]("debug") action { (_, c) => c.copy(debug = true)
      } text("write debugging output to stderr")
    opt[Unit]("tokens") action { (_, c) => c.copy(outputTokens = true)
      } text("write serialised representation of Unicode tokens (decompress mode only)")
    arg[String]("<mode>") action { (x, c) => x match {
      case "compress" => c.copy(mode = Modes.Compress)
      case "decompress" => c.copy(mode = Modes.Decompress)
      case "measure" => c.copy(mode = Modes.Measure)
    } } validate { x => x match {
      case "compress" => success
      case "decompress" => success
      case "measure" => success
      case x => failure("Unrecognised argument " + x + ", should be compress or decompress.")
    } } text("mode, either compress or decompress")
    arg[File]("input") optional() action { (x,c) =>
      c.copy(inFile = Some(x)) } text("input file (default: stdin)")
    arg[File]("output") optional() action { (x,c) =>
      c.copy(outFile = Some(x)) } text("output file (default: stdout)")
    opt[String]("base") required() action { (x, c) =>
      c.copy(base = lookup_copy(base, x)) } validate { x =>
      lookup_validate(base, x) } text("method to compress with")
    opt[String]("model") unbounded() action { (x,c) =>
      val model = lookup_copy(compressors, x)
      c.copy(models = c.models :+ model) } validate { x =>
      lookup_validate(compressors, x) } text("models to apply, in order given")
  }

  private def tokenBase(model: Distribution[Token]): Unit = models match {
    case NoModel() =>
      models = TokenModel(Array(model))
    case _ =>
      throw new AssertionError("Base distribution cannot be layered on top of existing models")
  }

  private def uniformToken(params: String): Unit = tokenBase(FlatToken.UniformToken)
  private def categoricalToken(params: String): Unit = tokenBase(CategoricalToken)
  private def polyaToken(params: String): Unit = tokenBase(FlatToken.PolyaToken(params))
  private def polyaTokenBase(params: String): Unit = tokenBase(FlatToken.PolyaTokenBase(params))

  private def byteBase(model: Distribution[Integer]): Unit = models match {
    case NoModel() =>
      models = ByteModel(Array(model))
    case _ =>
      throw new AssertionError("Base distribution cannot be layered on top of existing models")
  }

  private def uniformByte(params: String): Unit = byteBase(new UniformInteger(0, ByteEOF))
  private def polyaByte(params: String): Unit = byteBase(new SBST(0, ByteEOF))
  private def lzwByte(params: String): Unit = {
    val alphabet = JavaConversions
      .asJavaIterable(0 to ByteEOF)
      .asInstanceOf[java.lang.Iterable[Integer]]
    val lzw = new LZW[Integer](alphabet, ByteEOF)
    byteBase(lzw)
  }

  private def crp(params: String): Unit = {
    models = models match {
      case NoModel() =>
        throw new AssertionError("CRP needs a base distribution.")
      case ByteModel(models) =>
        val model = CRPU.createNew(params, models.last)
        ByteModel(models:+model)
      case TokenModel(models) =>
        val model = CRPU.createNew(params, models.last)
        TokenModel(models:+model)
    }
  }

  private def lzwEscape(params: String): Unit = {
    models = models match {
      case NoModel() =>
        throw new AssertionError("LZWEscape needs a base distribution.")
      case ByteModel(models) =>
        val model = new LZWEscape[Integer](models.last, ByteEOF)
        ByteModel(models:+model)
      case TokenModel(models) =>
        val model = new LZWEscape[Token](models.last, EOF())
        TokenModel(models:+model)
    }
  }

  private def ppm(params: String): Unit = {
    models = models match {
      case NoModel() =>
        throw new AssertionError("CRP needs a base distribution.")
      case ByteModel(models) =>
        val model = new PPM(params, models.last)
        ByteModel(models:+model)
      case TokenModel(models) =>
        val model = new PPM(params, models.last)
        TokenModel(models:+model)
    }
  }

  private def compress(out: BitWriter, debug: Boolean): Unit = {
    arith.start_encode(out)

    val ec =
      if (debug)
        new DebugEncoder(arith)
      else
        arith

    models match {
      case TokenModel(models) =>
        val utf8Decoder = new UTF8Decoder(inStream)
        for (token <- utf8Decoder) {
          models.last.encode(token, ec)
          models.foreach(d => d.learn(token))
        }
        models.last.encode(EOF(), ec)
      case ByteModel(models) =>
        val byteIterable = JavaConversions.iterableAsScalaIterable(
          IOTools.byteSequenceFromInputStream(inStream))
        for (byte <- byteIterable) {
          val c = byte.toInt & 0xff // get its unsigned value
          models.last.encode(c, ec)
          models.foreach(d => d.learn(c))
        }
        models.last.encode(ByteEOF, ec)
      case NoModel() =>
        throw new AssertionError("No model.")
    }

    arith.finish_encode()
    out.close()
  }

  private def decompress(debug: Boolean, outputTokens: Boolean): Unit = {
    val in = new InputStreamBitReader(inStream)

    arith.start_decode(in)
    val dc =
      if (debug)
        new DebugDecoder(arith)
      else
        arith

    models match {
      case TokenModel(models) =>
        def decompress(): Unit = {
          val token = models.last.decode(dc)
          if (!token.equals(EOF())) {
            models.foreach(d => d.learn(token))
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
      case ByteModel(models) =>
        def decompress(): Unit = {
          val byte = models.last.decode(dc)
          if (byte != ByteEOF) {
            models.foreach(d => d.learn(byte))
            outStream.write(byte)
            decompress()
          }
        }
        decompress()
      case NoModel() =>
        throw new AssertionError("No model.");
    }

    arith.finish_decode()
    outStream.close()
  }

  def parse(args: Array[String]) = {
    parser.parse(args, Config()) match {
      case Some(config) =>
        // input and output files
        inStream = config.inFile match {
          case Some(f) => new BufferedInputStream(new FileInputStream(f))
          case None => System.in
        }

        outStream = config.outFile match {
          case Some(f) => new BufferedOutputStream(new FileOutputStream(f))
          case None => System.out
        }

        // set up the base distribution first
        val (base_algo, base_params) = config.base
        base_algo(base_params)
        // apply transforms in order (there need not be any)
        for ((algo, params) <- config.models) {
          algo(params)
        }

        // compress/decompress
        config.mode match {
          case Modes.Compress =>
            val out = new OutputStreamBitWriter(outStream)
            compress(out, config.debug)
          case Modes.Measure =>
            val out = new BitCounter()
            compress(out, config.debug)
            val results = new PrintWriter(outStream)
            results.write("BITS WRITTEN: " + out.bitsWritten() + "\n")
            results.flush()
          case Modes.Decompress => decompress(config.debug, config.outputTokens)
        }
      case None =>
        // invalid arguments, terminate
        System.exit(1)
    }
  }
}

object Compressor {
  def main(args: Array[String]): Unit = {
    val c = new Compressor
    c.parse(args)
  }
}