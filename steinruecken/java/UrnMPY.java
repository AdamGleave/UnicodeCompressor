/* $Id$ */
package uk.ac.cam.eng.ml.tcs27.compression;

import java.util.Iterator;
import java.util.Collection;
import java.util.Hashtable;
import java.util.ArrayList;

/** A Pitman-Yor restaurant with full blending,
  * (fanout,depth)-dependent discount and concentration parameters,
  * and 1TPD updates.
  * The fanout of a node is the number of unique symbols
  * observed in its context, and the depth is the node's
  * distance from the root. */
public class UrnMPY<X> extends UrnPY<X> {

  /** Depth and fanout-dependent discount parameters.
    * The first component indicates depth, the second component fanout.
    * Depth ranges from 0 to D, fanout from 1 to F. */
  double[][] betas;
  /** Depth and fanout-dependent concentration parameters. */
  double[][] alphas;

  /** Number of fanout-dependent parameters. */
  int maxf = 0;

  /** Number of depth-dependent parameters. */
  int maxd = 0;

  // { 0.25, 0.7, 0.8, 0.82, 0.84, 0.88, 0.91, 0.92, 0.93, 0.94, 0.95 };
  // { 0.62, 0.69, 0.74, 0.80, 0.95 };
  // { 0.5, 0.7, 0.8, 0.82, 0.84, 0.88, 0.91, 0.92, 0.93, 0.94, 0.95 };
  
  
  public UrnMPY() {
    double[][] half = { { 0.5 } };
    double[][] zero = { { 0.0 } };
    this.alphas = zero;
    this.betas  = half;
    this.maxf = 1;
    this.maxd = 1;
    init();
  }

  public UrnMPY(int depth) {
    this();
    this.maxdepth = depth;
  }

  /** Constructs a new UPY urn scheme with given
    * concentrations and discounts.
    * @param alphas concentrations
    * @param betas discounts */
  public UrnMPY(double[][] alphas, double[][] betas) {
    super();
    this.betas = betas;
    this.alphas = alphas;
    this.maxd = betas.length;
    this.maxf = betas[0].length;
    // FIXME: sanity checks missing
  }
  
  /** Constructs a new UPY urn scheme with given
    * concentrations and discounts.
    * @param alphas concentrations
    * @param betas discounts
    * @param depth maximal context depth */
  public UrnMPY(double[][] alphas, double[][] betas, int depth) {
    super();
    this.betas = betas;
    this.alphas = alphas;
    this.maxd = betas.length;
    this.maxf = betas[0].length;
    this.maxdepth = depth;
    // FIXME: sanity checks missing
  }
  
  /** Constructs a new MPY urn scheme with given
    * matrix of discount parameters. All concentrations
    * are set to zero.
    * @param bs discounts
    * @param depth maximal context depth */
  public UrnMPY(ArrayList<ArrayList<Double>> bs, int depth) {
    super();
    if (bs == null || bs.size() == 0) {
      this.maxd = 1;
      this.maxf = 1;
      this.betas = new double[][] { { 0.5 } };
      this.alphas = new double[][] { { 0.0 } };
    } else {
      this.maxd = bs.size();
      this.maxf = bs.get(0).size();
      this.betas  = new double[maxd][maxf];
      this.alphas = new double[maxd][maxf];
      for (int d=0; d<maxd; d++) {
        ArrayList<Double> fs = bs.get(d);
        for (int f=0; f<maxf; f++) {
          this.alphas[d][f]=0;
          this.betas[d][f]=fs.get(f);
        }
      }
    }
    this.maxdepth = depth;
    // FIXME: sanity checks missing
  }
  
  
  /** Constructs a new MPY urn scheme with given
    * matrices of concentrations and discounts.
    * @param as concentrations
    * @param bs discounts
    * @param depth maximal context depth */
  public UrnMPY(ArrayList<ArrayList<Double>> as,
                ArrayList<ArrayList<Double>> bs, int depth) {
    super();
    if (bs == null || as == null) {
      this.maxd = 1;
      this.maxf = 1;
      this.betas = new double[][] { { 0.5 } };
      this.alphas = new double[][] { { 0.0 } };
    } else {
      this.maxd = bs.size();
      this.maxf = bs.get(0).size();
      this.betas  = new double[maxd][maxf];
      this.alphas = new double[maxd][maxf];
      for (int d=0; d<maxd; d++) {
        ArrayList<Double> afs = as.get(d);
        ArrayList<Double> bfs = bs.get(d);
        for (int f=0; f<maxf; f++) {
          this.alphas[d][f] = afs.get(f);
          this.betas[d][f]  = bfs.get(f);
        }
      }
    }
    this.maxdepth = depth;
    // FIXME: sanity checks missing
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
    return "t=m:d="+maxdepth+":"+sb.toString()+" ["+maxf+"×"+maxd+"] ";
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

  /** Computes the gradient of a given symbol's mass
    * with respect to the <b>discount</b> parameter of selected
    * depth and fanout.
    * @param sd selected depth
    * @param sf selected fanout (1,2,3,etc)
    * @param d  current depth */
  public double betaGradient(X x, PPMTrie<X,UrnMPY.Cargo<X>> ctx,
                              Mass<X> base,
                              int sd, int sf, int d) {
    if (ctx == null) {
      return 0.0;
    }
    int u = ctx.data.u;
    /* // A computational shortcut:
    if ((uu+1) < u) {
      return 0.0;
    } else
    */
    if (ctx.data.n == 0) {  // FACT: n=0 <-> u=0
      return betaGradient(x,ctx.vine,base,sd,sf,d-1); // recurse up
    } else {
      double grad = 0.0;
      Integer nx = ctx.data.counts.get(x);
      if (nx == null) { nx = 0; }
      // check if the current urn locally uses beta[sd,sf]
      boolean match = ((d == sd) || (d >= maxd && sd == maxd-1))
                   && ((sf == u-1) || ((sf == maxf-1) && (u >= maxf)));
      if (match) {  // current urn contains selected discount
        // contribution from count
        if (nx > 0) {
          grad += -1.0;
        }
        // contribution from parent
        double pmass = mass(x,ctx.vine,base,d-1);
        grad += u * pmass;
      }
      double alpha = getConcentration(d,u-1);
      /* Useful fact: ctx.vine.u >= ctx.u */
      //if (d > sd && u <= (sf+1)) {
        double beta  = getDiscount(d,u-1);
        // parent restaurant contains selected discount
        double pgrad = betaGradient(x,ctx.vine,base,sd,sf,d-1);
        grad += (alpha + u*beta) * pgrad;
      //}
      /* normalise */
      grad /= (ctx.data.n + alpha);
      return grad;
    }
  }
  
 
 /*
  public PPMTrie<X,UrnMPY.Cargo<X>> learn(X x, PPMTrie<X,UrnMPY.Cargo<X>> ctx, int depth) {
    Integer nx = ctx.data.counts.get(x);
    if (nx != null) {
      ctx.data.counts.put(x,nx+1);
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
*/

  public void learn(X x) {
    //int d = getDepth(context);
    context = learn(x,context,d);
    if (d < maxdepth) { d++; }
  }


  
//  /** Computes the gradient of a given symbol's mass
//    * with respect to the <b>concentration</b> parameter of
//    * selected depth and fanout.
//    * @param sd selected depth
//    * @param sf selected fanout (1,2,3,etc)
//    * @param d  current depth */
//  public double alphaGradient(X x, PPMTrie<X,UrnMPY.Cargo<X>> ctx,
//                              Mass<X> base,
//                              int sd, int sf, int d) {
//    if (ctx == null) {
//      return 0.0;
//    }
//    int u = ctx.data.u;
//    /* // A computational shortcut:
//    if ((uu+1) < u) {
//      return 0.0;
//    } else
//    */
//    if (ctx.data.n == 0) {  // FACT: n=0 <-> u=0
//      return betaGradient(x,ctx.vine,base,sd,sf,d-1); // recurse up
//    } else {
//      double ga = 0.0;
//      Integer nx = ctx.data.counts.get(x);
//      double beta  = getDiscount(d,u-1);
//      double alpha = getConcentration(d,u-1);
//      double bnorm = (ctx.data.n + alpha);
//      double anorm = bnorm*bnorm;
//      if (nx == null) { nx = 0; }
//      // check if the current urn locally uses beta[sd,sf]
//      boolean match = ((d == sd) || (d >= maxd && sd == maxd-1))
//                   && ((sf == u-1) || ((sf == maxf-1) && (u >= maxf)));
//      if (match) {  // current urn contains selected discount
//        // contribution from count
//        if (nx > 0) {
//          ga += (beta - nx) / anorm;
//        }
//        // contribution from parent
//        double pmass = mass(x,ctx.vine,base,d-1);
//        ga += (ctx.data.n - u*beta) * pmass / anorm;
//        //gb += ( (u - 1.0) / bnorm  + alpha + u*beta ) * pmass / anorm;
//      }
//      /* Useful fact: ctx.vine.u >= ctx.u */
//      //if (d > sd && u <= (sf+1)) {
//        // parent restaurant contains selected discount
//        double pgrad = betaGradient(x,ctx.vine,base,sd,sf,d-1);
//        grad += (alpha + u*beta) * pgrad;
//      //}
//      /* normalise */
//      grad /= (ctx.data.n + alpha);
//      return grad;
//    }
//  }



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


  /** A method for mapping parameters to the entire real line.
    * Useful when using a gradient optimiser.
    * The inverse is given by <code>decodeParameters</code>.
    * @see #decodeParameters(ArrayList) */
  public ArrayList<Double> encodeParameters(ArrayList<Double> pars) {
    int ks = pars.size();
    int bl = ks / 2;
    ArrayList<Double> xs = new ArrayList<Double>(); // transformed betas
    ArrayList<Double> ys = new ArrayList<Double>(); // transformed alphas
    // map all parameters to the real line
    for (int k=0; k < bl; k++) {
      // transform beta to x (the real line)
      double beta = pars.get(bl+k);
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
      ys.add(y);
    }
    ys.addAll(xs); // alphas first, then betas
    return ys;
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
      // transforming the beta gradients to x gradients
      double beta = pars.get(k+d); // get the kth discount
      double beta_corr = beta * (1.0 - beta);
      grad.set(k+d, grad.get(k+d) * beta_corr);
      // transforming the alpha gradients to y gradients
      double alpha = pars.get(k);
      double alpha_corr = alpha + beta;
      grad.set(k, -grad.get(k) * alpha_corr);
    }
    return grad;
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

