package uk.ac.cam.cl.arg58.mphil.compression

import java.util

import uk.ac.cam.cl.arg58.mphil.utf8.Token
import uk.ac.cam.eng.ml.tcs27.compression

import scala.collection.JavaConversions

object FlatToken {
  final val UniformToken = new FlatToken(new compression.UniformInteger(0, Token.Range - 1))

  def PolyaToken(a1: Int = 1, a2: Int = 2) = {
    val polya = new compression.SBST(0, Token.Range - 1, a1, a2)
    new FlatToken(polya)
  }
}

class FlatToken(base: compression.Distribution[Integer] with compression.AdaptiveCode[Integer])
    extends compression.SimpleMass[Token]
    with compression.Codable[Token]
    with compression.AdaptiveCode[Token] {
  def mass(t: Token): Double = base.mass(Token.toInt(t))
  def logMass(t: Token): Double = base.logMass(Token.toInt(t))

  private def tokensToInt(tokens: java.lang.Iterable[Token]) = {
    val tokensIterable = JavaConversions.iterableAsScalaIterable(tokens)
    val integersIterable = tokensIterable.map(t => Token.toInt(t) : Integer)
    JavaConversions.asJavaCollection(integersIterable)
  }

  override def encode(t: Token, ec: compression.Encoder): Unit = base.encode(Token.toInt(t), ec)
  override def encode(t: Token, omit: util.Collection[Token], ec: compression.Encoder): Unit =
    base.encode(Token.toInt(t), tokensToInt(omit), ec)

  override def decode(dc: compression.Decoder): Token = Token.ofInt(base.decode(dc))
  override def decode(omit: util.Collection[Token], dc: compression.Decoder): Token =
    Token.ofInt(base.decode(tokensToInt(omit), dc))

  override def discreteMass(t: Token): Long = base.discreteMass(Token.toInt(t))
  override def discreteTotalMass(): Long = base.discreteTotalMass()
  override def discreteTotalMass(col: java.lang.Iterable[Token]) =
    base.discreteTotalMass(tokensToInt(col))

  override def isFinite() = true

  override def toString() = "UniformToken"

  def sample(rnd: util.Random): Token = {
    Token.ofInt(base.sample(rnd))
  }

  override def learn(t: Token): Unit = base.learn(Token.toInt(t))

  override def getPredictiveDistribution(): compression.Mass[Token] = {
    this
  }
}
