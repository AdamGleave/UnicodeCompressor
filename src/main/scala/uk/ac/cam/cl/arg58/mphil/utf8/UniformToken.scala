package uk.ac.cam.cl.arg58.mphil.utf8

/**
  * Created by adam on 06/04/16.
  */
import java.util

import uk.ac.cam.eng.ml.tcs27.compression

import scala.collection.JavaConversions

// TODO: Could make it support AdaptiveCode, like UniformInteger, but not sure on the benefits.
class UniformToken() extends compression.SimpleMass[Token] with compression.Codable[Token] {
  val u = new compression.UniformInteger(0, Token.TotalRange)

  def mass(t: Token): Double = {
    1 / Token.TotalRange
  }

  def logMass(t: Token): Double = {
    -Math.log(Token.TotalRange)
  }

  override def encode(t: Token, ec: compression.Encoder): Unit = {
    u.encode(Token.toInt(t), ec)
  }

  private def tokensToInt(tokens: util.Collection[Token]) = {
    val tokensIterable = JavaConversions.collectionAsScalaIterable(tokens)
    val integersIterable = tokensIterable.map(t => Token.toInt(t) : Integer)
    JavaConversions.asJavaCollection(integersIterable)
  }

  override def encode(t: Token, omit: util.Collection[Token], ec: compression.Encoder): Unit = {
    u.encode(Token.toInt(t), tokensToInt(omit), ec)
  }

  override def decode(dc: compression.Decoder): Token = {
    Token.ofInt(u.decode(dc))
  }

  override def decode(omit: util.Collection[Token], dc: compression.Decoder): Token = {
    Token.ofInt(u.decode(tokensToInt(omit), dc))
  }

  override def discreteMass(t: Token) = {
    1
  }

  override def discreteTotalMass() = {
    Token.TotalRange
  }

  override def discreteTotalMass(col: java.lang.Iterable[Token]) = {
    JavaConversions.iterableAsScalaIterable(col).size
  }

  override def isFinite() = true

  override def toString(): Unit = {
    return "UniformToken"
  }

  def sample(rnd: util.Random): Token = {
    Token.ofInt(u.sample(rnd))
  }
}
