/* Automated copy from build process */
/* $Id: UrnXPY.java,v 1.9 2013/04/15 15:54:45 chris Exp $ */

import java.util.Iterator;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Set;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.Map;

/** A hierarchical exclusion urn scheme with Pitman-Yor style parameters.
  * Counts are kept in a hashtable.
  * Floating point arithmetic is used, and discretized just
  * before encoding / decoding.
  * A finite symbol domain is assumed.*/
public class UrnXPY<X> extends PPMUrn<X,UrnXPY.Cargo<X>> {

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
      return copy;
    }
  }

  /** Concentration parameter. */
  double alpha = 0.0;

  /** Discount parameter. */
  double beta = 0.5;

  /** Maximum context depth. */
  int maxdepth = 5;

  /** Current context depth. */
  int d = 0;

  public String toString() {
    return "t=x:d="+maxdepth+":a="+alpha+":b="+beta;
  }

  /** Current context. */
  PPMTrie<X,UrnXPY.Cargo<X>> context = null;


  /** Constructor. */
  public UrnXPY(double alpha, double beta, int depth) {
    this.alpha = alpha;
    this.beta = beta;
    this.maxdepth = depth;
    init();
  }
  
  /** Default constructor. */
  public UrnXPY() {
    this.alpha = 0.0;
    this.beta = 0.5;
    init();
  }
  
  /** Constructs a new exclusive PY urn with given maxdepth. */
  public UrnXPY(int depth) {
    this();
    this.maxdepth = depth;
  }

  /** Initialises the given node. */
  public void init(PPMTrie<X,UrnXPY.Cargo<X>> ctx) {
    if (ctx.data == null) {
      ctx.data = new Cargo<X>();
    }
  }
  
  /** Initialises this urn. */
  public void init() {
    this.context = new PPMTrie<X,UrnXPY.Cargo<X>>();
    init(this.context);
  }
 
  /** Initialise this node and all its parents. */
  private void initUp(PPMTrie<X,UrnXPY.Cargo<X>> node) {
    do {
      init(node);
      node = node.vine;
    } while (node != null);
  }

  public PPMTrie<X,UrnXPY.Cargo<X>> getContext() {
    return context;
  }
  
  public void setContext(PPMTrie<X,UrnXPY.Cargo<X>> ctx) {
    this.context = ctx;
    this.d = getDepth(ctx);
  }

  /** Updates the trie to incorporate a new observation.
    * @return the new context node in the trie */
  public PPMTrie<X,UrnXPY.Cargo<X>> learn(X x, PPMTrie<X,UrnXPY.Cargo<X>> ctx, int depth) {
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

  public double getDiscount(PPMTrie<X,UrnXPY.Cargo<X>> ctx) {
    return beta;
  }


  public double mass(X x, PPMTrie<X,UrnXPY.Cargo<X>> ctx,
                                        Set<X> excl, Mass<X> base) {
    if (ctx == null) {
      // use base distribution
      if (excl.isEmpty()) {
        // no exclusions...
        return base.mass(x);
      } else {
        // exclusions...
        if (excl.contains(x)) {
          return 0.0;
        } else {
          // compute base.massWithout(x,excl):
          double exm = 0.0;
          for (X z : excl) {
            exm += base.mass(z);
          }
          return base.mass(x)/(1.0-exm);
        }
      }
    } else {
      Integer nx = 0;
      int sum = 0;
      int uni = 0;
      for (Map.Entry<X,Integer> z : ctx.data.counts.entrySet()) {
        if (!excl.contains(z.getKey())) {
          sum += z.getValue();
          uni ++; // assuming there are only non-zero entries
          if (z.getKey().equals(x)) {
            nx = z.getValue();
          }
        }
      }
      double pmass = mass(x,ctx.vine,ctx.data.counts.keySet(),base);
      if (sum == 0) {
        // if the total count is zero, use the parent distribution
        return pmass;
      } else {
        double val = Double.NEGATIVE_INFINITY;
        double discount = getDiscount(ctx);
        if (nx == 0) {
          val = (alpha + uni*discount)*pmass / (sum + alpha);
          //val = (alpha + ctx.data.u*discount)*pmass / (ctx.data.n + alpha);
        } else {
          val = (((double) nx - discount)) / (sum + alpha);
          //val = (((double) nx - discount)) / (ctx.data.n + alpha);
        }
        return val;
      }
    }
  }
  
  public double mass(X x, PPMTrie<X,UrnXPY.Cargo<X>> ctx, Mass<X> base) {
    TreeSet<X> excl = new TreeSet<X>();
    return mass(x,ctx,excl,base);
  }
  
  public double mass(X x, Mass<X> base) {
    return mass(x, context, base);
  }



  public Distribution<X> getPredictive(PPMTrie<X,UrnXPY.Cargo<X>> ctx,
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
    return getPredictive(context, base, set);
  }
  
  public void addMass(Mass<X> base, Iterable<X> set, Set<X> excl,
                      Hashtable<X,Long> mass, long budget) {
    /** Remaining budget */
    long rem = budget;
    /** Remaining mass */
    double rest = 1.0;
    /** Number of unique elements in the base distribution. */
    long uni = 0L;
    for (X x : excl) {
      double m = base.mass(x);
      rest -= m;
    }
    for (X x : set) {
      if (!excl.contains(x)) {
        double m = base.mass(x);
        double f = ((double) rem * (m/rest));
        long add = (long) f;
        mass.put(x, mass.get(x) + add);
        rem -= add;
        rest -= m;
        uni++;
      }
    }
    /* for some really large budgets, this may leave an unspent
     * amount of up to 7000 or so -- this happens because a double
     * cannot represent large numbers accurately enough.
     * We could divide up the 7000 once more, or we just spend it
     * all on any random element. */
    if (rem > 0) {
      if (rem > uni<<2) {
        // try to distribute it fairly
        addMass(base, set, excl, mass, rem);
      } else {
        // just spend it all on the first element
        Iterator<X> it = set.iterator();
        X x = it.next();
        mass.put(x, mass.get(x) + rem);
      }
    } else {
      throw new RuntimeException("Overspent budget");
    }
  }

  /** Adds, for each symbol, scaled predictive scores to a hash table.
    * @param ctx context node
    * @param base base distribution
    * @param set iterable set of symbols (used for base distribution)
    * @param mass hash table to which scores will be added
    * @param budget total integer budget to be divided up in proportion
    *               to each element's predictive probability mass */
  public void addMass(PPMTrie<X,UrnXPY.Cargo<X>> ctx, Set<X> excl,
                      Mass<X> base, Iterable<X> set, Hashtable<X,Long> mass,
                      long budget) {
    if (ctx == null) {
      addMass(base, set, excl, mass, budget);
    } else {
      long sum = 0L;
      long uni = 0L;
      for (Map.Entry<X,Integer> e : ctx.data.counts.entrySet()) {
        X x = e.getKey();
        if (!excl.contains(x)) {
          int nx = e.getValue();
          sum += nx;
          uni++;
        }
      }
      if (sum > 0) {
        //double norm = (ctx.data.n + alpha);
        double norm = (sum + alpha);
        long rem = budget;
        for (Map.Entry<X,Integer> e : ctx.data.counts.entrySet()) {
          X x = e.getKey();
          int nx = e.getValue();
          if (!excl.contains(x)) {
            double f = (nx - beta) / norm;
            long m = (long) (f * (double) budget);
            mass.put(x, mass.get(x) + m);
            rem -= m;
          }
        }
        // remaining budget
        budget = rem;
      }
      // now delegate remaining budget to parent contexts
      addMass(ctx.vine, ctx.data.counts.keySet(), base, set, mass, budget);
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
                              PPMTrie<X,UrnXPY.Cargo<X>> ctx,
                              Mass<X> base, Iterable<X> set,
                              long budget) {
    Hashtable<X,Long> table = new Hashtable<X,Long>();
    long u = 0;
    for (X x : set) {
      table.put(x,1L);
      u++;
    }
    /* add mass */
    addMass(ctx, new TreeSet<X>(), base, set, table, budget-u);
    return table;
  }


  public void encode(X sym, PPMTrie<X,UrnXPY.Cargo<X>> ctx, Mass<X> base,
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
 
  public X decode(PPMTrie<X,UrnXPY.Cargo<X>> ctx, Mass<X> base,
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




  /** Creates a dot-description of the PPMTrie. */
  protected void toDot(StringBuilder sb, PPMTrie<X,UrnXPY.Cargo<X>> ctx) {
    sb.append("  n"+ctx.hashCode()+" [label=\""
                   //+ctx.data.counts
                   +"n:"+ctx.data.n
                   +",u:"+ctx.data.u
                   +"\"];\n");
    if (ctx.children != null) {
      for (X x : ctx.children.keySet()) {
        PPMTrie<X,UrnXPY.Cargo<X>> n = ctx.children.get(x);
        this.toDot(sb,n);
        sb.append("  n"+ctx.hashCode()+" -> n"+n.hashCode()
                 +" [label=\""+x+"\"];\n");
      }
    }
    if (ctx.vine != null) {
      sb.append("  n"+ctx.hashCode()+" -> n"+ctx.vine.hashCode()+" [style=dotted,constraint=false];\n");
    }
  }

  public String toDot(PPMTrie<X,UrnXPY.Cargo<X>> ctx) {
    StringBuilder sb = new StringBuilder();
    sb.append("digraph PPMtree {\n");
    this.toDot(sb,ctx);
    sb.append("}");
    return sb.toString();
  }


}

