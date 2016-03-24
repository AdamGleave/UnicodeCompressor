/* Automated copy from build process */
/* $Id: UrnPY.java,v 1.16 2013/04/15 15:54:45 chris Exp $ */

import java.util.Iterator;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;

/** A Pitman-Yor restaurant with full blending, and 
  * exclusive updates.
  * Counts are kept in a hashtable.
  * Floating point arithmetic is used, and discretized just
  * before encoding / decoding.
  * A finite symbol domain is assumed.*/
public class UrnPY<X> extends PPMUrn<X,UrnPY.Cargo<X>> {

  public static class Cargo<X> {
    /** Total number of symbol obversations. */
    int n = 0;
    /** Total number of unique observations. */
    int u = 0;
    /** Symbol counts. */
    Hashtable<X,Integer> counts = null;
    /** Constructor. */
    public Cargo() {
      this.counts = new Hashtable<X,Integer>();
    }
    /** Returns a cloned copy of this cargo object. */
    public Cargo clone() {
      Cargo<X> copy = new Cargo<X>();
      copy.n = n;
      copy.u = u;
      copy.counts = new Hashtable<X,Integer>(counts);
      /*
      boolean equal = true;
      for (Map.Entry<X,Integer> e : counts.entrySet()) {
        X key = e.getKey();
        equal &= e.getValue().equals(copy.counts.get(key));
      }
      if (!equal) {
        throw new RuntimeException("No equality");
      }
      */
      return copy;
    }
    /** Indicates if this instance is equal to the supplied object. */
    @Override
    @SuppressWarnings("unchecked")
    public boolean equals(Object other) {
      Cargo<X> obj = (Cargo<X>) other;
      boolean equal = true;
      equal &= n == obj.n;
      equal &= u == obj.u;
      for (Map.Entry<X,Integer> e : counts.entrySet()) {
        X key = e.getKey();
        equal &= e.getValue().equals(obj.counts.get(key));
      }
      return equal;
    }
  }

  /** Concentration parameter. */
  double alpha = 0.0;

  /** Discount parameter. */
  double beta = 0.5;

  /** Tree depth. */
  int maxdepth = 5;
  
  public String toString() {
    return "t=b:d="+maxdepth+":a="+alpha+":b="+beta;
  }

  /** Current context. */
  PPMTrie<X,UrnPY.Cargo<X>> context;

  /** Current depth. */
  int d = 0;

  /** Constructor. */
  public UrnPY(double alpha, double beta, int depth) {
    this.alpha = alpha;
    this.beta = beta;
    this.maxdepth = depth;
    init();
  }
  
  /** Default constructor. */
  public UrnPY() {
    this.alpha = 0.0;
    this.beta = 0.5;
    init();
  }
  
  /** Default constructor. */
  public UrnPY(int depth) {
    this();
    this.maxdepth = depth;
  }

  /** Initialises this urn. */
  public void init() {
    // create a root node and make it the current context
    this.context = new PPMTrie<X,UrnPY.Cargo<X>>();
    init(context);
    // set depth to zero
    this.d = 0;
  }

  /** Initialises a given node.
    * @deprecated */
  public void init(PPMTrie<X,UrnPY.Cargo<X>> ctx) {
    if (ctx.data == null) {
      ctx.data = new Cargo<X>();
    }
  }

  /** Initialise this node and all its parents. */
  private void initUp(PPMTrie<X,UrnPY.Cargo<X>> node) {
    do {
      init(node);
      node = node.vine;
    } while (node != null);
  }

  public PPMTrie<X,UrnPY.Cargo<X>> getContext() {
    return context;
  }
  
  public void setContext(PPMTrie<X,UrnPY.Cargo<X>> ctx) {
    this.context = ctx;
    this.d = getDepth(ctx);
  }

  /*
  public PPMTrie<X,UrnPY.Cargo<X>> learn(X x, PPMTrie<X,UrnPY.Cargo<X>> ctx) {
    Integer k = ctx.data.counts.get(x);
    if (k != null) {
      ctx.data.counts.put(x,k+1);
      // disable the next line to have "update exclusion":
      //if (ctx.vine != null) { learn(x,ctx.vine); }
    } else {
      ctx.data.counts.put(x,1);
      ctx.data.u++;
      if (ctx.vine != null) {
        learn(x,ctx.vine);
      }
    }
    ctx.data.n++;
    PPMTrie<X,UrnPY.Cargo<X>> child = ctx.findOrAdd(x);
    init(child);
    return child;
  }
  */
  
  public PPMTrie<X,UrnPY.Cargo<X>> learn(X x, PPMTrie<X,UrnPY.Cargo<X>> ctx, int depth) {
    Integer k = ctx.data.counts.get(x);
    if (k != null) {
      ctx.data.counts.put(x,k+1);
      // disable the next line to have "update exclusion":
      //if (ctx.vine != null) { learn(x,ctx.vine,depth-1); }
    } else {
      ctx.data.counts.put(x,1);
      ctx.data.u++;
      if (ctx.vine != null) {
        learn(x, ctx.vine, depth-1);
      }
    }
    ctx.data.n++;
    if (depth < maxdepth) {
      ctx = ctx.findOrAdd(x);
      init(ctx);
    } else
    if (ctx.vine != null) {
      ctx = ctx.vine.findOrAdd(x);
      init(ctx);
    }
    return ctx;
  }
  
  public void learn(X x) {
    context = learn(x,context,d);
    if (d < maxdepth) { d++; }
  }



  public double getDiscount(PPMTrie<X,UrnPY.Cargo<X>> ctx) {
    return beta;
  }


  public double mass(X x, PPMTrie<X,UrnPY.Cargo<X>> ctx, Mass<X> base) {
    double pmass = (ctx.vine != null ? mass(x,ctx.vine,base)
                                     : base.mass(x));
    Integer nx = ctx.data.counts.get(x);
    if (nx == null) { nx = 0; }
    if (ctx.data.n == 0) {
      // if the total count is zero, use the parent distribution
      return pmass;
    } else {
      double val = Double.NEGATIVE_INFINITY;
      double norm = (ctx.data.n + alpha);
      double discount = getDiscount(ctx);
      if (nx == 0) {
        val = pmass*(alpha+ctx.data.u*discount) / norm;
      } else {
        val = (((double) nx - discount) + pmass*(alpha+ctx.data.u*discount))
              / norm;
      }
      return val;
    }
  }
  
  public double mass(X x, Mass<X> base) {
    return mass(x, context, base);
  }

  /** Adds, for each symbol, scaled predictive scores to a hash table.
    * @param ctx context node
    * @param base base distribution
    * @param set iterable set of symbols (used for base distribution)
    * @param mass hash table to which scores will be added
    * @param budget total integer budget to be divided up in proportion
    *               to each element's predictive probability mass */
  public void addMass(PPMTrie<X,UrnPY.Cargo<X>> ctx, Mass<X> base,
                      Iterable<X> set, Hashtable<X,Long> mass, long budget) {
    if (ctx != null) {
      if (ctx.data.n > 0) {
        double norm = ((double) ctx.data.n + alpha);
        long sum = 0L; // actual sum spent (may differ from budget)
        for (X x : ctx.data.counts.keySet()) {
          int nx = ctx.data.counts.get(x);
          double f = (nx - beta) / norm;
          long add = (long) (f * (double) budget);
          mass.put(x, mass.get(x) + add);
          sum += add;
        }
        budget -= sum;
      }
      // now allocate remaining budget from parent contexts
      addMass(ctx.vine, base, set, mass, budget);
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
          addMass(null, base, set, mass, rem);
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
                              PPMTrie<X,UrnPY.Cargo<X>> ctx,
                              Mass<X> base, Iterable<X> set,
                              long budget) {
    Hashtable<X,Long> table = new Hashtable<X,Long>();
    long u = 0;
    for (X x : set) {
      table.put(x,1L);
      u++;
    }
    /* add mass */
    addMass(ctx, base, set, table, budget-u);
    return table;
  }


  public void encode(X sym, PPMTrie<X,UrnPY.Cargo<X>> ctx, Mass<X> base,
                            Iterable<X> set, Encoder ec) {
    long budget = ec.getRange();
    Hashtable<X,Long> table = getDiscretePredictive(ctx,base,set,budget);
    /** Compute cumulative, on demand. */
    long sum = 0L;
    for (X x : set) {
      long m = table.get(x);
      if (x.equals(sym)) {
        ec.storeRegion(sum,sum+m,budget);
        return;
      }
      sum+=m;
    }
    throw new ZeroMassException("unknown symbol: '"+sym+"'");
    /*
    if (sum != budget) {
      System.err.println("sum="+sum+" != budget="+budget+" x="+sym);
    }
    */
  }
  
  public void encode(X sym, Mass<X> base, Iterable<X> set, Encoder ec) {
    encode(sym, context, base, set, ec);
  }


  public X decode(PPMTrie<X,UrnPY.Cargo<X>> ctx, Mass<X> base,
                                     Iterable<X> set, Decoder dc) {
    long budget = dc.getRange();
    Hashtable<X,Long> table = getDiscretePredictive(ctx,base,set,budget);
    long r = dc.getTarget(budget);
    // compute cumulative distribution
    long sum = 0L;
    long lo = 0;
    long hi = 0;
    for (X x : set) {
      lo = hi;
      hi += table.get(x);
      if (r >= lo && r < hi) {
        dc.loadRegion(lo,hi,budget);
        return x;
      }
    }
    throw new RuntimeException("decoding failure");
  }
  
  public X decode(Mass<X> base, Iterable<X> set, Decoder dc) {
    return decode(context, base, set, dc);
  }


  public Distribution<X> getPredictive(PPMTrie<X,UrnPY.Cargo<X>> ctx,
                                       Mass<X> base, Iterable<X> set) {
    /*Mass<X> parent = base;
    if (ctx.vine != null) {
      parent = getPredictive(ctx.vine, base, set);
    }*/
    Hashtable<X,Double> table = new Hashtable<X,Double>();
    for (X x : set) {
      table.put(x, mass(x,ctx,base));
    }
    DiscreteLookup<X> dl = new DiscreteLookup<X>(table);
    return dl;
  }
  
  public Distribution<X> getPredictive(Mass<X> base, Iterable<X> set) {
    return getPredictive(getContext(), base, set);
  }

  public String getStateInfo(PPMTrie<X,UrnPY.Cargo<X>> ctx) {
    String cc = ctx.data.counts.toString();
    String vv = (ctx.vine != null) ? " "+getStateInfo(ctx.vine) : " base";
    return "[n="+ctx.data.n+", u="+ctx.data.u+", cc="+cc+"]"+vv;
  }


  /** Creates a dot-description of the PPMTrie. */
  protected void toDot(StringBuilder sb, PPMTrie<X,UrnPY.Cargo<X>> ctx) {
    sb.append("  n"+ctx.hashCode()+" [label=\""
                   //+ctx.data.counts
                   +"n:"+ctx.data.n
                   +",u:"+ctx.data.u
                   +"\"];\n");
    if (ctx.children != null) {
      for (X x : ctx.children.keySet()) {
        PPMTrie<X,UrnPY.Cargo<X>> n = ctx.children.get(x);
        this.toDot(sb,n);
        sb.append("  n"+ctx.hashCode()+" -> n"+n.hashCode()
                 +" [label=\""+x+"\"];\n");
      }
    }
    if (ctx.vine != null) {
      sb.append("  n"+ctx.hashCode()+" -> n"+ctx.vine.hashCode()+" [style=dotted,constraint=false];\n");
    }
  }

  public String toDot(PPMTrie<X,UrnPY.Cargo<X>> ctx) {
    StringBuilder sb = new StringBuilder();
    sb.append("digraph PPMtree {\n");
    this.toDot(sb,ctx);
    sb.append("}");
    return sb.toString();
  }


}

