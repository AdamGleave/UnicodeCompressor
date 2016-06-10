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

import java.util

import uk.ac.cam.cl.arg58.mphil.utf8.Token
import uk.ac.cam.eng.ml.tcs27.compression

import scala.collection.JavaConversions

object FlatToken {
  final val UniformToken = new FlatToken(new compression.UniformInteger(0, Token.Range - 1))

  private final val PolyaParser = new ParamsParser(Array("a"))
  def PolyaToken(params: String) = {
    val config = PolyaParser.parse(params)
    val alpha = config.get("a") match {
      case None => 0.5
      case Some(a) => a.toDouble
    }
    val polya = new compression.SBST(0, Token.Range - 1, alpha)
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
