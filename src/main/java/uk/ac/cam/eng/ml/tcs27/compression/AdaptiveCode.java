/* Automated copy from build process */
/* $Id: AdaptiveCode.java,v 1.6 2015/08/11 11:28:16 chris Exp $ */
package uk.ac.cam.eng.ml.tcs27.compression;

/** Interface for adaptive data models with coding support.
  * <p>Such data models can be used for sequence compression,
  * allowing the model to adapt (via {@code learn(x)}) after
  * each symbol, before encoding the next symbol.</p>
  * <dl><dt><b>Notes:</b></dt><dd>
  * AdaptiveCodes over integers (bytes + EOF) can be used with
  * the ByteCompressor class to create fully fledged file
  * compressors.</dd></dl>
  * @see ByteCompressor */
public interface AdaptiveCode<X> extends Codable<X>, Mass<X> {

  /** Adapts the model by incorporating a single observation <var>x</var>.
    * @param x symbol observation */
  public void learn(X x);

  /** Returns the predictive probability mass of the supplied symbol.
    * @param x the symbol of interest
    * @return probability mass of the supplied symbol */
  public double mass(X x);

  /** Returns the predictive log probability mass of the supplied symbol.
    * @param x the symbol of interest
    * @return log probability mass of the supplied symbol */
  public double logMass(X x);

  /** Returns the current predictive distribution.
    * @return the predictive probability mass function */
  public Mass<X> getPredictiveDistribution();

}
