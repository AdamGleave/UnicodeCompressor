/* Automated copy from build process */
/* $Id: PPMUrn.java,v 1.12 2013/10/12 18:19:38 chris Exp $ */
package uk.ac.cam.eng.ml.tcs27.compression;

import java.util.Random;
import java.util.ArrayList;
import java.util.HashMap;

/** Interface of urn schemes for PPM algorithms and memoizers. */
public abstract class PPMUrn<X,Y> {

  /** Returns the urn scheme's current context node. */
  public abstract PPMTrie<X,Y> getContext();
  
  /** Sets the urn scheme's current context.
    * Note: this can be used to replace the entire context tree. */
  public abstract void setContext(PPMTrie<X,Y> ctx);

  /** Initialises the given context node. */
  public abstract void init(PPMTrie<X,Y> ctx);

  /** Updates urn data and structure to incorporate a datapoint. */
  public abstract void learn(X x);

  /** Returns the predictive probability mass of a given element. */
  public abstract double mass(X x, Mass<X> base);

  /** Computes the predictive probability distribution. */
  public abstract Distribution<X> getPredictive(Mass<X> base, Iterable<X> set);

  /** An encoding method. */
  public void encode(X x, Mass<X> base, Iterable<X> set, Encoder ec) {
    Mass<X> predictive = getPredictive(base,set);
    // (new CumulativeOnDemand<X>(predictive,set)).encode(x,ec);
    long budget = ec.getRange();
    HashMap<X,Long> cm = Coding.getDiscreteMass(predictive,set,budget);
    Coding.encode(x,cm,set,ec);
  }
  
  /** A decoding method. */
  public X decode(Mass<X> base, Iterable<X> set, Decoder dc) {
    Mass<X> predictive = getPredictive(base,set);
    //return (new CumulativeOnDemand<X>(predictive,set)).decode(dc);
    long budget = dc.getRange();
    HashMap<X,Long> cm = Coding.getDiscreteMass(predictive,set,budget);
    return Coding.decode(cm,set,dc);
  }

  /** A sampling method. */
  public X sample(Random rnd, Mass<X> base, Iterable<X> set) {
    Distribution<X> predictive = getPredictive(base,set);
    return predictive.sample(rnd);
  }
  
  /** A method for finding the least predicted element. */
  public X least(Mass<X> base, Iterable<X> set) {
    Distribution<X> predictive = getPredictive(base,set);
    return Samplers.leastMass(predictive,set);
  }

  /** Creates a new trie. */
  public PPMTrie<X,Y> createTrie() {
    return new PPMTrie<X,Y>();
  }

  /** Returns the depth of the current node.
    * The root node has depth 0. */
  public int getDepth(PPMTrie<X,Y> node) {
    int depth = 0;
    while (node.vine != null) {
      node = node.vine;
      depth++;
    }
    return depth;
  }



  /* ================================================================== */
  /* METHODS FOR USING CONJUGATE GRADIENT PARAMETER OPTIMISATION  */
  /* ================================================================== */

  public ArrayList<Double> createGradientList() {
    //return new ArrayList<Double>();
    throw new UnsupportedOperationException();
  }

  /** Computes the gradients of the logMass function
    * for a given symbol and context, and adds them to
    * the specified ArrayList.
    * @param x the symbol
    * @param ctx the context node
    * @param base the base distribution
    * @param pars parameter array to add the gradients to. */
  public void addLogMassGradients(X x, PPMTrie<X,Y> ctx,
                                  Mass<X> base, ArrayList<Double> pars) {
    throw new UnsupportedOperationException();
  }
  
  /** Returns the optimizable parameters of the current urn.
    * Useful when using a gradient optimiser.
    * @see #encodeParameters(ArrayList) */
  public ArrayList<Double> getCurrentParameters() {
    throw new UnsupportedOperationException();
  }

  /** A method for mapping parameters to
    * the entire real line.  Useful when using a gradient optimiser.
    * The inverse is given by <code>decodeParameters</code>.
    * @see #decodeParameters(ArrayList) */
  public ArrayList<Double> encodeParameters(ArrayList<Double> pars) {
    throw new UnsupportedOperationException();
  }

  /** A method for decoding back the parameters from reals.
    * This is the inverse of <code>encodeParameters</code>.
    * @see #encodeParameters(ArrayList) */
  public ArrayList<Double> decodeParameters(ArrayList<Double> pars) {
    throw new UnsupportedOperationException();
  }

  /** Adjusts pure parameter gradients (Δabs) to be encoded parameter
    * gradients (Δxys).
    * This operation is currently implemented destructively.
    * @param grad pure gradients
    * @param pars pure parameters
    * @return encoded gradients
    * @see #encodeParameters(ArrayList) */
  public ArrayList<Double> encodeGradients(ArrayList<Double> grad,
                                           ArrayList<Double> pars) {
    throw new UnsupportedOperationException();
  }

  /** Creates a new urn instance with given parameters.
    * @param pars pure parameters (unencoded) */
  public PPMUrn<X,Y> createUrn(ArrayList<Double> pars) {
    throw new UnsupportedOperationException();
  }

  /** Returns the parameters as human-readable String. */
  public String stringFromParameters(ArrayList<Double> pars) {
    return ""+pars;
  }


  /** Returns state information, for debugging. */
  public String getStateInfo() {
    return getContext().toString();
  }

  /** Returns a graph description in dot format. */
  public String toDot(PPMTrie<X,Y> node) {
    throw new UnsupportedOperationException();
  }

  /*
  public X sample(X x, PPMTrie<X,Y> ctx, Mass<X> base, Random rnd);
  */

}
