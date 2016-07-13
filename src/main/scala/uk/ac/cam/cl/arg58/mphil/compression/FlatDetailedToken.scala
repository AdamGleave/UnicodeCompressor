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

import uk.ac.cam.cl.arg58.mphil.utf8.DetailedToken
import uk.ac.cam.eng.ml.tcs27.compression

import scala.collection.JavaConversions

object FlatDetailedToken {
  final val UniformToken = new FlatDetailedToken(new compression.UniformInteger(0, DetailedToken.Range - 1))

  private final val PolyaParser = new ParamsParser(Array("a"))
  def PolyaToken(params: String) = {
    val config = PolyaParser.parse(params)
    val alpha = config.get("a") match {
      case None => 0.5
      case Some(a) => a.toDouble
    }
    val polya = new compression.SBST(0, DetailedToken.Range - 1, alpha)
    new FlatDetailedToken(polya)
  }
}

class FlatDetailedToken(base: compression.Distribution[Integer] with compression.AdaptiveCode[Integer])
    extends compression.SimpleMass[DetailedToken]
    with compression.Codable[DetailedToken]
    with compression.AdaptiveCode[DetailedToken] {
  def mass(t: DetailedToken): Double = base.mass(DetailedToken.toInt(t))
  def logMass(t: DetailedToken): Double = base.logMass(DetailedToken.toInt(t))

  private def tokensToInt(tokens: java.lang.Iterable[DetailedToken]) = {
    val tokensIterable = JavaConversions.iterableAsScalaIterable(tokens)
    val integersIterable = tokensIterable.map(t => DetailedToken.toInt(t) : Integer)
    JavaConversions.asJavaCollection(integersIterable)
  }

  override def encode(t: DetailedToken, ec: compression.Encoder): Unit = base.encode(DetailedToken.toInt(t), ec)
  override def encode(t: DetailedToken, omit: util.Collection[DetailedToken], ec: compression.Encoder): Unit =
    base.encode(DetailedToken.toInt(t), tokensToInt(omit), ec)

  override def decode(dc: compression.Decoder): DetailedToken = DetailedToken.ofInt(base.decode(dc))
  override def decode(omit: util.Collection[DetailedToken], dc: compression.Decoder): DetailedToken =
    DetailedToken.ofInt(base.decode(tokensToInt(omit), dc))

  override def discreteMass(t: DetailedToken): Long = base.discreteMass(DetailedToken.toInt(t))
  override def discreteTotalMass(): Long = base.discreteTotalMass()
  override def discreteTotalMass(col: java.lang.Iterable[DetailedToken]) =
    base.discreteTotalMass(tokensToInt(col))

  override def isFinite() = true

  override def toString() = "UniformToken"

  def sample(rnd: util.Random): DetailedToken = {
    DetailedToken.ofInt(base.sample(rnd))
  }

  override def learn(t: DetailedToken): Unit = base.learn(DetailedToken.toInt(t))

  override def getPredictiveDistribution(): compression.Mass[DetailedToken] = {
    this
  }
}
