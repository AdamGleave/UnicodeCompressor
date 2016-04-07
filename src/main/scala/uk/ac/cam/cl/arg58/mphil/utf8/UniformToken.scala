package uk.ac.cam.cl.arg58.mphil.utf8

/**
  * Created by adam on 06/04/16.
  */
import java.util

import uk.ac.cam.eng.ml.tcs27.compression

// TODO: Could make it support AdaptiveCode, like UniformInteger, but not sure on the benefits.
class UniformToken extends compression.SimpleMass[Token] with compression.Codable[Token] {
  def mass(t: Token): Double = {
    1 / Token.TotalRange
  }

  def logMass(t: Token): Double = {
    -Math.log(Token.TotalRange)
  }

  override def encode(t: Token, ec: compression.Encoder): Unit = {
    val code = Token.toInt(t)
    ec.storeRegion(code, code + 1, Token.TotalRange)
  }

  override def encode(t: Token, omit: util.Collection[Token], ec: compression.Encoder): Unit = {
    encode(t, ec) // TODO
  }

  override def decode(dc: compression.Decoder): Token = {
    val aim = dc.getTarget(Token.TotalRange).toInt
    dc.loadRegion(aim, aim + 1, Token.TotalRange)
    Token.ofInt(aim)
  }

  override def decode(omit: util.Collection[Token], dc: compression.Decoder): Token = {
    decode(dc) // TODO
  }

  def sample(rnd : util.Random): Token = {
    Token.ofInt(rnd.nextInt(Token.TotalRange))
  }
}
