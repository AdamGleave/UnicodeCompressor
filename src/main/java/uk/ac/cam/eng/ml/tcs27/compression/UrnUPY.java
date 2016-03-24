/* Automated copy from build process */
/* $Id: UrnUPY.java,v 1.4 2014/09/04 13:48:48 chris Exp $ */
package uk.ac.cam.eng.ml.tcs27.compression;

import java.util.Iterator;
import java.util.Collection;
import java.util.Hashtable;
import java.util.ArrayList;

/** A Pitman-Yor restaurant with full blending,
  * fanout-dependent discount and concentration parameters,
  * and 1TPD updates.
  * The fanout of a node is the number of unique symbols
  * observed in its context. */
public class UrnUPY<X> extends UrnPY<X> {

  /** Fanout-dependent discount parameters.
    * The first parameter betas[0] is for nodes with a fanout of 1. */
  double[] betas;
  /** Fanout-dependent concentration parameters. */
  double[] alphas;
  /** Number of fanout-dependent parameters. */
  int maxfan = 0;

  // { 0.25, 0.7, 0.8, 0.82, 0.84, 0.88, 0.91, 0.92, 0.93, 0.94, 0.95 };
  // { 0.62, 0.69, 0.74, 0.80, 0.95 };
  // { 0.5, 0.7, 0.8, 0.82, 0.84, 0.88, 0.91, 0.92, 0.93, 0.94, 0.95 };
  
  public UrnUPY() {
    double[] half = { 0.5 };
    double[] zero = { 0.0 };
    this.alphas = zero;
    this.betas  = half;
    this.maxfan = 1;
    init();
  }

  public UrnUPY(int depth) {
    this();
    this.maxdepth = depth;
  }

  /** Constructs a new UPY urn scheme with given
    * concentrations and discounts.
    * @param alphas concentrations
    * @param betas discounts */
  public UrnUPY(double[] alphas, double[] betas) {
    super();
    this.betas = betas;
    this.alphas = alphas;
    this.maxfan = betas.length;
  }
  
  /** Constructs a new UPY urn scheme with given
    * concentrations and discounts.
    * @param alphas concentrations
    * @param betas discounts
    * @param depth maximal context depth */
  public UrnUPY(double[] alphas, double[] betas, int depth) {
    super();
    this.betas = betas;
    this.alphas = alphas;
    this.maxfan = betas.length;
    this.maxdepth = depth;
  }
  
  public UrnUPY(int maxdepth,
                 ArrayList<Double> concentrations,
                 ArrayList<Double> discounts) {
    super();
    int cs = concentrations != null ? concentrations.size() : 0;
    int ds = discounts != null ? discounts.size() : 0;
    int ms = cs < ds ? ds : cs;
    double lastd = 0.5;
    double lastc = 0.0;
    this.alphas = new double[ms];
    for (int k=0; k<ms; k++) {
      if (k < cs) { lastc = concentrations.get(k); }
      this.alphas[k] = lastc;
    }
    this.betas = new double[ms];
    for (int k=0; k<ds; k++) {
      if (k < ds) { lastd = discounts.get(k); }
      this.betas[k] = lastd;
    }
    this.maxfan   = ms;
    this.maxdepth = maxdepth;
  }
  
  public UrnUPY(ArrayList<Double> concentrations,
                 ArrayList<Double> discounts) {
    // FIXME: this constructor is probably not sensible to have
    this(discounts.size()-1, concentrations, discounts);
  }
  
  public UrnUPY(ArrayList<Double> discounts) {
    this.betas = new double[discounts.size()];
    for (int k=0; k<discounts.size(); k++) {
      this.betas[k] = discounts.get(k);
    }
    this.alphas = new double[] { 0.0 };
    this.maxfan   = discounts.size();
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    if (alphas.length > 0) {
      sb.append("a=");
      sb.append(alphas[0]);
      for (int d=1; d<alphas.length; d++) {
        sb.append(","+alphas[d]);
      }
      sb.append(":");
    }
    if (betas.length > 0) {
      sb.append("b=");
      sb.append(betas[0]);
      for (int d=1; d<betas.length; d++) {
        sb.append(","+betas[d]);
      }
      sb.append("");
    }
    return "t=u:"+sb.toString();
  }

  /** Returns the discount parameter for a given node fanout. */
  public double getDiscount(int u) {
    if (u < betas.length) {
      return betas[u];
    } else {
      return betas[betas.length-1];
    }
  }
  
  /** Returns the concentration parameter for a given node fanout. */
  public double getConcentration(int u) {
    if (u < alphas.length) {
      return alphas[u];
    } else {
      return alphas[alphas.length-1];
    }
  }


  public double mass(X x, PPMTrie<X,UrnUPY.Cargo<X>> ctx, Mass<X> base) {
    if (ctx == null) {
      return base.mass(x);
    }
    double pmass = mass(x,ctx.vine,base);
    Integer nx = ctx.data.counts.get(x);
    if (nx == null) { nx = 0; }
    if (ctx.data.n == 0) {
      // if the total count is zero, use the parent distribution
      return pmass;
    } else {
      double alpha = getConcentration(ctx.data.u-1);
      double beta  = getDiscount(ctx.data.u-1);
      double norm = (ctx.data.n + alpha);
      double val = Double.NEGATIVE_INFINITY;
      if (nx == 0) {
        val = pmass*(alpha+ctx.data.u*beta) / norm;
      } else {
        val = (((double) nx - beta) + pmass*(alpha+ctx.data.u*beta))
              / norm;
      }
      return val;
    }
  }

  public double mass(X x, PPMTrie<X,UrnUPY.Cargo<X>> ctx,
                     Mass<X> base, int depth) {
    return mass(x,ctx,base);
  }
  
  @Override
  public double mass(X x, Mass<X> base) {
    return mass(x, context, base, getDepth(context));
  }

  /** Computes the gradient of a given symbol's mass
    * with respect to the <b>discount</b> parameter of selected fanout.
    * @param uu selected fanout */
  public double betaGradient(X x, PPMTrie<X,UrnDPY.Cargo<X>> ctx,
                              Mass<X> base,
                              int uu) {
    if (ctx == null) {
      return 0.0;
    }
    int u = ctx.data.u;
    /* // A computational shortcut:
    if ((uu+1) < u) {
      return 0.0;
    } else
    */
    if (ctx.data.n == 0) {
      return betaGradient(x,ctx.vine,base,uu);
    } else {
      double grad = 0.0;
      Integer nx = ctx.data.counts.get(x);
      if (nx == null) { nx = 0; }
      if (u == (uu+1) || (u > maxfan && (uu+1) == maxfan)) {
        // contribution from count
        if (nx > 0) {
          grad += -1.0;
        }
        // contribution from parent
        double pmass = mass(x,ctx.vine,base);
        grad += u * pmass;
      }
      if ((uu+1) >= u || (u > maxfan && (uu+1) == maxfan)) {
        // parent restaurant contains selected discount
        double pgrad = betaGradient(x,ctx.vine,base,uu);
        grad += (getConcentration(u-1) + u*getDiscount(u-1)) * pgrad;
      }
      //  (u == (uu+1) || (uu > maxfan && u > (uu+1)))) {
        // FIXME: think about uu < max_u, or uu > max_u...
        // current restaurant contains selected discount
      //if (u == (uu+1) || (uu > maxfan && u > (uu+1))) {
      //  // current restaurant contains selected discount
      //  double pmass = mass(x,ctx.vine,base);
      //  grad += u * pmass;
      //}
      grad /= (ctx.data.n + getConcentration(u-1));
      return grad;
    }
  }




  /** Computes the gradients of a given symbol's log mass
    * with respect to the parameters of selected depth. */
  public ArrayList<Double> logGradients(X x, PPMTrie<X,UrnUPY.Cargo<X>> ctx,
                                        Mass<X> base) {
    ArrayList<Double> g = createGradientList();
    for (int u=0; u<maxfan; u++) {
      g.set(u, betaGradient(x,ctx,base,u));
    }
    double mass = mass(x,ctx,base);
    // convert gradients of P to gradients of logP:
    for (int k=0; k<g.size(); k++) {
      g.set(k, g.get(k) / mass);
    }
    //System.err.println("grad = "+g);
    //System.err.println("mass["+d+","+depth+"] = "+mass);
    return g;
  }

  /** Computes ∂/∂β logP(x | ...) for all discount parameters β, and
    * sums them onto the specified array list. */
  public void addLogMassGradients(X x, PPMTrie<X,UrnUPY.Cargo<X>> ctx,
                              Mass<X> base, ArrayList<Double> grad) {
    //int ctxdepth = getDepth(ctx);
    ArrayList<Double> g = logGradients(x,ctx,base);
    for (int k=0; k<grad.size(); k++) {
      grad.set(k, grad.get(k) + g.get(k));
    }
  }
  
  /** Computes ∂/∂β logP(x | ...) for all discount parameters β, and
    * adds them to the specified array list. */
  public ArrayList<Double> createGradientList() {
    ArrayList<Double> grad = new ArrayList<Double>();
    for (int u=0; u<maxfan; u++) {
      grad.add(0.0); // betas only, for the moment
    }
    return grad;
  }

  public ArrayList<Double> getCurrentParameters() {
    ArrayList<Double> pars = new ArrayList<Double>();
    // For the moment, betas only...
    for (int k=0; k<betas.length; k++) {
      pars.add(betas[k]);
    }
    return pars;
  }

  public ArrayList<Double> encodeParameters(ArrayList<Double> pars) {
    int d = pars.size();
    ArrayList<Double> xs = new ArrayList<Double>();
    // map all parameters to the real line
    for (int k=0; k<d; k++) {
      // transform beta to x (the real line)
      double beta = pars.get(k);
      double x = Double.NaN;
      if (beta == 0.0) {
        x = -10000.0;
      } else
      if (beta == 1.0) {
        x = +10000.0;
      } else {
        x = Math.log(beta) - Math.log(1.0-beta);
      }
      xs.add(x);
    }
    return xs;
  }

  public ArrayList<Double> decodeParameters(ArrayList<Double> pars) {
    ArrayList<Double> betas = new ArrayList<Double>();
    int d = pars.size();
    for (int k=0; k<d; k++) {
      double logit = pars.get(k);
      double eep = Math.exp(logit);
      double beta = eep / (eep+1.0);
      betas.add(beta);
    }
    return betas;
  }

  public ArrayList<Double> encodeGradients(ArrayList<Double> grad,
                                           ArrayList<Double> pars) {
    int d = grad.size();
    for (int k=0; k<d; k++) {
      double beta = pars.get(k); // get the kth discount
      double beta_corr = beta * (1.0 - beta);
      grad.set(k, -grad.get(k) * beta_corr);
    }
    return grad;
  }

  public UrnUPY<X> createUrn(ArrayList<Double> pars) {
    int d = pars.size();
    double[] betas = new double[d];
    for (int k=0; k<d; k++) {
      betas[k]  = pars.get(k);
    }
    return new UrnUPY<X>(alphas,betas,maxdepth);
  }

  public String stringFromParameters(ArrayList<Double> pars) {
    return "β="+pars;
  }


  /** Adds, for each symbol, scaled predictive scores to a hash table.
    * @param ctx context node
    * @param base base distribution
    * @param set iterable set of symbols (used for base distribution)
    * @param mass hash table to which scores will be added
    * @param budget total integer budget to be divided up in proportion
    *               to each element's predictive probability mass
    * @param depth context depth */
  public void addMass(PPMTrie<X,UrnUPY.Cargo<X>> ctx, Mass<X> base,
                      Iterable<X> set, Hashtable<X,Long> mass,
                      long budget, int depth) {
    if (ctx != null) {
      if (ctx.data.n > 0) {
        long sum = 0L; // actual sum spent (may differ from budget)
        double discount = getDiscount(ctx.data.u-1);
        double conc     = getConcentration(ctx.data.u-1);
        double norm     = ((double) ctx.data.n + conc);
        for (X x : ctx.data.counts.keySet()) {
          int nx = ctx.data.counts.get(x);
          if (nx > 0) {
            double f = (nx - discount) / norm;
            long add = (long) (f * (double) budget);
            mass.put(x, mass.get(x) + add);
            sum += add;
          } else {
            // no contribution
          }
        }
        budget -= sum;
      }
      // now allocate remaining budget from parent contexts
      addMass(ctx.vine, base, set, mass, budget, depth-1);
    } else {
      // add contributions of base distribution
      /** Remaining budget */
      long rem = budget;
      /** Remaining mass */
      double rest = 1.0;
      /** Number of unique elements in the base distribution. */
      long uni = 0L;
      for (X x : set) {
        double m = base.mass(x);
        double f = ((double) rem * (m/rest));
        long add = (long) f;
        mass.put(x, mass.get(x) + add);
        rem -= add;
        rest -= m;
        uni++;
      }
      /* for some really large budgets, this may leave an unspent
       * amount of up to 7000 or so -- this happens because a double
       * cannot represent large numbers accurately enough.
       * We could divide up the 7000 once more, or we just spend it
       * all on any random element. */
      if (rem > 0) {
        if (rem > uni<<2) {
          // try to distribute it fairly
          addMass(null, base, set, mass, rem, depth-1);
        } else {
          // just spend it all on the first element
          Iterator<X> it = set.iterator();
          X x = it.next();
          mass.put(x, mass.get(x) + rem);
        }
      } else {
        throw new RuntimeException("*overspent");
      }
    }
  }

  /** Computes the predictive distribution as a Hashtable,
    * scaled to a given long integer.
    * The counts in the hash table will sum to the given
    * integer.
    * @param ctx context node
    * @param base base distribution
    * @param set iterable symbol set
    * @param budget total (long) integer budget */
  public Hashtable<X,Long> getDiscretePredictive(
                              PPMTrie<X,UrnUPY.Cargo<X>> ctx,
                              Mass<X> base, Iterable<X> set,
                              long budget) {
    Hashtable<X,Long> table = new Hashtable<X,Long>();
    long u = 0;
    for (X x : set) {
      table.put(x,1L);
      u++;
    }
    int depth = getDepth(ctx);
    /* add mass */
    addMass(ctx, base, set, table, budget-u, depth);
    return table;
  }



  public Distribution<X> getPredictive(PPMTrie<X,UrnUPY.Cargo<X>> ctx,
                                       Mass<X> base, Iterable<X> set) {
    Hashtable<X,Double> table = new Hashtable<X,Double>();
    for (X x : set) {
      table.put(x, mass(x,ctx,base));
    }
    DiscreteLookup<X> dl = new DiscreteLookup<X>(table);
    return dl;
  }

}

