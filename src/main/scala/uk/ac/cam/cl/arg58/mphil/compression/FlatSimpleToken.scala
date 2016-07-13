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

import uk.ac.cam.cl.arg58.mphil.utf8.SimpleToken
import uk.ac.cam.eng.ml.tcs27.compression

import scala.collection.JavaConversions

object FlatSimpleToken {
  final val UniformToken = new FlatSimpleToken(new compression.UniformInteger(0, SimpleToken.Range - 1))

  private final val PolyaParser = new ParamsParser(Array("a"))
  def PolyaToken(params: String) = {
    val config = PolyaParser.parse(params)
    val alpha = config.get("a") match {
      case None => 0.5
      case Some(a) => a.toDouble
    }
    val polya = new compression.SBST(0, SimpleToken.Range - 1, alpha)
    new FlatSimpleToken(polya)
  }

  def PolyaTokenBase(base: IntegerMass, params: String) = {
    val config = PolyaParser.parse(params)
    val alpha = config.get("a") match {
      case None => 0.5
      case Some(a) => a.toDouble
    }
    val polya = new SBSTBase(0, SimpleToken.Range - 1, base, alpha)
    new FlatSimpleToken(polya)
  }
}

class FlatSimpleToken(base: compression.Distribution[Integer] with compression.AdaptiveCode[Integer])
  extends compression.SimpleMass[SimpleToken]
    with compression.Codable[SimpleToken]
    with compression.AdaptiveCode[SimpleToken] {
  def mass(t: SimpleToken): Double = base.mass(SimpleToken.toInt(t))
  def logMass(t: SimpleToken): Double = base.logMass(SimpleToken.toInt(t))

  private def tokensToInt(tokens: java.lang.Iterable[SimpleToken]) = {
    val tokensIterable = JavaConversions.iterableAsScalaIterable(tokens)
    val integersIterable = tokensIterable.map(t => SimpleToken.toInt(t) : Integer)
    JavaConversions.asJavaCollection(integersIterable)
  }

  override def encode(t: SimpleToken, ec: compression.Encoder): Unit = base.encode(SimpleToken.toInt(t), ec)
  override def encode(t: SimpleToken, omit: util.Collection[SimpleToken], ec: compression.Encoder): Unit =
    base.encode(SimpleToken.toInt(t), tokensToInt(omit), ec)

  override def decode(dc: compression.Decoder): SimpleToken = SimpleToken.ofInt(base.decode(dc))
  override def decode(omit: util.Collection[SimpleToken], dc: compression.Decoder): SimpleToken =
    SimpleToken.ofInt(base.decode(tokensToInt(omit), dc))

  override def discreteMass(t: SimpleToken): Long = base.discreteMass(SimpleToken.toInt(t))
  override def discreteTotalMass(): Long = base.discreteTotalMass()
  override def discreteTotalMass(col: java.lang.Iterable[SimpleToken]) =
    base.discreteTotalMass(tokensToInt(col))

  override def isFinite() = true

  override def toString() = "FlatSimpleToken"

  def sample(rnd: util.Random): SimpleToken = {
    SimpleToken.ofInt(base.sample(rnd))
  }

  override def learn(t: SimpleToken): Unit = base.learn(SimpleToken.toInt(t))

  override def getPredictiveDistribution(): compression.Mass[SimpleToken] = {
    this
  }
}
