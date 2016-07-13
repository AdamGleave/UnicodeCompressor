/* $Id$ */
package uk.ac.cam.eng.ml.tcs27.compression;

import java.util.Iterator;
import java.util.Collection;
import java.util.Hashtable;
import java.util.ArrayList;

/** A Pitman-Yor restaurant with full blending, 1TPD updates,
  * and dynamically changing (fanout,depth)-dependent discount
  * and concentration parameters.
  * The fanout of a node is the number of unique symbols
  * observed in its context, and the depth is the node's
  * distance from the root. */
public class UrnGMPY<X> extends UrnMPY<X> {

  /** Base distribution. Must be set at construction time. */
  Mass<X> base = null;
 
  /** Cached gradients of the previous symbol. */
  private ArrayList<Double> gradients = null;

  /** Epsilon / base step size. */
  double epsilon = 0.003;
  

  public UrnGMPY() {
    super();
  }

  public UrnGMPY(int depth, Mass<X> base) {
    super(depth);
    this.base = base;
  }

  /** Constructs a new MPY urn scheme with given
    * concentration and discount matrices.
    * @param alphas concentrations
    * @param betas discounts
    * @param base base distribution
    * @param eps gradient adjustment base step size */
  public UrnGMPY(double[][] alphas, double[][] betas, Mass<X> base, double eps) {
    super(alphas,betas);
    this.base = base;
    this.epsilon = eps;
  }
  
  /** Constructs a new MPY urn scheme with given
    * concentration and discount matrices.
    * @param alphas concentrations
    * @param betas discounts
    * @param depth maximal context depth
    * @param base base distribution
    * @param eps gradient adjustment base step size */
  public UrnGMPY(double[][] alphas, double[][] betas, int depth,
                 Mass<X> base, double eps) {
    super(alphas,betas,depth);
    this.base = base;
    this.epsilon = eps;
  }
  
  /** Constructs a new MPY urn scheme with given
    * matrix of discount parameters. All concentrations
    * are set to zero.
    * @param bs discounts
    * @param depth maximal context depth
    * @param base base distribution
    * @param eps gradient adjustment base step size */
  public UrnGMPY(ArrayList<ArrayList<Double>> bs, int depth,
                 Mass<X> base, double eps) {
    super(bs,depth);
    this.base = base;
    this.epsilon = eps;
  }
  
  
  /** Constructs a new MPY urn scheme with given
    * matrices of concentrations and discounts.
    * @param as concentrations
    * @param bs discounts
    * @param depth maximal context depth
    * @param base base distribution
    * @param eps gradient adjustment base step size */
  public UrnGMPY(ArrayList<ArrayList<Double>> as,
                 ArrayList<ArrayList<Double>> bs,
                 int depth, Mass<X> base, double eps) {
    super(as,bs,depth);
    this.base = base;
    this.epsilon = eps;
  }
  
  

  public String toString() {
    StringBuilder sb = new StringBuilder();
    if (alphas.length > 0) {
      sb.append("A=");
      for (int d=0; d<maxd; d++) {
        sb.append(alphas[d][0]);
        for (int f=1; f<maxf; f++) {
          sb.append(","+alphas[d][f]);
        }
        if (d+1 < maxd) { sb.append(";"); }
      }
      sb.append(":");
    }
    if (betas.length > 0) {
      sb.append("B=");
      for (int d=0; d<maxd; d++) {
        sb.append(betas[d][0]);
        for (int f=1; f<maxf; f++) {
          sb.append(","+betas[d][f]);
        }
        if (d+1 < maxd) { sb.append(";"); }
      }
    }
    String adpars = ":e="+epsilon;
    return "t=g:d="+maxdepth+adpars+":"+sb.toString()+" ["+maxf+"×"+maxd+"] ";
  }


  /** Returns the discount parameter for a given depth and node fanout. */
  public double getDiscount(int d, int f) {
    double[] fbetas = null;
    if (d < maxd) {
      fbetas = betas[d];
    } else {
      fbetas = betas[maxd-1];
    }
    if (f < maxf) {
      return fbetas[f];
    } else {
      return fbetas[maxf-1];
    }
  }
  
  /** Returns the concentration parameter for a given depdth and node fanout. */
  public double getConcentration(int d, int f) {
    double[] falphas = null;
    if (d < maxd) {
      falphas = alphas[d];
    } else {
      falphas = alphas[maxd-1];
    }
    if (f < maxf) {
      return falphas[f];
    } else {
      return falphas[maxf-1];
    }
  }

  public void setDiscount(int d, int f, double value) {
    if (d >= 0 && d < maxd && f >= 0 && f < maxf) {
      betas[d][f] = value;
    }
  }
  
  public void setConcentration(int d, int f, double value) {
    if (d >= 0 && d < maxd && f >= 0 && f < maxf) {
      alphas[d][f] = value;
    }
  }



  public double mass(X x, PPMTrie<X,UrnMPY.Cargo<X>> ctx, Mass<X> base, int depth) {
    if (ctx == null) {
      return base.mass(x);
    }
    double pmass = mass(x,ctx.vine,base,depth-1);
    Integer nx = ctx.data.counts.get(x);
    if (nx == null) { nx = 0; }
    int n = ctx.data.n;
    if (n == 0) {
      // if the total count is zero, use the parent distribution
      return pmass;
    } else {
      int u = ctx.data.u;
      double alpha = getConcentration(depth, u-1);  // for given depth and fanout
      double beta  = getDiscount(depth, u-1);
      double norm = (n + alpha);
      double val = Double.NEGATIVE_INFINITY;
      if (nx == 0) {
        val = pmass*(alpha + u*beta) / norm;
      } else {
        val = (((double) nx - beta) + pmass*(alpha + u*beta))
              / norm;
      }
      /*
      // FIXME : remove debug output
      if (alpha > 1.0) {
         System.err.println("mass: alpha="+alpha+"\t mass="+val);
      }
      */
      return val;
    }
  }

  public double mass(X x, PPMTrie<X,UrnMPY.Cargo<X>> ctx, Mass<X> base) {
    return mass(x, ctx, base, getDepth(ctx));
  }
  
  @Override
  public double mass(X x, Mass<X> base) {
    int d = getDepth(context);
    return mass(x, context, base, d);
  }

  int count = 1;

  public void learn(X x) {
    // COMPUTE GRADIENTS for current symbol
    this.gradients = getPlainGradients(x,context,base);
    // COMPUTE the probability mass of symbol to be learned
    double mx = mass(x,context,base);
    // ADJUST the parameters
    count++;
    //adjustParameters(this.gradients, mx, 1.0/Math.log(count));
    //adjustParameters(this.gradients, mx, 10.0/count);
    adjustParameters(this.gradients, mx, epsilon);
    // LEARN the current symbol
    context = learn(x,context,d);
    // ADJUST the current depth (only important for the first symbols)
    if (d < maxdepth) { d++; }
  }

  /** Adjusts the current parameters using the given gradients.
    * @param mx probability mass of previous symbol
    * @param eps base step size */
  public void adjustParameters(ArrayList<Double> gradients, double mx, double eps) {
    final int half = maxd*maxf;
    int k=0; // gradient index
    for (int d=0; d<maxd; d++) {
      for (int f=0; f<maxf; f++) {
        double alpha = getConcentration(d,f);
        double beta  = getDiscount(d,f);
        double ga = gradients.get(k);
        double gb = gradients.get(half+k);
        k++;
        if (Double.isNaN(ga)) { ga=0.0; }
        if (Double.isNaN(gb)) { gb=0.0; }
        double newa = alpha + eps * ga/mx;
        double newb = beta  + eps * gb/mx;
        if (newb < 0) { newb = 0.0000001; }
        if (newb > 1) { newb = 1.0; }
        if (newa < -newb) { newa = -newb + 0.0001; }
        if (newa > 65535) { newa = 65535; }
        setConcentration(d,f, newa);
        setDiscount(d,f, newb);
      }
    }
    //System.err.println(this);
  }

  
  /** Efficiently computes the PLAIN (untransformed) gradients
    * of a given symbol's mass
    * with respect to all depth- and fanout dependent
    * discount and concentration parameters. */
  public ArrayList<Double> getPlainGradients(X x,
                                     PPMTrie<X,UrnMPY.Cargo<X>> ctx,
                                     Mass<X> base) {
    // Initialise gradients
    ArrayList<Double> ga = new ArrayList<Double>();
    ArrayList<Double> gb = new ArrayList<Double>();
    for (int d=0; d<maxd; d++) {
      for (int f=0; f<maxf; f++) {
        ga.add(0.0);
        gb.add(0.0);
      }
    }
    // obtain node path from current node up to the root
    ArrayList<PPMTrie<X,UrnMPY.Cargo<X>>> path
                 = new ArrayList<PPMTrie<X,UrnMPY.Cargo<X>>>();
    // Make sure path has the correct number of components...
    for (int k=0; k<=maxdepth; k++) { path.add(null); }
    // ...and now set these components:
    PPMTrie<X,UrnMPY.Cargo<X>> tmp = ctx;
    int dp = getDepth(tmp);
    while (tmp != null) {
      path.set(dp,tmp);
      tmp = tmp.vine; dp--;
    }
    // So, now to business.
    // Let's compute all gradients at once, incrementally
    // from shallowest to deepest depth downwards.
    /* FACTS:
     * - There are maxd*maxf alphas and maxd*maxf betas.
     * - Computing the gradients at depth w requires the
     *   gradients for ALL the shallower depths.
     * - Each node on x's path in the context tree
     *   has exactly one fanout: only the parameters of
     *   those fanouts will get a non-zero gradient.
     */
    for (int d=0; d <= maxdepth; d++) {
      // fetch node at correct depth
      PPMTrie<X,UrnMPY.Cargo<X>> node = path.get(d);
      if (node == null) {
        //System.err.println("THIS SHOULD NEVER HAPPEN!");
      } else {
        /* NOTE: Plain gradients for alphas and betas. */
        int u = node.data.u; // total unique symbols at this node
        int n = node.data.n; // total count at this node
        if (n > 0) {
          double alpha = getConcentration(d,u-1);
          double beta  = getDiscount(d,u-1);
          double bnorm = n+alpha;
          double anorm = bnorm*bnorm;
          double pmass = mass(x,node.vine,base,d-1); // parent mass
          Integer nx = node.data.counts.get(x); // how often x occurs here
          if (nx == null) { nx = 0; }
          // Add contributions from all selected depths sd
          for (int sd=0; sd < maxd; sd++) { // or lev <= maxd ??
            for (int sf=0; sf < maxf; sf++) { // or lev <= maxd ??
              //int sf = u < maxf ? u-1 : maxf-1;
              double da = 0.0;
              double db = 0.0;
              int w = sd * maxf + sf; // index of the selected parameter!
              boolean match = ((d == sd) || (d >= maxd && sd == maxd-1))
                           && ((sf == u-1) || ((sf == maxf-1) && (u >= maxf)));
              if (match) {
                // current restaurant contains selected parameters
                if (nx > 0) {
                  da += (beta - nx) / anorm;
                  db += -1.0 / bnorm;   // (no transform)
                }
                // current restaurant contains selected parameters
                da += (n - u*beta) / anorm * pmass;
                db += node.data.u * pmass / bnorm; // (no transform)
              }
              if (true) {
                // parent restaurant contains selected parameters
                //if (node.vine != null) {
                  PPMTrie<X,UrnMPY.Cargo<X>> parent = node.vine;
                  int pw = w;
                  double pgrada = ga.get(pw);
                  double pgradb = gb.get(pw);
                  da += (alpha + u*beta) * pgrada / bnorm;
                  db += (alpha + u*beta) * pgradb / bnorm;
                //}
              }
              ga.set(w,da);
              gb.set(w,db);
            } // end sf-loop
          } // end sd-loop
        } else {
          // inherit from the parent (no work needed)
        }
        /* NOTE: ga and gb now contain the gradients correct down
         *       to depth d. */
      } // endif (node==null)
      // NOW ga and gb contain gradients correct up to level d
    } // end of incremental depth loop
    // NOW ga and gb contain correct gradients
    ArrayList<Double> gradients = new ArrayList<Double>(ga);
    gradients.addAll(gb);
    return gradients;
  }

  

  /** Efficiently computes the gradients of a given symbol's mass
    * with respect to all depth- and fanout dependent
    * discount and concentration parameters. */
  public ArrayList<Double> getGradients(X x, PPMTrie<X,UrnMPY.Cargo<X>> ctx,
                                        Mass<X> base) {
    // Initialise gradients
    ArrayList<Double> ga = new ArrayList<Double>();
    ArrayList<Double> gb = new ArrayList<Double>();
    for (int d=0; d<maxd; d++) {
      for (int f=0; f<maxf; f++) {
        ga.add(0.0);
        gb.add(0.0);
      }
    }
    // obtain node path from current node up to the root
    ArrayList<PPMTrie<X,UrnMPY.Cargo<X>>> path
                 = new ArrayList<PPMTrie<X,UrnMPY.Cargo<X>>>();
    // Make sure path has the correct number of components...
    for (int k=0; k<=maxdepth; k++) { path.add(null); }
    // ...and now set these components:
    PPMTrie<X,UrnMPY.Cargo<X>> tmp = ctx;
    int dp = getDepth(tmp);
    while (tmp != null) {
      path.set(dp,tmp);
      tmp = tmp.vine; dp--;
    }
    // So, now to business.
    // Let's compute all gradients at once, incrementally
    // from shallowest to deepest depth downwards.
    /* FACTS:
     * - There are maxd*maxf alphas and maxd*maxf betas.
     * - Computing the gradients at depth w requires the
     *   gradients for ALL the shallower depths.
     * - Each node on x's path in the context tree
     *   has exactly one fanout: only the parameters of
     *   those fanouts will get a non-zero gradient.
     */
    for (int d=0; d <= maxdepth; d++) {
      // fetch node at correct depth
      PPMTrie<X,UrnMPY.Cargo<X>> node = path.get(d);
      if (node == null) {
        // when that happens, leave gradients untouched
        //System.err.println("THIS SHOULD NEVER HAPPEN!");
        /*
        for (int w=0; w < maxd*maxf; w++) {
          ga.set(w, 0.0);
          gb.set(w, 0.0);
        }
        */
      } else {
        /* NOTE: gradients are computed for alphas and betas.
         * Due to a coordinate transform for the alpha gradients,
         *        y[d][u] = log(a[d][u] + b[d][u])
         * the betas become a function of the alphas, which makes
         * some things a little more complicated.
         * For details, see ppm.tex */
        int u = node.data.u; // total unique symbols at this node
        int n = node.data.n; // total count at this node
        if (n > 0) {
          double alpha = getConcentration(d,u-1);
          double beta  = getDiscount(d,u-1);
          double bnorm = n+alpha;
          double anorm = bnorm*bnorm;
          double pmass = mass(x,node.vine,base,d-1); // parent mass
          Integer nx = node.data.counts.get(x); // how often x occurs here
          if (nx == null) { nx = 0; }
          // Add contributions from all selected depths sd
          for (int sd=0; sd < maxd; sd++) { // or lev <= maxd ??
            for (int sf=0; sf < maxf; sf++) { // or lev <= maxd ??
              //int sf = u < maxf ? u-1 : maxf-1;
              double da = 0.0;
              double db = 0.0;
              int w = sd * maxf + sf; // index of the selected parameter!
              //boolean match = ((d == sd) || (d >= maxd && sd == maxd-1))
              //             && ((sf == u-1) || ((sf == maxf-1) && (u >= maxf)));
              boolean match = ((d == sd) || (d >= maxd && sd == maxd-1))
                           && ((sf == u-1) || ((sf == maxf-1) && (u >= maxf)));
              if (match) {
                // current restaurant contains selected parameters
                if (nx > 0) {
                  da += (beta - nx) / anorm;
                  db -= -1.0 / bnorm + (nx - beta) / anorm;
                  //db += -1.0 / bnorm;    // without coordinate transform
                }
                // current restaurant contains selected parameters
                da += (n - u*beta) / anorm * pmass;
                db -= ((u - 1.0) / bnorm + (alpha + u*beta) / anorm) * pmass;
                //db += node.data.u * pmass / bnorm;   // without transform
                //da = 0;
                //db = Double.NaN;
              }
              if (true) {
                // parent restaurant contains selected parameters
                //if (node.vine != null) {
                  PPMTrie<X,UrnMPY.Cargo<X>> parent = node.vine;
                  //int pd = d >= maxd ? maxd-1 : d-1; // parent depth
                  //int pf = parent.data.u > maxf ? maxf-1 : u-1; // parent fanout
                  //int pw = pd * maxf + pf; // parent parameter coordinate
                  int pw = w;
                  double pgrada = ga.get(pw);
                  double pgradb = gb.get(pw);
                  da += (alpha + u*beta) * pgrada / bnorm;
                  db += (alpha + u*beta) * pgradb / bnorm;
                //}
              }
              ga.set(w,da);
              gb.set(w,db);
            } // end sf-loop
          } // end sd-loop
        } else {
          // inherit from the parent (no work needed)
        }
        /* NOTE: ga and gb now contain the gradients correct down
         *       to depth d. */
      } // endif (node==null)
      // NOW ga and gb contain gradients correct up to level d
    } // end of incremental depth loop
    // Since this method is all wrong, let's compute
    // it again in the safe way:
    /*
    for (int sd=0; sd<maxd; sd++) {
      for (int sf=0; sf<maxf; sf++) {
        gb.set(d, betaGradient(x,ctx,base,sd,sf,getDepth(ctx)));
      }
    }
    */
    // NOW ga and gb contain correct gradients
    ArrayList<Double> gradients = new ArrayList<Double>(ga);
    gradients.addAll(gb);
    return gradients;
  }






  /** Computes the gradients of a given symbol's log mass
    * with respect to the parameters of selected depth. */
  public ArrayList<Double> logGradients(X x, PPMTrie<X,UrnMPY.Cargo<X>> ctx,
                                        Mass<X> base) {
    //ArrayList<Double> g = createGradientList();
    ArrayList<Double> g = getGradients(x,ctx,base);

    //int depth = getDepth(ctx);
    //int half = maxd*maxf;
    //int k=0;
    // Add alphas
    /*
    for (int d=0; d<maxd; d++) {
      for (int f=0; f<maxf; f++) {
        //g.set(half + d*maxf + f, betaGradient(x,ctx,base,d,f,depth));
        g.set(k, alphaGradient(x,ctx,base,d,f,depth));
        k++;
      }
    }
    */
    // Add betas
    /*
    for (int d=0; d<maxd; d++) {
      for (int f=0; f<maxf; f++) {
        //g.set(half + d*maxf + f, betaGradient(x,ctx,base,d,f,depth));
        g.set(k, betaGradient(x,ctx,base,d,f,depth));
        k++;
      }
    }
    */
    // convert gradients of P to gradients of logP:
    double mass = mass(x,ctx,base);
    for (int j=0; j<g.size(); j++) {
      g.set(j, g.get(j) / mass);
    }
    //System.err.println("grad = "+g);
    //System.err.println("mass["+d+","+depth+"] = "+mass);
    return g;
  }

  /** Computes ∂/∂β logP(x | ...) for all discount parameters β, and
    * sums them onto the specified array list. */
  public void addLogMassGradients(X x, PPMTrie<X,UrnMPY.Cargo<X>> ctx,
                              Mass<X> base, ArrayList<Double> grad) {
    //int ctxdepth = getDepth(ctx);
    ArrayList<Double> g = logGradients(x,ctx,base);
    for (int k=0; k<grad.size(); k++) {
      grad.set(k, grad.get(k) + g.get(k));
    }
  }
  
  /** Creates a list of gradients, initialised to zero. */
  public ArrayList<Double> createGradientList() {
    ArrayList<Double> grad = new ArrayList<Double>(maxd*maxf);
    for (int d=0; d<maxd; d++) {
      for (int f=0; f<maxf; f++) {
        grad.add(0.0); // alphas
        grad.add(0.0); // betas
      }
    }
    return grad;
  }


  /** Returns the current parameters, serialised to a list. */
  public ArrayList<Double> getCurrentParameters() {
    ArrayList<Double> pars = new ArrayList<Double>();
    for (int d=0; d<maxd; d++) {
      for (int f=0; f<maxf; f++) {
        pars.add(alphas[d][f]);
      }
    }
    for (int d=0; d<maxd; d++) {
      for (int f=0; f<maxf; f++) {
        pars.add(betas[d][f]);
      }
    }
    return pars;
  }



  /** Creates an urn from the given array parameters.
    * The parameters must be presented in the same order as used
    * by encodeParameters, decodeParameters, encodeGradients, etc. */
  public UrnMPY<X> createUrn(ArrayList<Double> pars) {
    double[][] as = new double[maxd][maxf]; // zeros by default
    double[][] bs = new double[maxd][maxf]; // zeros by default
    int k = 0;
    for (int d=0; d<maxd; d++) {
      for (int f=0; f<maxf; f++) {
        as[d][f]  = pars.get(k);
        k++;
      }
    }
    for (int d=0; d<maxd; d++) {
      for (int f=0; f<maxf; f++) {
        bs[d][f]  = pars.get(k);
        k++;
      }
    }
    return new UrnMPY<X>(as,bs,maxdepth);
  }

  public String stringFromParameters(ArrayList<Double> pars) {
    //StringBuilder sa = new StringBuilder();
    StringBuilder sb = new StringBuilder();
    int k=0;
    for (int d=0; d<maxd; d++) {
      if (d==0) {
        sb.append(  "α=[[");
      } else {
        sb.append("\n   [");
      }
      for (int f=0; f<maxf; f++) {
        if (f > 0) {
          sb.append(", ");
        }
        sb.append(pars.get(k));
        k++;
      }
      sb.append("]");
    }
    sb.append("]\n");
    for (int d=0; d<maxd; d++) {
      if (d==0) {
        sb.append(  "β=[[");
      } else {
        sb.append("\n   [");
      }
      for (int f=0; f<maxf; f++) {
        if (f > 0) {
          //sa.append(", ");
          sb.append(", ");
        }
        sb.append(pars.get(k));
        k++;
      }
      sb.append("]");
    }
    sb.append("]");
    return sb.toString();
  }


  /** Adds, for each symbol, scaled predictive scores to a hash table.
    * @param ctx context node
    * @param base base distribution
    * @param set iterable set of symbols (used for base distribution)
    * @param mass hash table to which scores will be added
    * @param budget total integer budget to be divided up in proportion
    *               to each element's predictive probability mass
    * @param depth context depth */
  public void addMass(PPMTrie<X,UrnMPY.Cargo<X>> ctx, Mass<X> base,
                      Iterable<X> set, Hashtable<X,Long> mass,
                      long budget, int depth) {
    if (ctx != null) {
      if (ctx.data.n > 0) {
        long sum = 0L; // actual sum spent (may differ from budget)
        double discount = getDiscount(depth, ctx.data.u-1);
        double conc     = getConcentration(depth, ctx.data.u-1);
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
                              PPMTrie<X,UrnMPY.Cargo<X>> ctx,
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



  public Distribution<X> getPredictive(PPMTrie<X,UrnMPY.Cargo<X>> ctx,
                                       Mass<X> base, Iterable<X> set) {
    Hashtable<X,Double> table = new Hashtable<X,Double>();
    for (X x : set) {
      table.put(x, mass(x,ctx,base));
    }
    DiscreteLookup<X> dl = new DiscreteLookup<X>(table);
    return dl;
  }

}

