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

import uk.ac.cam.cl.arg58.mphil.utf8._
import uk.ac.cam.eng.ml.tcs27.compression
import java.util

import scala.collection.JavaConversions

object CategoricalToken extends compression.SimpleMass[DetailedToken] with compression.Codable[DetailedToken]
                                                              with compression.AdaptiveCode[DetailedToken] {
  // TODO: these weights could be improved; empirical study, perhaps.
  final val weights = Array(90, 5, 2, 1, 1, 1)
  val cat : compression.SimpleMass[Integer] = new WeightedInteger(0, weights)
  val bases : Array[compression.SimpleMass[Integer]] = Array(
    new compression.UniformInteger(0, DUnicodeCharacter.Range - 1),
    new compression.UniformInteger(0, Overlong.Range - 1),
    new compression.UniformInteger(0, SurrogateCodePoint.Range - 1),
    new compression.UniformInteger(0, TooHigh.Range - 1),
    new compression.UniformInteger(0, DIllegalByte.Range - 1),
    new compression.UniformInteger(0, DEOF.Range - 1)
  )

  def mass(t: DetailedToken): Double = {
    val tokenTypeId = DTokenTypes.fromToken(t).id
    val catMass = cat.mass(tokenTypeId)
    val code = DTokenTypes.toInt(t)
    val tokenMass = bases(tokenTypeId).mass(code)
    catMass * tokenMass
  }

  def logMass(t: DetailedToken): Double = {
    val tokenTypeId = DTokenTypes.fromToken(t).id
    val catMass = cat.logMass(tokenTypeId)
    val code = DTokenTypes.toInt(t)
    val tokenMass = bases(tokenTypeId).logMass(code)
    catMass + tokenMass
  }

  override def encode(t: DetailedToken, ec: compression.Encoder): Unit = {
    val tokenTypeId = DTokenTypes.fromToken(t).id
    cat.encode(tokenTypeId, ec)
    val code = DTokenTypes.toInt(t)
    bases(tokenTypeId).encode(code, ec)
  }

  override def encode(t: DetailedToken, omit: util.Collection[DetailedToken], ec: compression.Encoder): Unit = {
    // TODO: exclusion coding for omitted tokens of other types?
    val tokenType = DTokenTypes.fromToken(t)
    cat.encode(tokenType.id, ec)
    val code = DTokenTypes.toInt(t)
    val omitCodes = JavaConversions.collectionAsScalaIterable(omit)
      .filter(t => DTokenTypes.fromToken(t) == tokenType)
      .map(t => DTokenTypes.toInt(t) : Integer)
    bases(tokenType.id).encode(code, JavaConversions.asJavaCollection(omitCodes), ec)
  }

  override def decode(dc: compression.Decoder): DetailedToken = {
    val tokenTypeId = cat.decode(dc)
    val code = bases(tokenTypeId).decode(dc)
    val tokenType = DTokenTypes(tokenTypeId)
    DTokenTypes.ofInt(tokenType, code)
  }

  override def decode(omit: util.Collection[DetailedToken], dc: compression.Decoder): DetailedToken = {
    // TODO: exclusion coding for omitted tokens of other types?
    val tokenTypeId = cat.decode(dc)
    val tokenType = DTokenTypes(tokenTypeId)
    val omitCodes = JavaConversions.collectionAsScalaIterable(omit)
      .filter(t => DTokenTypes.fromToken(t) == tokenType)
      .map(t => DTokenTypes.toInt(t) : Integer)
    val code = bases(tokenTypeId).decode(JavaConversions.asJavaCollection(omitCodes), dc)
    DTokenTypes.ofInt(tokenType, code)
  }

  override def sample(rnd: util.Random): DetailedToken = {
    val tokenTypeId = cat.sample(rnd)
    val code = bases(tokenTypeId).sample(rnd)
    val tokenType = DTokenTypes(tokenTypeId)
    DTokenTypes.ofInt(tokenType, code)
  }

  override def discreteMass(t: DetailedToken): Long = {
    val tokenTypeId = DTokenTypes.fromToken(t).id
    val catDiscreteMass = cat.discreteMass(tokenTypeId)
    val code = DTokenTypes.toInt(t)
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

  override def learn(t: DetailedToken): Unit = {
    // no-op
  }

  override def getPredictiveDistribution(): compression.Mass[DetailedToken] = {
    this
  }
}