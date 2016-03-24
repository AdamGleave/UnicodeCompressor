/* Automated copy from build process */
/* $Id: UrnDCPY.java,v 1.14 2014/09/18 16:31:39 chris Exp $ */

import java.util.Iterator;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.ArrayList;

/** A Pitman-Yor restaurant with full blending,
  * depth-dependent discount and concentration parameters,
  * and exclusive updates. */
public class UrnDCPY<X> extends UrnPY<X> {

  /** Depth-dependent discount parameters. */
  double[] betas;
  /** Depth-dependent concentration parameters. */
  double[] alphas;
  /** Depth of the parameter vectors. */
  int pardepth = 0;

  // { 0.25, 0.7, 0.8, 0.82, 0.84, 0.88, 0.91, 0.92, 0.93, 0.94, 0.95 };
  // { 0.62, 0.69, 0.74, 0.80, 0.95 };
  // { 0.5, 0.7, 0.8, 0.82, 0.84, 0.88, 0.91, 0.92, 0.93, 0.94, 0.95 };
  
  public UrnDCPY() {
    double[] half = { 0.5 };
    double[] zero = { 0.0 };
    this.alphas = zero;
    this.betas  = half;
    this.maxdepth = betas.length-1;
    this.pardepth = 0;
    init();
  }

  public UrnDCPY(int depth) {
    this();
    this.maxdepth = depth;
  }
  
  /** Constructs a new DCPY urn scheme with given
    * concentrations and discounts.
    * @param alphas concentrations
    * @param betas discounts */
  public UrnDCPY(double[] alphas, double[] betas) {
    super();
    this.betas = betas;
    this.alphas = alphas;
    this.pardepth = betas.length-1;
    this.maxdepth = pardepth;
  }

  /** Constructs a new DCPY urn scheme with given
    * concentrations and discounts.
    * @param alphas concentrations
    * @param betas discounts
    * @param maxdepth maximal context depth */
  public UrnDCPY(double[] alphas, double[] betas, int maxdepth) {
    this(alphas,betas);
    this.maxdepth = maxdepth;
  }
  
  public UrnDCPY(int maxdepth,
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
    this.pardepth = ms-1;
    this.maxdepth = maxdepth;
  }
  
  public UrnDCPY(ArrayList<Double> concentrations,
                 ArrayList<Double> discounts) {
    this(discounts.size()-1, concentrations, discounts);
  }
  
  public UrnDCPY(ArrayList<Double> discounts) {
    this.betas = new double[discounts.size()];
    for (int k=0; k<discounts.size(); k++) {
      this.betas[k] = discounts.get(k);
    }
    this.alphas = new double[] { 0.0 };
    this.pardepth = discounts.size()-1;
    this.maxdepth = this.pardepth;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    if (alphas.length >= 1) {
      sb.append("a=");
      sb.append(alphas[0]);
      for (int d=1; d<alphas.length; d++) {
        sb.append(","+alphas[d]);
      }
      sb.append(":");
    }
    if (betas.length >= 1) {
      sb.append("b=");
      sb.append(betas[0]);
      for (int d=1; d<betas.length; d++) {
        sb.append(","+betas[d]);
      }
      sb.append("");
    }
    return "UrnDCPY,t=b:d="+maxdepth+":"+sb.toString()+")";
  }

  /** Returns the discount parameter for a given context depth. */
  public double getDiscount(int depth) {
    if (depth < betas.length) {
      return betas[depth];
    } else {
      return betas[betas.length-1];
    }
  }
  
  /** Returns the concentration parameter for a given context depth. */
  public double getConcentration(int depth) {
    if (depth < alphas.length) {
      return alphas[depth];
    } else {
      return alphas[alphas.length-1];
    }
  }


  public double mass(X x, PPMTrie<X,UrnDCPY.Cargo<X>> ctx,
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
      double alpha = getConcentration(depth);
      double beta  = getDiscount(depth);
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
  
  @Override
  public double mass(X x, PPMTrie<X,UrnDCPY.Cargo<X>> ctx, Mass<X> base) {
    return mass(x,ctx,base,getDepth(ctx));
  }
  
  @Override
  public double mass(X x, Mass<X> base) {
    return mass(x, context, base, getDepth(context));
  }

  /** Computes the gradient of a given symbol's mass
    * with respect to the <b>discount</b> parameter of selected depth.
    * @param d current node depth (fetch via getDepth)
    * @param depth selected depth */
  public double betaGradient(X x, PPMTrie<X,UrnDPY.Cargo<X>> ctx,
                              Mass<X> base,
                              int d, int depth) {
    if (ctx == null) {
      return 0.0;
    }
    //int maxdepth = betas.length-1;
    // TODO: the line below is an effective, but inelegant fix
    //if (depth != maxdepth) { maxdepth++; }
    if (ctx.data.n == 0) {
      return betaGradient(x,ctx.vine,base,d-1,depth);
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
        double pgrad = betaGradient(x,ctx.vine,base,d-1,depth);
        grad += (getConcentration(d) + ctx.data.u*getDiscount(d)) * pgrad;
      }
      grad /= (ctx.data.n + getConcentration(d));
      return grad;
    }
  }



  /** Computes the gradients of a given symbol's mass
    * with respect to discount and concentration parameters. */
  public ArrayList<Double> getGradients(X x, PPMTrie<X,UrnDCPY.Cargo<X>> ctx,
                                        Mass<X> base) {
    // initialise gradients
    ArrayList<Double> ga = new ArrayList<Double>();
    ArrayList<Double> gb = new ArrayList<Double>();
    for (int k=0; k<alphas.length; k++) { ga.add(0.0); }
    for (int k=0; k<betas.length; k++)  { gb.add(0.0); }
    // obtain node path from current node up to the root
    ArrayList<PPMTrie<X,UrnDCPY.Cargo<X>>> path
                 = new ArrayList<PPMTrie<X,UrnDCPY.Cargo<X>>>();

    for (int k=0; k<=maxdepth+1; k++) {
      path.add(ctx);
    }
    PPMTrie<X,UrnDCPY.Cargo<X>> tmp = ctx;
    int dp = getDepth(tmp);
    while (tmp != null) {
      path.set(dp,tmp);
      tmp = tmp.vine; dp--;
    }
    // compute all gradients at once,
    // incrementally from shallowest to deepest depth downwards
    for (int d=0; d <= maxdepth; d++) {
      // fetch node at correct depth
      PPMTrie<X,UrnDCPY.Cargo<X>> node = path.get(d);
      if (node == null) {
        // set all gradients to zero
        for (int w=0; w <= pardepth; w++) {
          ga.set(w, 0.0);
          gb.set(w, 0.0);
        }
      } else {
        /* NOTE: gradients are computed for alphas and betas.
         * Due to a coordinate transform for the alpha gradients,
         *        y[d] = log(a[d] + b[d])
         * the betas become a function of the alphas, which makes
         * some things a little more complicated.
         * For details, see ppm.tex */
        Integer nx = node.data.counts.get(x);
        if (nx == null) { nx = 0; }
        double bnorm = (node.data.n+getConcentration(d));
        double anorm = bnorm*bnorm;
        // Add contributions from all depths w up to pardepth
        for (int lev=0; lev <= pardepth; lev++) {
          int w = lev > pardepth ? pardepth : lev;
          double da = 0.0;
          double db = 0.0;
          if ((d == lev || (lev == pardepth && d > pardepth)) && nx > 0) {
            // current restaurant contains selected parameters
            da += (getDiscount(d) - nx) / anorm;
            //db -= 1.0 / bnorm;
            db += -1.0 / bnorm + (nx - getDiscount(d)) / anorm;
          }
          if (d == lev || (lev == pardepth && d > pardepth)) {
            // current restaurant contains selected parameters
            double pmass = mass(x,node.vine,base,d-1);
            da += (node.data.n - node.data.u*getDiscount(d))
                  * pmass / anorm;
            //db += node.data.u * pmass / bnorm;
            db += (  (node.data.u - 1.0) / bnorm
                   + (getConcentration(d) + node.data.u*getDiscount(d)) / anorm
                  ) * pmass;
          }
          if (true) {
            // parent restaurant contains selected parameters
            double pgrada = ga.get(w);
            double pgradb = gb.get(w);
            da += (getConcentration(d) + node.data.u*getDiscount(d)) * pgrada / bnorm;
            db += (getConcentration(d) + node.data.u*getDiscount(d)) * pgradb / bnorm;
          }
          ga.set(w,da);
          gb.set(w,db);
        }
        /* NOTE: ga and gb now contain the gradients correct down
         *       to depth d. */
        //System.err.println("beta gradients: "+gb);
      }
      // NOW ga and gb contain gradients correct up to level d
    }
    // Since this method is all wrong, let's compute
    // it again in the safe way:
    /*
    for (int d=0; d<=maxdepth; d++) {
      gb.set(d, betaGradient(x,ctx,base,getDepth(ctx),d));
    }
    */
    // NOW ga and gb contain correct gradients
    ArrayList<Double> gradients = new ArrayList<Double>(ga);
    gradients.addAll(gb);
    return gradients;
  }

  /** Computes the gradients of a given symbol's log mass
    * with respect to the parameters of selected depth. */
  public ArrayList<Double> logGradients(X x, PPMTrie<X,UrnDCPY.Cargo<X>> ctx,
                                        Mass<X> base) {
    double mass = mass(x,ctx,base);
    ArrayList<Double> g = getGradients(x,ctx,base);
    for (int k=0; k<g.size(); k++) {
      g.set(k, g.get(k) / mass);
    }
    //System.err.println("grad["+d+","+depth+"] = "+grad);
    //System.err.println("mass["+d+","+depth+"] = "+mass);
    return g;
  }

  /** Computes ∂/∂β logP(x | ...) for all discount parameters β, and
    * sums them onto the specified array list. */
  public void addLogMassGradients(X x, PPMTrie<X,UrnDCPY.Cargo<X>> ctx,
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
    for (int d=0; d<=pardepth; d++) {
      grad.add(0.0);
      grad.add(0.0);
    }
    return grad;
  }

  public ArrayList<Double> getCurrentParameters() {
    ArrayList<Double> pars = new ArrayList<Double>();
    for (double a : alphas) {
      pars.add(a);
    }
    for (double b : betas) {
      pars.add(b);
    }
    return pars;
  }

  /** A method for mapping parameters to
    * the entire real line.  Useful when using a gradient optimiser.
    * The inverse is given by <code>decodeParameters</code>.
    * @see #decodeParameters(ArrayList) */
  public ArrayList<Double> encodeParameters(ArrayList<Double> pars) {
    int d = pars.size() / 2;
    ArrayList<Double> xs = new ArrayList<Double>();
    ArrayList<Double> ys = new ArrayList<Double>();
    // map all parameters to the real line
    for (int k=0; k<d; k++) {
      // transform beta to x (the real line)
      double beta = pars.get(k+d);
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
      // transform alpha to y (the real line)
      double alpha = pars.get(k);
      double y = Math.log(alpha + beta);
      ys.add(y); // first alpha
    }
    // now encode them in a single array
    // ys (alphas) first, xs (betas) second
    ys.addAll(xs);
    return ys;
  }

  /** Adjusts pure parameter gradients (Δabs) to be encoded parameter
    * gradients (Δxys).
    * This operation is currently implemented destructively,
    * overwriting <code>grad</code> (but not <code>pars</code>).
    * @param grad pure gradients
    * @param pars pure parameters
    * @return encoded (adjusted) gradients
    * @see #encodeParameters(ArrayList) */
  public ArrayList<Double> encodeGradients(ArrayList<Double> grad,
                                           ArrayList<Double> pars) {
    int d = pars.size() / 2;
    for (int k=0; k<d; k++) {
      // correction for the betas:
      double beta = pars.get(k+d); // get the kth discount
      double beta_corr = beta * (1.0 - beta);
      grad.set(k+d, -grad.get(k+d) * beta_corr);
      // correction for the alphas:
      double alpha = pars.get(k);        // get the kth concentration
      double alpha_corr = alpha + beta;
      grad.set(k, -grad.get(k) * alpha_corr);
    }
    return grad;
  }

  /** A method for decoding back the parameters from reals.
    * This is the inverse of <code>encodeParameters</code>.
    * @see #encodeParameters(ArrayList) */
  public ArrayList<Double> decodeParameters(ArrayList<Double> pars) {
    Tuple<ArrayList<Double>,ArrayList<Double>> tuple = decodeAlphasBetas(pars);
    // extract lists
    ArrayList<Double> alphas = tuple.get0();
    ArrayList<Double> betas  = tuple.get1();
    // return the concatenation of both lists
    alphas.addAll(betas);
    return alphas;
  }

  
  public Tuple<ArrayList<Double>,ArrayList<Double>>
                     decodeAlphasBetas(ArrayList<Double> pars) {
    int d = pars.size() / 2;
    ArrayList<Double> alphas = new ArrayList<Double>();
    ArrayList<Double> betas = new ArrayList<Double>();
    // transform back the parameters
    for (int k=0; k<d; k++) {
      // for beta: compute inverse logit:
      double logit = pars.get(k+d);
      double eep = Math.exp(logit);
      double beta = eep / (eep+1.0);
      betas.add(beta);
      // for alpha: compute inverse log(alpha + beta):
      double log = pars.get(k);
      double alpha = Math.exp(log)-beta;
      alphas.add(alpha);
    }
    return Tuple.of(alphas,betas);
  }

  public UrnDCPY<X> createUrn(ArrayList<Double> pars) {
    int d = pars.size() / 2;
    double[] alphas = new double[d];
    double[] betas = new double[d];
    // (maybe using List.subList would be easier... just saying!)
    for (int k=0; k<d; k++) {
      alphas[k] = pars.get(k);
      betas[k]  = pars.get(k+d);
    }
    return new UrnDCPY<X>(alphas,betas,maxdepth);
  }

  public String stringFromParameters(ArrayList<Double> pars) {
    int d = pars.size() / 2;
    List<Double> alphas = pars.subList(0,d);
    List<Double> betas = pars.subList(d,pars.size());
    return "α="+alphas+",\nβ="+betas;
  }


  /** Adds, for each symbol, scaled predictive scores to a hash table.
    * @param ctx context node
    * @param base base distribution
    * @param set iterable set of symbols (used for base distribution)
    * @param mass hash table to which scores will be added
    * @param budget total integer budget to be divided up in proportion
    *               to each element's predictive probability mass
    * @param depth context depth */
  public void addMass(PPMTrie<X,UrnDCPY.Cargo<X>> ctx, Mass<X> base,
                      Iterable<X> set, Hashtable<X,Long> mass,
                      long budget, int depth) {
    if (ctx != null) {
      if (ctx.data.n > 0) {
        long sum = 0L; // actual sum spent (may differ from budget)
        double discount = getDiscount(depth);
        double conc     = getConcentration(depth);
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
                              PPMTrie<X,UrnDCPY.Cargo<X>> ctx,
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



  public Distribution<X> getPredictive(PPMTrie<X,UrnDCPY.Cargo<X>> ctx,
                                       Mass<X> base, Iterable<X> set) {
    Hashtable<X,Double> table = new Hashtable<X,Double>();
    for (X x : set) {
      table.put(x, mass(x,ctx,base));
    }
    DiscreteLookup<X> dl = new DiscreteLookup<X>(table);
    return dl;
  }

}

