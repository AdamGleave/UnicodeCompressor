/* Automated copy from build process */
/* $Id: UrnDPY.java,v 1.13 2014/09/18 16:33:45 chris Exp $ */

import java.util.Iterator;
import java.util.Collection;
import java.util.Hashtable;
import java.util.ArrayList;

/** A Pitman-Yor restaurant with full blending,
  * depth-dependent discount parameters, and exclusive
  * updates. */
public class UrnDPY<X> extends UrnPY<X> {

  /** Depth-dependent discount parameters. */
  double[] discounts;
  // { 0.25, 0.7, 0.8, 0.82, 0.84, 0.88, 0.91, 0.92, 0.93, 0.94, 0.95 };
  // { 0.62, 0.69, 0.74, 0.80, 0.95 };
  // { 0.5, 0.7, 0.8, 0.82, 0.84, 0.88, 0.91, 0.92, 0.93, 0.94, 0.95 };
  
  public UrnDPY() {
    double[] good = { 0.25, 0.7,  0.8,  0.82, 0.84, 0.88,
                      0.91, 0.92, 0.93, 0.94, 0.95 };
    double[] half = { 0.5 };
    this.alpha = 0.0;
    this.discounts = half;
    this.maxdepth = discounts.length-1;
    init();
  }

  public UrnDPY(int depth) {
    this();
    this.maxdepth = depth;
  }

  public UrnDPY(double[] discounts) {
    super();
    this.discounts = discounts;
    this.maxdepth = discounts.length-1;
  }
  
  public UrnDPY(ArrayList<Double> discounts) {
    super();
    this.discounts = new double[discounts.size()];
    this.maxdepth = discounts.size()-1;
    for (int k=0; k<discounts.size(); k++) {
      this.discounts[k] = discounts.get(k);
    }
  }

  public UrnDPY(double alpha, double[] discounts) {
    this(discounts);
    this.alpha = alpha;
  }
  
  public UrnDPY(double alpha, ArrayList<Double> discounts) {
    this(discounts);
    this.alpha = alpha;
  }


  public String toString() {
    StringBuilder sb = new StringBuilder();
    if (discounts.length > 1) {
      sb.append(discounts[0]);
      for (int d=1; d<discounts.length; d++) {
        sb.append(","+discounts[d]);
      }
    }
    //return "UrnDPY(alpha="+alpha+",disc=["+sb.toString()+"])";
    return "UrnDPY(d="+maxdepth+":a="+alpha+":b="+sb.toString()+")";
  }

  /** Returns the discount parameter for a given context depth. */
  public double getDiscount(int depth) {
    if (depth < discounts.length) {
      return discounts[depth];
    } else {
      return discounts[discounts.length-1];
    }
  }

  @Override
  public double getDiscount(PPMTrie<X,UrnDPY.Cargo<X>> ctx) {
    return getDiscount(getDepth(ctx));
  }
  

  public double mass(X x, PPMTrie<X,UrnDPY.Cargo<X>> ctx,
                                             Mass<X> base, int depth) {
    if (ctx == null) {
      return base.mass(x);
    }
    double pmass = mass(x,ctx.vine,base,depth-1);
    Integer nx = ctx.data.counts.get(x);
    if (nx == null) { nx = 0; }
    if (ctx.data.n == 0) {
      // if the total count is zero, use the parent distribution
      return pmass;
    } else {
      double val = Double.NEGATIVE_INFINITY;
      double norm = (ctx.data.n + alpha);
      double discount = getDiscount(depth);
      if (nx == 0) {
        val = pmass*(alpha+ctx.data.u*discount) / norm;
      } else {
        val = (((double) nx - discount) + pmass*(alpha+ctx.data.u*discount))
              / norm;
      }
      return val;
    }
  }
  
  @Override
  public double mass(X x, PPMTrie<X,UrnDPY.Cargo<X>> ctx, Mass<X> base) {
    return mass(x,ctx,base,getDepth(ctx));
  }

  /** Computes the gradient of a given symbol's mass
    * with respect to the discount parameter of selected depth.
    * @param d current node depth (fetch via getDepth)
    * @param depth selected depth */
  public double gradient(X x, PPMTrie<X,UrnDPY.Cargo<X>> ctx,
                              Mass<X> base,
                              int d, int depth) {
    if (ctx == null) {
      return 0.0;
    }
    int maxdepth = discounts.length-1;
    // TODO: the line below is an effective, but inelegant fix
    if (depth != maxdepth) { maxdepth++; }
    if (ctx.data.n == 0) {
      return gradient(x,ctx.vine,base,d-1,depth);
    } else {
      double grad = 0.0;
      Integer nx = ctx.data.counts.get(x);
      if (nx == null) { nx = 0; }
      if (nx > 0 && (d==depth || d > maxdepth)) {
        // current restaurant contains selected discount
        grad += -1.0;
      }
      if (d==depth || d > maxdepth) {
        // current restaurant contains selected discount
        double pmass = mass(x,ctx.vine,base,d-1);
        grad += ctx.data.u * pmass;
      }
      if (d > depth) {
        // parent restaurant contains selected discount
        double pgrad = gradient(x,ctx.vine,base,d-1,depth);
        grad += (alpha + ctx.data.u*getDiscount(d)) * pgrad;
      }
      grad /= (ctx.data.n+alpha);
      return grad;
    }
  }

  /** Computes the gradient of a given symbol's log mass
    * with respect to the discount parameter of selected depth.
    * @param d current node depth (fetch via getDepth)
    * @param depth selected depth */
  public double logGradient(X x, PPMTrie<X,UrnDPY.Cargo<X>> ctx,
                              Mass<X> base,
                              int d, int depth) {
    if (ctx != null) {
      double mass = mass(x,ctx,base,d);
      double grad = gradient(x,ctx,base,d,depth);
      //System.err.println("grad["+d+","+depth+"] = "+grad);
      //System.err.println("mass["+d+","+depth+"] = "+mass);
      return (grad/mass);
    } else {
      return 0.0;
    }
  }

  /** Computes ∂/∂β logP(x | ...) for all discount parameters β, and
    * sums them onto the specified array list. */
  public void addLogGradients(X x, PPMTrie<X,UrnDPY.Cargo<X>> ctx,
                             Mass<X> base, ArrayList<Double> grad) {
    int ctxdepth = getDepth(ctx);
    for (int d=0; d<discounts.length; d++) {
      Double g = grad.get(d);
      if (g == null) { g = 0.0; }
      grad.set(d,g+logGradient(x, ctx, base, ctxdepth, d));
    }
  }
  
  /** Computes ∂/∂β logP(x | ...) for all discount parameters β, and
    * adds them to the specified array list. */
  public ArrayList<Double> createGradientList() {
    ArrayList<Double> grad = new ArrayList<Double>();
    for (int d=0; d<discounts.length; d++) {
      grad.add(0.0);
    }
    return grad;
  }

  /** Adds, for each symbol, scaled predictive scores to a hash table.
    * @param ctx context node
    * @param base base distribution
    * @param set iterable set of symbols (used for base distribution)
    * @param mass hash table to which scores will be added
    * @param budget total integer budget to be divided up in proportion
    *               to each element's predictive probability mass
    * @param depth context depth */
  public void addMass(PPMTrie<X,UrnDPY.Cargo<X>> ctx, Mass<X> base,
                      Iterable<X> set, Hashtable<X,Long> mass,
                      long budget, int depth) {
    if (ctx != null) {
      if (ctx.data.n > 0) {
        double norm = ((double) ctx.data.n + alpha);
        long sum = 0L; // actual sum spent (may differ from budget)
        double discount = getDiscount(depth);
        for (X x : ctx.data.counts.keySet()) {
          int nx = ctx.data.counts.get(x);
          double f = (nx - discount) / norm;
          long add = (long) (f * (double) budget);
          mass.put(x, mass.get(x) + add);
          sum += add;
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
                              PPMTrie<X,UrnDPY.Cargo<X>> ctx,
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



  public Distribution<X> getPredictive(PPMTrie<X,UrnDPY.Cargo<X>> ctx,
                                       Mass<X> base, Iterable<X> set) {
    Hashtable<X,Double> table = new Hashtable<X,Double>();
    for (X x : set) {
      table.put(x, mass(x,ctx,base));
    }
    DiscreteLookup<X> dl = new DiscreteLookup<X>(table);
    return dl;
  }

}

