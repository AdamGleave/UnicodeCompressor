package uk.ac.cam.cl.arg58.mphil.compression

import uk.ac.cam.cl.arg58.mphil.utf8._
import uk.ac.cam.eng.ml.tcs27.compression
import java.util

import scala.collection.JavaConversions

class CategoricalToken extends compression.SimpleMass[Token] with compression.Codable[Token]
                                                             with compression.AdaptiveCode[Token] {
  // TODO: these weights could be improved; empirical study, perhaps.
  final val weights = Array(90, 5, 2, 1, 1, 1)
  val cat : compression.SimpleMass[Integer] = new WeightedInteger(0, weights)
  val bases : Array[compression.SimpleMass[Integer]] = Array(
    new compression.UniformInteger(0, UnicodeCharacter.Range),
    new compression.UniformInteger(0, IllegalByte.Range),
    new compression.UniformInteger(0, Overlong.Range),
    new compression.UniformInteger(0, SurrogateCodePoint.Range),
    new compression.UniformInteger(0, TooHigh.Range),
    new compression.UniformInteger(0, EOF.Range)
  )

  def mass(t: Token): Double = {
    val tokenTypeId = TokenTypes.fromToken(t).id
    val catMass = cat.mass(tokenTypeId)
    val code = TokenTypes.toInt(t)
    val tokenMass = bases(tokenTypeId).mass(code)
    catMass * tokenMass
  }

  def logMass(t: Token): Double = {
    val tokenTypeId = TokenTypes.fromToken(t).id
    val catMass = cat.logMass(tokenTypeId)
    val code = TokenTypes.toInt(t)
    val tokenMass = bases(tokenTypeId).logMass(code)
    catMass + tokenMass
  }

  override def encode(t: Token, ec: compression.Encoder): Unit = {
    val tokenTypeId = TokenTypes.fromToken(t).id
    cat.encode(tokenTypeId, ec)
    val code = TokenTypes.toInt(t)
    bases(tokenTypeId).encode(code, ec)
  }

  override def encode(t: Token, omit: util.Collection[Token], ec: compression.Encoder): Unit = {
    // TODO: exclusion coding for omitted tokens of other types?
    val tokenType = TokenTypes.fromToken(t)
    cat.encode(tokenType.id, ec)
    val code = TokenTypes.toInt(t)
    val omitCodes = JavaConversions.collectionAsScalaIterable(omit)
      .filter(t => TokenTypes.fromToken(t) == tokenType)
      .map(t => TokenTypes.toInt(t) : Integer)
    bases(tokenType.id).encode(code, JavaConversions.asJavaCollection(omitCodes), ec)
  }

  override def decode(dc: compression.Decoder): Token = {
    val tokenTypeId = cat.decode(dc)
    val code = bases(tokenTypeId).decode(dc)
    val tokenType = TokenTypes(tokenTypeId)
    TokenTypes.ofInt(tokenType, code)
  }

  override def decode(omit: util.Collection[Token], dc: compression.Decoder): Token = {
    // TODO: exclusion coding for omitted tokens of other types?
    val tokenTypeId = cat.decode(dc)
    val tokenType = TokenTypes(tokenTypeId)
    val omitCodes = JavaConversions.collectionAsScalaIterable(omit)
      .filter(t => TokenTypes.fromToken(t) == tokenType)
      .map(t => TokenTypes.toInt(t) : Integer)
    val code = bases(tokenTypeId).decode(JavaConversions.asJavaCollection(omitCodes), dc)
    TokenTypes.ofInt(tokenType, code)
  }

  override def sample(rnd: util.Random): Token = {
    val tokenTypeId = cat.sample(rnd)
    val code = bases(tokenTypeId).sample(rnd)
    val tokenType = TokenTypes(tokenTypeId)
    TokenTypes.ofInt(tokenType, code)
  }

  override def discreteMass(t: Token): Long = {
    val tokenTypeId = TokenTypes.fromToken(t).id
    val catDiscreteMass = cat.discreteMass(tokenTypeId)
    val code = TokenTypes.toInt(t)
    val tokenDiscreteMass = bases(tokenTypeId).discreteMass(code)
    catDiscreteMass * tokenDiscreteMass
  }

  override def discreteTotalMass(): Long = {
    var total : Long = 0
    for (i <- 0 to weights.length - 1) {
      total += cat.discreteMass(i) * bases(i).discreteTotalMass()
    }
    total
  }

  override def learn(t: Token): Unit = {
    // no-op
  }

  override def getPredictiveDistribution(): compression.Mass[Token] = {
    this
  }
}