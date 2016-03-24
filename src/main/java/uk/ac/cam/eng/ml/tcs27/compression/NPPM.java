/* Automated copy from build process */
/* $Id: NPPM.java,v 1.52 2015/08/11 11:28:16 chris Exp $ */

import java.util.Iterator;
import java.util.Hashtable;
import java.util.HashSet;
import java.util.Set;
import java.util.Arrays;
import java.util.Vector;
import java.util.Random;
import java.util.Collection;
import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintStream;
import java.io.IOException;
import java.io.FileNotFoundException;

/** Implementation of generalised PPM.
  * This is general in the sense that any escape mechanism can be
  * passed in.
  * @see PPMMethod
  * @see PPM */
public class NPPM<X,Y> extends SimpleMass<X> implements AdaptiveCode<X> {

  /** Base distribution. */
  Distribution<X> base = null;
  
  /** Iterable collection of base elements. */
  Iterable<X> set = null;

  /** Total number of symbols processed. */
  public long count = 0;

  /** Urn scheme. */
  PPMUrn<X,Y> urn;


  /** Minimal constructor. */
  private NPPM() {
  }

  /** Constructs a new PPM process of given context depth and base
    * distribution.
    * @param maxdepth maximum depth of the context tree
    * @param urn a suitable PPM urn scheme
    * @param base base distribution over symbols
    * @param set an iterable collection of symbols
    */
  public NPPM(int maxdepth, PPMUrn<X,Y> urn, Distribution<X> base, Iterable<X> set) {
    //this.maxdepth = maxdepth;
    this.base = base;
    this.set = set;
    this.urn = urn;
    //this.context = urn.createTrie();
    //this.root = this.context;
    //urn.init(context);
  }

  public static <X> NPPM<X,UrnPY.Cargo<X>>
         newBlendingPPM(int maxdepth, Distribution<X> base, Iterable<X> set) {
    return new NPPM<X,UrnPY.Cargo<X>>(maxdepth,new UrnPY<X>(maxdepth),base,set);
  }
  
  public static <X> NPPM<X,UrnPY.Cargo<X>>
         newBlendingPPM(int maxdepth, Distribution<X> base, Iterable<X> set,
                        double alpha, double beta) {
    return new NPPM<X,UrnPY.Cargo<X>>(maxdepth,
                                      new UrnPY<X>(alpha,beta,maxdepth),
                                      base, set);
  }
  
  public static <X> NPPM<X,UrnXPY.Cargo<X>>
         newExclusivePPM(int maxdepth, Distribution<X> base, Iterable<X> set) {
    return new NPPM<X,UrnXPY.Cargo<X>>(maxdepth,new UrnXPY<X>(maxdepth),base,set);
  }
  
  public static <X> NPPM<X,UrnXPY.Cargo<X>>
         newExclusivePPM(int maxdepth, Distribution<X> base, Iterable<X> set,
                         double alpha, double beta) {
    return new NPPM<X,UrnXPY.Cargo<X>>(maxdepth,
                                       new UrnXPY<X>(alpha,beta,maxdepth),
                                       base,set);
  }
  
  public static <X> NPPM<X,UrnDPY.Cargo<X>>
         newDepthBlendingPPM(int maxdepth, Distribution<X> base, Iterable<X> set) {
    return new NPPM<X,UrnDPY.Cargo<X>>(maxdepth,new UrnDPY<X>(maxdepth),base,set);
  }
  
  public static <X> NPPM<X,UrnDPY.Cargo<X>>
         newDepthBlendingPPM(int maxdepth, Distribution<X> base,
                                           Iterable<X> set,
                                           double[] discounts) {
    return new NPPM<X,UrnDPY.Cargo<X>>(maxdepth,new UrnDPY<X>(discounts),base,set);
  }
  
  public static <X> NPPM<X,UrnDCPY.Cargo<X>>
         newDepthBlendingPPM(int maxdepth, Distribution<X> base,
                                           Iterable<X> set,
                                           double[] alphas,
                                           double[] betas) {
    return new NPPM<X,UrnDCPY.Cargo<X>>(maxdepth,new UrnDCPY<X>(alphas,betas),base,set);
  }

  public static <X> NPPM<X,UrnDPY.Cargo<X>>
         newDepthBlendingPPM(int maxdepth, Distribution<X> base,
                                           Iterable<X> set,
                                           ArrayList<Double> discounts) {
    return new NPPM<X,UrnDPY.Cargo<X>>(maxdepth,new UrnDPY<X>(discounts),base,set);
  }
  
  public static <X> NPPM<X,UrnDCPY.Cargo<X>>
         newDepthBlendingPPM(int maxdepth, Distribution<X> base,
                                           Iterable<X> set,
                                           ArrayList<Double> alphas,
                                           ArrayList<Double> betas) {
    return new NPPM<X,UrnDCPY.Cargo<X>>(maxdepth,new UrnDCPY<X>(maxdepth,alphas,betas),base,set);
  }
  
  public static <X> NPPM<X,UrnUPY.Cargo<X>>
         newBranchBlendingPPM(int maxdepth, Distribution<X> base,
                                           Iterable<X> set,
                                           ArrayList<Double> alphas,
                                           ArrayList<Double> betas) {
    return new NPPM<X,UrnUPY.Cargo<X>>(maxdepth,new UrnUPY<X>(maxdepth,alphas,betas),base,set);
  }
  
  public static <X> NPPM<X,UrnMPY.Cargo<X>>
         newMatrixBlendingPPM(int maxdepth, Distribution<X> base,
                                            Iterable<X> set,
                                            ArrayList<ArrayList<Double>> betas) {
    return new NPPM<X,UrnMPY.Cargo<X>>(maxdepth,new UrnMPY<X>(betas,maxdepth),base,set);
  }
  
  public static <X> NPPM<X,UrnMPY.Cargo<X>>
         newMatrixBlendingPPM(int maxdepth, Distribution<X> base,
                                            Iterable<X> set,
                                            ArrayList<ArrayList<Double>> alphas,
                                            ArrayList<ArrayList<Double>> betas) {
    return new NPPM<X,UrnMPY.Cargo<X>>(maxdepth,
                                       new UrnMPY<X>(alphas,betas,maxdepth),
                                       base, set);
  }


  public static <X> NPPM<X,UrnGMPY.Cargo<X>>
         newGMatrixBlendingPPM(int maxdepth, Distribution<X> base,
                                             Iterable<X> set,
                                             ArrayList<ArrayList<Double>> alphas,
                                             ArrayList<ArrayList<Double>> betas,
                                             double eps) {
    return new NPPM<X,UrnGMPY.Cargo<X>>(maxdepth,
                                       new UrnGMPY<X>(alphas,betas,maxdepth,base,eps),
                                       base, set);
  }
  
  
  /** Constructs a new NPPM process with given parameters.
    * <br>
    * Example parameter string: "<tt>t=b:d=5:a=0:b=0.5</tt>".
    * Meaning of parameters:<br>
    * <dl><dt><b>Parameters and values:</b></dt><dd><ul>
    *   <li>field: <b>d</b>, type: int, maximum context depth.</li>
    *   <li>field: <b>a</b>, type: double, strength parameter (alpha).</li>
    *   <li>field: <b>b</b>, type: double, discount parameter (beta).</li>
    *   <li>field: <b>t</b>, type: char. Values: <b>b</b> for blending,
    *                                    <b>u</b> for branch-blending,
    *                                    <b>x</b> for exclusions.</li>
    * </ul></dd></dl>
    * @param pars parameters in String form
    * @param base base distribution over symbols */
  public static <X> NPPM<X,?> createNew(String pars, Distribution<X> base, Iterable<X> set) {
    // defaults:
    char type = '?'; // blending
    double alpha = 0.0;
    double beta = 0.5;
    double epsilon = 0.05;
    ArrayList<Double> alphas = null;
    ArrayList<Double> betas = null;
    ArrayList<ArrayList<Double>> amatrix = null;
    ArrayList<ArrayList<Double>> bmatrix = null;
    int maxdepth = -1;
    // now parse arguments
    String[] s = pars.split(":");
    for (int k=0; k<s.length; k++) {
      int eq = s[k].indexOf('=');
      if (eq != -1) {
        String key = s[k].substring(0,eq);
        String val = s[k].substring(eq+1);
        if (key.equals("d")) {
          maxdepth = Integer.decode(val);
        } else
        if (key.equals("e")) {
          epsilon = Double.parseDouble(val);
        } else
        if (key.equals("a")) {
          // ALPHA parameters (concentrations)
          if (val.contains(",")) {
            // multiple, depth-dependent alphas
            String[] as = val.split(",");
            alphas = new ArrayList<Double>();
            for (int j=0; j<as.length; j++) {
              // strip off trailing space
              if (as[j].endsWith(" ")) {
                as[j] = as[j].substring(0,as[j].length()-1);
              }
              alphas.add(Double.parseDouble(as[j]));
            }
            if (maxdepth == -1) {
              maxdepth = as.length-1;
            }
          } else {
            // one single, global alpha
            alpha = Double.valueOf(val);
          }
        } else
        if (key.equals("b")) {
          // BETA parameters (discounts)
          if (val.contains(",")) {
            // multiple, depth-dependent betas
            String[] bs = val.split(",");
            betas = new ArrayList<Double>();
            for (int j=0; j<bs.length; j++) {
              // strip off trailing space
              if (bs[j].endsWith(" ")) {
                bs[j] = bs[j].substring(0,bs[j].length()-1);
              }
              betas.add(Double.parseDouble(bs[j]));
            }
            if (maxdepth == -1) {
              maxdepth = bs.length-1;
            }
          } else {
            // one single, global beta
            beta = Double.valueOf(val);
          }
        } else
        if (key.equals("t")) {
          if (val.length() == 1) {
            type = val.charAt(0);
          } else {
            System.err.println("Warning: NPPM: illegal value \""+val+"\" "
                               +"for parameter "+key+".");
          }
        } else
        if (key.equals("A")) {
          // alpha-matrix
          amatrix = new ArrayList<ArrayList<Double>>();
          String[] rows = val.split(";");
          for (int d=0; d<rows.length; d++) {
            String[] cells = rows[d].split(",");
            ArrayList<Double> ar = new ArrayList<Double>();
            for (int f=0; f<cells.length; f++) {
              double am = Double.parseDouble(cells[f]);
              ar.add(am);
            }
            amatrix.add(ar);
          }
          if (maxdepth == -1) {
            maxdepth = amatrix.size()-1;
          }
        } else
        if (key.equals("B")) {
          // beta-matrix
          bmatrix = new ArrayList<ArrayList<Double>>();
          String[] rows = val.split(";");
          for (int d=0; d<rows.length; d++) {
            String[] cells = rows[d].split(",");
            ArrayList<Double> br = new ArrayList<Double>();
            for (int f=0; f<cells.length; f++) {
              double bm = Double.parseDouble(cells[f]);
              br.add(bm);
            }
            bmatrix.add(br);
          }
          if (maxdepth == -1) {
            maxdepth = bmatrix.size()-1;
          }
        } else {
          System.err.println("Warning: NPPM: unknown parameter \""+s[k]+"\"");
        }
      }
    }
    if (maxdepth == -1) {
      System.err.println("Warning: NPPM: using default depth \"d=5\".");
      maxdepth = 5; // safe default
    }
    if (type == '?') {
      if (amatrix != null && bmatrix != null) {
        type = 'm';
      } else {
        type = 'b';
      }
    }
    // Now create and return a matching PPM
    switch (type) {
      case 'b': // blending
                if (betas == null && alphas == null) {
                  return newBlendingPPM(maxdepth, base, set, alpha, beta);
                } else {
                  return newDepthBlendingPPM(maxdepth, base, set,
                                             alphas, betas);
                }
      case 'u': // node-fanout-dependent blending
                if (betas == null && alphas == null) {
                  betas = new ArrayList<Double>();
                  betas.add(beta);
                  alphas = new ArrayList<Double>();
                  alphas.add(alpha);
                }
                return newBranchBlendingPPM(maxdepth, base,
                                            set, alphas, betas);
      case 'x': // exclusion
                return newExclusivePPM(maxdepth, base, set, alpha, beta);
      case 'm': // depth- and node-fanout-dependent blending
                return newMatrixBlendingPPM(maxdepth, base, set, amatrix, bmatrix);
      case 'g': // depth- and node-fanout-dependent blending + gradient-adj.
                return newGMatrixBlendingPPM(maxdepth, base, set, amatrix, bmatrix, epsilon);
      default:  System.err.println("Warning: NPPM: unknown urn type '"+type+"'.");
                System.err.println("               Try 'b', 'x', 'u' or 'm'.");
                return null;
    }
  }


  /** Trains this model on a single symbol. */
  public void learn(X x) {
    urn.learn(x);
    count++;
  }

  /** Trains this model on a sequence of symbols. */
  public void learn(Iterable<X> seq) {
    for (X x : seq) {
      learn(x);
    }
  }

  /** Returns additional information about the current state. */
  public String getStateInfo() {
    return urn.getStateInfo();
  }
  
  /** Returns the probability mass of a symbol in the
    * current context. */
  public double mass(X x) {
    return urn.mass(x,base);
  }
  
  /** Returns the log probability mass of a symbol in
    * the current context. */
  public double logMass(X x) {
    return Math.log(urn.mass(x,base));
  }

  /** Returns if this distribution is defined on a finite set of
    * elements. For PPM, this property is inherited from the base
    * distribution. */
  public boolean isFinite() {
    return base.isFinite();
  }

  public X sample(Random rnd) {
    X x = urn.sample(rnd,base,set);
    learn(x);
    return x;
  }
 
  /** Returns a least predicted element.
    * @see #sample(Random)
    * @see Samplers#leastMass(Mass,Iterable) */
  public X least() {
    X x = urn.least(base,set);
    learn(x);
    return x;
  }

  /** Returns a String representation of this process. */
  public String toString() {
    return "NPPM("+urn+") over "+base;
  }

  public void encode(X x, Encoder ec) {
    urn.encode(x,base,set,ec);
  }
  
  public void encode(X x, Collection<X> without, Encoder ec) {
    throw new UnsupportedOperationException();
  }
  
  public X decode(Decoder dc) {
    return urn.decode(base,set,dc);
  }
  
  public X decode(Collection<X> without, Decoder dc) {
    throw new UnsupportedOperationException();
  }
  
  public Distribution<X> getPredictiveDistribution() {
    throw new UnsupportedOperationException();
  }

  
  /** Computes gradients for this symbol sequence. */
  public ArrayList<Double> grade(Iterable<X> seq) {
    final String highlight = "\033[35m";
    final String neutral = "\033[m";
    double logp = 0.0;
    ArrayList<Double> grad = urn.createGradientList();
    for (X x : seq) {
      logp += Math.log(urn.mass(x,base));
      urn.addLogMassGradients(x,urn.getContext(),base,grad);
      learn(x);
    }
    double bits = logp/-Tools.LN2;
    System.err.println("  "+bits+" bits\t("
                       +highlight+bits/8.0+" bytes"+neutral+")");
    return grad;
  }
  
  /** Computes log score for this symbol sequence. */
  public double score(Iterable<X> seq) {
    double logp = 0.0;
    for (X x : seq) {
      logp += Math.log(urn.mass(x,base));
      learn(x);
    }
    double bits = logp/-Tools.LN2;
    //System.err.println("logp = "+logp+" nats, "+bits+" bits ("+bits/8.0+" bytes)");
    return logp;
  }
  
  /** Sends bits-per-symbol scores to the specified PrintStream. */
  public void printBPS(Iterable<X> seq, int k, PrintStream ps) {
    double logp = 0.0;
    int now = k;
    for (X x : seq) {
      if (now == 0) {
        double bits = logp/-Tools.LN2;
        ps.println(count+"\t"+(bits/count)+"\t"+(((long) (1000.0*bits))/1000.0));
        now = k;
      }
      logp += Math.log(urn.mass(x,base));
      learn(x);
      now--;
    }
    double bits = logp/-Tools.LN2;
    ps.println(count+"\t"+(bits/count)+"\t"+(((long) (1000.0*bits))/1000.0));
  }

  public static void delogitDiscounts(ArrayList<Double> a) {
    // get correct discounts
    for (int k=0; k<a.size(); k++) {
      double logit = a.get(k);
      double loglogitinv = -Tools.logSumExp(0,-logit);
      //double el = Math.exp(logit);
      // ( el/(1.0+el) );
      a.set(k, Math.exp(loglogitinv));
    }
  }
  
  public static void enlogitDiscounts(ArrayList<Double> a) {
    // get correct discounts
    for (int k=0; k<a.size(); k++) {
      double aa = a.get(k);
      if (aa == 1.0) {
        //a.set(k, Double.POSITIVE_INFINITY);
        a.set(k, 10000.0);
      } else
      if (aa == 0.0) {
        //a.set(k, Double.NEGATIVE_INFINITY);
        a.set(k, -10000.0);
      } else {
        double logit = Math.log(aa) - Math.log(1.0-aa);
        a.set(k, logit);
      }
    }
  }

  public static ArrayList<Double> encodeParameters(ArrayList<Double> pars) {
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

  public static Tuple<ArrayList<Double>,ArrayList<Double>>
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

  public static ArrayList<Double> decodeParameters(ArrayList<Double> pars) {
    Tuple<ArrayList<Double>,ArrayList<Double>> tuple = decodeAlphasBetas(pars);
    // extract lists
    ArrayList<Double> alphas = tuple.get0();
    ArrayList<Double> betas  = tuple.get1();
    // return the concatenation of both lists
    alphas.addAll(betas);
    return alphas;
  }
      
  public static <X> NPPM<X,UrnDCPY.Cargo<X>> getModel(
                                 ArrayList<Double> alphas,
                                 ArrayList<Double> betas,
                                 Distribution<X> base,
                                 Iterable<X> set,
                                 PPMTrie<X,UrnDCPY.Cargo<X>> ctx) {
    // create new PPM instance with these parameters
    UrnDCPY<X> urn = new UrnDCPY<X>(alphas, betas);
    NPPM<X,UrnDCPY.Cargo<X>> model = null;
    if (ctx != null) {
      urn.setContext(ctx.clone());
    }
    model = new NPPM<X,UrnDCPY.Cargo<X>>(alphas.size(),urn,base,set);
    return model;
  }
  
  public static <X> NPPM<X,UrnDCPY.Cargo<X>> getModel(
                               ArrayList<Double> pars,
                               Distribution<X> base,
                               Iterable<X> set,
                               PPMTrie<X,UrnDCPY.Cargo<X>> ctx) {
    // create new PPM instance with these parameters
    ArrayList<Double> alphas = new ArrayList<Double>();
    ArrayList<Double> betas = new ArrayList<Double>();
    int mid = pars.size()/2;
    for (int k=0; k<mid; k++) { alphas.add(pars.get(k)); }
    for (int k=mid; k<pars.size(); k++) { betas.add(pars.get(k)); }
    return getModel(alphas,betas,base,set,ctx);
  }

  
  public static <X,Y> ArrayList<Double> optimize(final PPMUrn<X,Y> urn,
                                                 final Iterable<X> seq,
                                                 final Distribution<X> base,
                                                 final Iterable<X> set,
                                                 ArrayList<Double> pars,
                                                 final int maxdepth,
                                                 final boolean check,
                                                 final boolean verbose,
                                                 final PPMTrie<X,Y> ctx) {
    MacOpt.MVG mvg = new MacOpt.MVG() {
      /** Helper function: created model from ab parameters. */
      protected NPPM<X,Y> getModel(ArrayList<Double> abs) {
        // create new urn with these parameters
        PPMUrn<X,Y> newurn = urn.createUrn(abs);
        if (ctx != null) { newurn.setContext(ctx.clone()); }
        // create new model with this urn
        return new NPPM<X,Y>(maxdepth, newurn, base, set);
      }
      /** Computes the gradients (in xy space, for given xy parameters). */
      public ArrayList<Double> getGradient(ArrayList<Double> xys) {
        // decode parameters and create a matching model
        ArrayList<Double> abs = urn.decodeParameters(xys);
        NPPM<X,Y> model = getModel(abs);
        System.out.println(model.urn.stringFromParameters(abs));
        // evaluate the gradients
        ArrayList<Double> grad = model.grade(seq);
        //if (verbose) {
          System.err.println("AB gradients: "+grad);
        //}
        // transform ab gradients to xy gradients
        grad = urn.encodeGradients(grad,abs);
          System.err.println("XY gradients: "+grad);
        return grad;
      }
      /** Computes the log score (for given xy parameters). */
      public double eval(ArrayList<Double> xys) {
        // decode parameters
        ArrayList<Double> abs = urn.decodeParameters(xys);
        NPPM<X,Y> model = getModel(abs);
        //System.err.println("MODEL = "+model);
        // evaluate
        double score = -model.score(seq);
        return score;
      }
    };
    // Let's start!
    // map the start parameters from ab to xy space
    System.err.println();
    System.err.println("original pars = "+pars);
      pars = urn.encodeParameters(pars);

    if (check) {
      System.err.println(" encoded pars = "+pars);
      ArrayList<Double> decoded = urn.decodeParameters(pars);
      System.err.println(" decoded pars = "+decoded);
      /* for (int j=0; j<decoded.size(); j++) {
        diff.add(original.get(j)-decoded.get(j));
      } */
    }
    MacOpt macopt = new MacOpt(mvg, pars, pars.size());
    if (verbose) {
      macopt.verbose = 3;
    } else {
      macopt.verbose = 1;
    }
    //macopt.tol=0.00001;
    macopt.tol=0.0001;
    // if (check) { macopt.checkgrad(pars, 0.0001, 0); }
    if (check) { macopt.checkgrad(pars, 0.00001, 0); }
    // if (check) { macopt.checkgrad(pars, 0.000001, 0); }
    // -----------------------------------------
    macopt.macopt(); // RUN THE OPTIMISER
    pars = macopt.p;
    // -----------------------------------------
    // decode the optimised parameters (from xy space to ab space)
    ArrayList<Double> abs = urn.decodeParameters(pars);
    // create new urn with these parameters
    PPMUrn<X,Y> newurn = urn.createUrn(abs);
    if (ctx != null) { newurn.setContext(ctx.clone()); }
    // create new model with this urn
    NPPM<X,Y> best = new NPPM<X,Y>(maxdepth,newurn,base,set);
    
    String pstr = newurn.stringFromParameters(abs);
    System.out.println("| MAXDEPTH: "+maxdepth);
    System.out.println("|  OPTIMAL: "+pstr.replaceAll("\n","\n|           "));
    System.out.println("|      URN: "+newurn);
    //System.out.println(" OPTIMAL: α="+alphas+" β="+betas);
    /*
    if (check) { macopt.checkgrad(pars, 0.0001, 0); }
    if (check) { macopt.checkgrad(pars, 0.00001, 0); }
    if (check) { macopt.checkgrad(pars, 0.000001, 0); }
    */
    // compute final score (again, conditional on supplied context)
    double score = -best.score(seq);
    double bits = score / Tools.LN2;
    System.out.println("|    SCORE: "+bits/8.0+" bytes, "+bits/best.count+" bps");
    System.err.println("|------------------------------------------------------------");
    // decode parameters back to normal space, and return them
    return abs;
  }
  
 
  /** Progressive optimisation, for a sequence of segments. */
  public static <X,Y> void progOptimize(
                                PPMUrn<X,Y> urn,
                                Iterable<Iterable<X>> seqs,
                                final Distribution<X> base,
                                final Iterable<X> set,
                                ArrayList<Double> pars,
                                final int maxdepth,
                                PPMTrie<X,Y> ctx) {
    // construct a new model (parameters don't matter much)
    PPMUrn<X,Y> newurn = urn.createUrn(pars);
    newurn.setContext(ctx);
    NPPM<X,Y> model = new NPPM<X,Y>(maxdepth,newurn,base,set);
    // optimise the parameters for each subsequence,
    // conditional on all preceding subsequences
    ArrayList<Double> parscopy = new ArrayList<Double>(pars);
    int segn = 0;
    for (Iterable<X> seq : seqs) {
      // optimize parameters for the next subsequence
      System.err.println("===================================================");
      System.err.println("Optimizing parameters for next subsequence...");
      System.err.println("SEGMENT: "+segn);
      //PPMTrie<X,UrnDCPY.Cargo<X>> ctx = model.urn.getContext();
      //System.err.println("CURRENT CONTEXT: "+ctx);
      optimize(urn, seq, base, set, parscopy, maxdepth, true, false, ctx);
      //System.err.println("CURRENT CONTEXT: "+ctx);
      // train the master model
      System.err.println("Adding subsequence to training data...");
      model.learn(seq);
      segn++;
    }
    System.err.println("===================================================");
    System.err.println("TOTAL SEGMENTS: "+segn);
  }




  /** Computes error bars at a given optimal point.
    * @return error bars (upper and lower, for every parameter) */
  public static <X> ArrayList<Double> errorBars(ArrayList<Double> pars,
                                   ArrayList<Double> limits,
                                   double tol,
                                   Distribution<X> base,
                                   Iterable<X> set,
                                   PPMTrie<X,UrnDCPY.Cargo<X>> ctx,
                                   Iterable<X> iseq) {
    ArrayList<Double> errors = new ArrayList<Double>();
    // verbosity
    int verbose = 2;
    // evaluate base line score at optimal location (pars)
    NPPM<X,UrnDCPY.Cargo<X>> model = getModel(pars,base,set,ctx);
    double logp = model.score(iseq);
    long size = model.count;
    double optbps = -logp/(size*Tools.LN2);
    final int maxit = 50;
    // now take each parameter in turn...
    for (int k=0; k<pars.size(); k++) {
      double orig = pars.get(k);
      double upper_limit = limits.get(2*k);
      double lower_limit = limits.get(2*k+1);
      for (boolean upward : new boolean[] { true, false }) {
        double newbps = Double.POSITIVE_INFINITY;
        double mid = Double.NEGATIVE_INFINITY;
        double a = 0.0;
        double z = 0.0;
        if (upward) {
          // searching for upper margin
          pars.set(k,upper_limit);
          model = getModel(pars,base,set,ctx);
          logp = model.score(iseq);
          newbps = -logp/(model.count*Tools.LN2);
          a = orig;
          z = upper_limit;
          mid = Double.NEGATIVE_INFINITY;
        } else {
          // searching for lower margin
          pars.set(k,lower_limit);
          model = getModel(pars,base,set,ctx);
          logp = model.score(iseq);
          newbps = -logp/(model.count*Tools.LN2);
          a = lower_limit;
          z = orig;
          mid = Double.NEGATIVE_INFINITY;
        }
        System.err.println("Searching between "+a+" and "+z+":");
        double dev = (newbps - optbps) / optbps;
        System.err.println("optbps="+optbps+", newbps="+newbps+", dev="+dev);
        if (dev < tol) {
          System.err.println("dev="+dev+" is smaller than tol="+tol);
          if (upward) {
            //errors.add(upper_limit);
            errors.add(Double.POSITIVE_INFINITY);
          } else {
            //errors.add(lower_limit);
            errors.add(Double.NEGATIVE_INFINITY);
          }
        } else {
          while (Math.abs(dev - tol) > 0.000001) {
            mid = a + (z-a) / 2.0;
            pars.set(k,mid);
            model = getModel(pars,base,set,ctx);
            logp = model.score(iseq);
            newbps = -logp/(model.count*Tools.LN2);
            //System.out.println(" err["+k+"]="+(orig+step)+"  "+newbps+"  "+dev);
            dev = (newbps - optbps) / optbps;
            System.err.println("optbps="+optbps+", newbps="+newbps+", dev="+dev);
            if (dev > tol) {
              if (upward) { z = mid; } else { a = mid; }
              if (verbose > 1) { System.err.print(">"); }
            } else
            if (dev < tol) {
              if (upward) { a = mid; } else { z = mid; }
              if (verbose > 1) { System.err.print("<"); }
            }
            if (Math.abs(a-z) < 0.000000000001) {
              if (verbose > 1) { System.err.println("#"); }
              break;
            }
          }
          errors.add(mid);
          if (verbose > 1) { System.err.println("!"); }
          pars.set(k,orig);
          if (verbose > 0) {
            System.err.println();
            System.out.println(" OPT["+k+"]="+orig+"  "+optbps);
            System.out.println(" ERR["+k+",+]="+(mid)+"  "+newbps);
            System.err.println();
          }
        }
      }
    }
    return errors;
  }


  public static String bitsToString(double bits) {
    return ((int)(1000*bits))/1000.0+" bits"
           +"  ("+((int) Math.ceil(bits/8))+" bytes)";
  }

  private static String leadzero(String s) {
    return (s.length() == 1 ? "0"+s : s);
  }


  /** Implements basic command line functionality. */
  @SuppressWarnings("unchecked")
  public static void main(String[] args) throws FileNotFoundException {
    String self = "java NPPM";
    /* Input modes. */
    final int MODE_CHAR = 1;
    final int MODE_INT  = 2;
    int mode = 0;
    /* Input alphabet and distribution. */
    Iterable<Character> cset = null;
    Iterable<Integer>   bset = null;
    Distribution<Character> cbase = null;
    Distribution<Integer>   bbase = null;
    Integer beof = null;

    /* Evaluation string */
    String text = "";
    /* Description of input */
    String idsc = "none";
    /* Description of training */
    String tdsc = "none";
    char urntype = 'b'; // b,u,x [see createNew(...)]
    double eps = 0.05;
    /* Training sequence */
    Iterable<Byte>      btseq = null;
    Iterable<Character> ctseq = null;
    /* Evaluation sequence */
    Iterable<Byte>      biseq = null;
    Iterable<Character> ciseq = null;
    /* Context after training */
    PPMTrie<Character,UrnDCPY.Cargo<Character>> ctctx = null;
    PPMTrie<Integer,UrnDCPY.Cargo<Integer>>     btctx = null;
    PPMTrie tctx = null;
    // PPM parameters
    int maxdepth = 3;
    // DOT output
    String dotfnm = null;
    // base distribution + iterable set
    /*
    UniformChar ascii = UniformChar.ascii();
    Distribution<Character> base = ascii;
    Iterable<Character> set = ascii;
    */

    // actions and options
    int msglength = 0;
    int worstlength = 0;
    boolean html = false;
    boolean optimise = false;
    boolean bps = false;
    int bps_step = 1000;
    ArrayList<Double> alphas = new ArrayList<Double>();
    ArrayList<Double> betas  = new ArrayList<Double>();
    ArrayList<ArrayList<Double>> amatrix = null;
    ArrayList<ArrayList<Double>> bmatrix = null;
    ArrayList<Double> etol = null;
    ArrayList<String> files = null;
    String argtarget = null;
    // parse commandline arguments:
    for (int a=0; a<args.length; a++) {
      if (args[a].equals("-s")) { // input string (evaluation)
        argtarget=args[a];
      } else
      if (args[a].equals("-f")) { // input files (evaluation)
        argtarget=args[a];
      } else
      if (args[a].equals("-t")) { // input files (training)
        argtarget=args[a];
      } else
      if (args[a].equals("-d")) { // set depth
        a++;
        maxdepth = Integer.decode(args[a]);
      } else
      if (args[a].equals("-eps")) { // set epsilon (base step size)
        a++;
        eps = Double.parseDouble(args[a]);
      } else
      if (args[a].equals("-bps")) {
        a++;
        bps = true;
        bps_step = Integer.decode(args[a]);
      } else
      if (args[a].equals("-a")) {
        argtarget=args[a];
      } else
      if (args[a].equals("-b")) {
        argtarget=args[a];
      } else
      if (args[a].equals("-B")) {
        argtarget=args[a];
      } else
      if (args[a].equals("-A")) {
        argtarget=args[a];
      } else
      if (args[a].equals("-eb")) {
        argtarget=args[a];
      } else
      if (args[a].equals("-g")) {
        a++;
        msglength = Integer.decode(args[a]);
      } else
      if (args[a].equals("-w")) {
        a++;
        worstlength = Integer.decode(args[a]);
      } else
      if (args[a].equals("-h")) {
        html = true;
      } else
      if (args[a].equals("-opt")) {
        optimise = true;
      } else
      if (args[a].equals("-optm")) {
        optimise = true;
        files = new ArrayList<String>();
        a++;
        while (a < args.length && !args[a].startsWith("-")) {
          files.add(args[a]);
          a++;
        }
      } else
      if (args[a].equals("-u")) { // custom uniform distribution
        a++;
        Discrete<Character> dcd =
            new Discrete<Character>(IOTools.charSequenceFromString(args[a]));
        cbase = dcd;
        cset = dcd;
        mode = MODE_CHAR;
      } else
      if (args[a].equals("-7bit")) { // 7-bit ASCII
        UniformChar dcd = FileGen.uniform7Bit();
        cbase = dcd;
        cset = dcd;
        mode = MODE_CHAR;
      } else
      if (args[a].equals("-257")) { // 257 selected Unicode characters
        ArrayList<Character> cs = new ArrayList<Character>();
        for (Character c : FileGen.uniform7Bit()) { cs.add(c); }
        for (Character c : FileGen.uniformBraille()) {
          if (cs.size() < 257) { cs.add(c); } else { break; }
        }
        Discrete<Character> dcd = new Discrete<Character>(cs);
        cbase = dcd;
        cset = dcd;
        mode = MODE_CHAR;
      } else
      if (args[a].equals("-8bit")) { // 8-bit bytes + EOF
        UniformChar dcd = FileGen.uniform7Bit();
        bbase = ByteCompressor.base;
        bset = ByteCompressor.base;
        beof = ByteCompressor.eof;
        mode = MODE_INT;
      } else
      if (args[a].equals("-m")) {  // model / urn type
        a++;
        if (args[a].length() == 1) {
          urntype = args[a].charAt(0);
        } else {
          System.err.println(self+": Urn type \""+args[a]+"\" should be one letter.");
          return;
        }
      } else
      if (args[a].equals("-dt")) {
        a++;
        dotfnm = args[a];
      } else
      if (args[a].equals("-h") || args[a].equals("--help")) {
        System.out.println("Usage: "+self+" [options]");
        System.out.println("\t -h, --help\t a helpful message not unlike this one");
        System.out.println("\t -a alpha  \t alpha values (e.g. 20.0:1.0:0.5)");
        System.out.println("\t -A matrix \t alpha values (e.g. 10,1,0.5:20,2,0.7)");
        System.out.println("\t -b beta   \t beta values (e.g. 0.05:0.4:0.7)");
        System.out.println("\t -B matrix \t beta values (e.g. 0.01,0.2:0.03,0.5)");
        System.out.println("\t -d N      \t maximum tree depth (e.g. "+maxdepth+")");
        System.out.println("\t -eps r    \t base step size (e.g. "+eps+")");
        System.out.println("\t -u STRING \t custom alphabet");
        //System.out.println("\t -6bit     \t uniform distribution over 64 symbols");
        System.out.println("\t -7bit     \t alphabet = 128 ASCII symbols");
        System.out.println("\t -8bit     \t alphabet = 256 byte values + EOF");
        System.out.println("\t -257      \t alphabet = 257 Unicode symbols");
        System.out.println("\t -m URN    \t set model / urn type (e.g. '"+urntype+"')");
        System.out.println("\t -s STRING \t evaluate model on STRING");
        System.out.println("\t -f FILE   \t evaluate model on FILE");
        System.out.println("\t -t FILE   \t pre-train model on FILE");
        System.out.println("\t -g N      \t get N symbols from the trained model");
        System.out.println("\t -w N      \t get N most unlike symbols from the trained model");
        System.out.println("\t -h        \t output HTML format instead of plain text (for -g)");
        System.out.println("\t -bps K    \t print bits/symbol (for every K symbols)");
        System.out.println("\t -opt      \t optimise parameters (expensive)");
        System.out.println("\t -optm ... \t optimise parameters on seq delta chunks");
        System.out.println("\t -dt FILE  \t write final tree to FILE (in dot-format)");
        System.out.println("\t -eb 0.01  \t get error bars for parameters (tolerance 0.01)");
        return;
      } else
      if (args[a].startsWith("-") && !Character.isDigit(args[a].charAt(1))) {
        System.err.println(self+": Unknown option '"+args[a]+"'.  '--help' prints available options.");
        return;
      } else
      if (argtarget != null) {
        // alphas
        if (argtarget.equals("-a")) {
          String[] ss = args[a].split(":");
          for (int k=0; k<ss.length; k++) {
            // strip off trailing comma
            if (ss[k].endsWith(",")) {
              ss[k] = ss[k].substring(0,ss[k].length()-1);
            }
            // convert and add
            alphas.add(Double.parseDouble(ss[k]));
          }
          maxdepth = alphas.size()-1;
        } else
        // betas
        if (argtarget.equals("-b")) {
          String[] ss = args[a].split(":");
          for (int k=0; k<ss.length; k++) {
            // strip off trailing comma
            if (ss[k].endsWith(",")) {
              ss[k] = ss[k].substring(0,ss[k].length()-1);
            }
            // convert and add
            betas.add(Double.parseDouble(ss[k]));
          }
          maxdepth = betas.size()-1;
        } else
        // alpha-matrix
        if (argtarget.equals("-A")) {
          amatrix = new ArrayList<ArrayList<Double>>();
          String[] rows = args[a].split(":");
          for (int d=0; d<rows.length; d++) {
            String[] cells = rows[d].split(",");
            ArrayList<Double> ar = new ArrayList<Double>();
            for (int f=0; f<cells.length; f++) {
              // strip off trailing comma
              //if (ss[k].endsWith(",")) {
              //  ss[k] = ss[k].substring(0,ss[k].length()-1);
              //}
              // convert and add
              double am = Double.parseDouble(cells[f]);
              ar.add(am);
            }
            amatrix.add(ar);
          }
          maxdepth = amatrix.size()-1;
        } else
        // beta-matrix
        if (argtarget.equals("-B")) {
          bmatrix = new ArrayList<ArrayList<Double>>();
          String[] rows = args[a].split(":");
          for (int d=0; d<rows.length; d++) {
            String[] cells = rows[d].split(",");
            ArrayList<Double> br = new ArrayList<Double>();
            for (int f=0; f<cells.length; f++) {
              // strip off trailing comma
              //if (ss[k].endsWith(",")) {
              //  ss[k] = ss[k].substring(0,ss[k].length()-1);
              //}
              // convert and add
              double b = Double.parseDouble(cells[f]);
              if (b > 1.0) {
                System.err.println(self+": Warning: beta parameter out of range: "+b);
              }
              br.add(b);
            }
            bmatrix.add(br);
          }
          maxdepth = bmatrix.size()-1;
        } else
        // input files for training sequence
        if (argtarget.equals("-t")) {
          String tfnm = args[a];
          if (mode == MODE_CHAR) {
            if (ctseq == null) {
              // set training sequence to file
              tdsc = "<"+tfnm+">";
              ctseq = IOTools.charSequenceFromFile(tfnm);
            } else {
              // append file to the training sequence
              tdsc = tdsc + " <"+tfnm+">";
              ctseq = IOTools.concat(ctseq,IOTools.charSequenceFromFile(tfnm));
            }
          } else
          if (mode == MODE_INT) {
            if (btseq == null) {
              // set training sequence to file
              tdsc = "<"+tfnm+">";
              btseq = IOTools.byteSequenceFromFile(tfnm);
            } else {
              // append file to the training sequence
              tdsc = tdsc + " <"+tfnm+">";
              btseq = IOTools.concat(btseq,IOTools.byteSequenceFromFile(tfnm));
            }
          } else {
            System.err.println(self+": Error: no input mode selected (choose an alphabet!)");
            return;
          }
        } else
        // input files for evaluation sequence
        if (argtarget.equals("-f")) {
          String ifnm = args[a];
          if (mode == MODE_CHAR) {
            if (ciseq == null) {
              // set evaluation sequence to file
              idsc  = "<"+ifnm+">";
              ciseq = IOTools.charSequenceFromFile(ifnm);
            } else {
              // append file to the evaluation sequence
              idsc  = idsc + " <"+ifnm+">";
              ciseq = IOTools.concat(ciseq,IOTools.charSequenceFromFile(ifnm));
            }
          } else
          if (mode == MODE_INT) {
            if (biseq == null) {
              // set evaluation sequence to file
              idsc = "<"+ifnm+">";
              biseq = IOTools.byteSequenceFromFile(ifnm);
            } else {
              // append file to the evaluation sequence
              idsc = idsc + " <"+ifnm+">";
              biseq = IOTools.concat(biseq,IOTools.byteSequenceFromFile(ifnm));
            }
          } else {
            System.err.println(self+": Error: no input mode selected (choose an alphabet!)");
            return;
          }
        } else
        if (argtarget.equals("-s")) {
          if (mode == MODE_INT) {
            System.err.println(self+": option '-s' only works with char alphabets right now");
            return;
          } else {
            mode = MODE_CHAR;
            // set input sequence for evaluation, from string
            if (ciseq == null) {
              text = args[a];
              idsc = "\""+args[a]+"\"";
              ciseq = IOTools.charSequenceFromString(args[a]);
            } else {
              // append string to the evaluation sequence
              idsc = idsc + " \""+args[a]+"\"";
              ciseq = IOTools.concat(ciseq,IOTools.charSequenceFromString(args[a]));
            }
          }
        } else
        // error bar tolerances
        if (argtarget.equals("-eb")) {
          if (etol == null) {
            etol = new ArrayList<Double>();
          }
          String[] ss = args[a].split(":");
          for (int k=0; k<ss.length; k++) {
            etol.add(Double.parseDouble(ss[k]));
          }
        } else {
          System.err.println(self+": Unknown argument '"+args[a]+"'.  Try '--help' for advice.");
        }
      } else {
        System.err.println(self+": Unknown argument '"+args[a]+"'.  Try '--help' for advice.");
        return;
      }

    }
    
    if (mode == 0) {
      System.err.println(self+": Warning: no valid input alphabet selected (mode="+mode+").");
    }
    /* set up PPM model instance */
    NPPM ppm = null;
    switch (urntype) {
      case 'b': // blending
                System.err.println(self+": URN TYPE = BLENDING (depth-dependent parameters).");
                switch (mode) {
                 case MODE_CHAR:
                    ppm = newDepthBlendingPPM(maxdepth, cbase, cset, alphas, betas);
                    break;
                 case MODE_INT:
                    ppm = newDepthBlendingPPM(maxdepth, bbase, bset, alphas, betas);
                    break;
                }
                break;
      case 'u': // node-fanout-dependent blending
                System.err.println(self+": URN TYPE = BLENDING (fanout-dependent parameters).");
                switch (mode) {
                 case MODE_CHAR:
                   ppm = newBranchBlendingPPM(maxdepth, cbase, cset, alphas, betas);
                   break;
                 case MODE_INT:
                   ppm = newBranchBlendingPPM(maxdepth, bbase, bset, alphas, betas);
                   break;
                }
                break;
      case 'm': // depth- and node-fanout-dependent blending
                System.err.println(self+": URN TYPE = BLENDING (depth + fanout-dependent).");
                switch (mode) {
                 case MODE_CHAR:
                   ppm = newMatrixBlendingPPM(maxdepth, cbase, cset, amatrix, bmatrix);
                   break;
                 case MODE_INT:
                   ppm = newMatrixBlendingPPM(maxdepth, bbase, bset, amatrix, bmatrix);
                   break;
                }
                break;
      case 'g': // depth- and node-fanout-dependent blending + AUTO-ADJUST
                System.err.println(self+": URN TYPE = BLENDING (depth + fanout-dependent + auto-adj).");
                switch (mode) {
                 case MODE_CHAR:
                   ppm = newGMatrixBlendingPPM(maxdepth, cbase, cset,
                                               amatrix, bmatrix, eps);
                   break;
                 case MODE_INT:
                   ppm = newGMatrixBlendingPPM(maxdepth, bbase, bset,
                                               amatrix, bmatrix, eps);
                   break;
                }
                break;
      case 'x': // exclusion
                System.err.println(self+": URN TYPE = EXCLUSION / escape mechanism.");
                switch (mode) {
                 case MODE_CHAR:
                   if (alphas.size() > 1 || betas.size() > 1) {
                     System.err.println(self+": Warning: too many alpha / beta parameters specified.");
                     ppm = newExclusivePPM(maxdepth, cbase, cset, alphas.get(0), betas.get(0));
                   } else
                   if (alphas.size() == 1 || betas.size() == 1) {
                     ppm = newExclusivePPM(maxdepth, cbase, cset, alphas.get(0), betas.get(0));
                   } else {
                     System.err.println(self+": using PPMD settings (a=0, b=0.5).");
                     ppm = newExclusivePPM(maxdepth, cbase, cset, 0.0, 0.5); // PPMD default
                   }
                   break;
                 case MODE_INT:
                   if (alphas.size() > 1 || betas.size() > 1) {
                     System.err.println(self+": Warning: too many alpha / beta parameters specified.");
                     ppm = newExclusivePPM(maxdepth, bbase, bset, alphas.get(0), betas.get(0));
                   } else
                   if (alphas.size() == 1 || betas.size() == 1) {
                     ppm = newExclusivePPM(maxdepth, bbase, bset, alphas.get(0), betas.get(0));
                   } else {
                     System.err.println(self+": using PPMD settings (a=0, b=0.5).");
                     ppm = newExclusivePPM(maxdepth, bbase, bset, 0.0, 0.5); // PPMD default
                   }
                }
                break;
      default:  System.err.println(self+": Warning: unknown urn type '"+urntype+"'. Try one of {b,m,u,x}.");
    }
    //NPPM<Character,UrnUPY.Cargo<Character>> ppm =
    //     NPPM.newBranchBlendingPPM(maxdepth, base, set, alphas, betas);
    //NPPM<Character,UrnDCPY.Cargo<Character>> ppm =
    //     NPPM.newDepthBlendingPPM(maxdepth, base, set, alphas, betas);
    /* Get urn instance. */
    //PPMUrn<Character,UrnDCPY.Cargo<Character>> urn = ppm.urn;
    PPMUrn urn = ppm.urn;
    //PPMUrn<Character,UrnUPY.Cargo<Character>> urn = ppm.urn;


    // attempt to urn parameters
    ArrayList<Double> pars = null;
    try {
      pars = urn.getCurrentParameters();
    }
    catch (UnsupportedOperationException e) {
      System.err.println(self+": Warning: urn does not support 'getCurrentParameters'.");
    }

    /* Print what happened. */
    System.err.println("Created "+ppm);
    
    /* Pre-training */
    if (btseq != null) {
      System.err.println("Pre-training with: "+tdsc+" (byte mode)");
      Iterable<Integer> intseq = IOTools.map(btseq,new Function<Byte,Integer>() {
        public Integer eval(Byte b) { return ByteCompressor.byte2int(b); }
      });
      ppm.learn(intseq);
      btctx = ppm.urn.getContext().clone();
      tctx = btctx;
    }
    if (ctseq != null) {
      System.err.println("Pre-training with: "+tdsc+" (char mode)");
      ppm.learn(ctseq);
      ctctx = ppm.urn.getContext().clone();
      tctx = ctctx;
    }

    if (optimise) {
      int pardepth = pars.size();
      System.err.println("OPTIMIZER");
      System.err.println("| Starting conditions:");
      System.err.println("|    maxdepth:   "+maxdepth);
      String pstr = urn.stringFromParameters(pars);
      System.err.println("|    parameters: "+pstr.replaceAll("\n","\n|                "));
      System.err.println("|    components: "+pardepth);
      if (files != null) {
        System.err.println("| Files: "+files);
        System.err.println("| OPERATION CURRENTLY NOT SUPPORTED.");
        /*
        System.err.print("Optimizing parameters for each subsequence... ");
        ArrayList<Iterable<Character>> seqs
                               = new ArrayList<Iterable<Character>>();
        for (String fnm : files) {
          Iterable<Character> seq = IOTools.charSequenceFromFile(fnm);
          seqs.add(seq);
        }
        //progOptimize(urn, seqs, base, set, pars, maxdepth, tctx);
        System.err.println("Complete.");
        */
      } else
      if (mode == MODE_CHAR && ciseq != null) {
        /*
        System.err.print("CHECKS: ");
        ArrayList<Double> grad = ppm.grade(tseq);
        System.err.println("Gradients: "+grad);
        */
        //System.err.print("Training... ");
        System.err.print("Optimizing parameters... ");
        pars = optimize(urn, ciseq, cbase, cset, pars, maxdepth,
                        true, false, ctctx);
        //System.out.println("OPT: "+pars);
        /*
        ArrayList<Double> grad = ppm.grade(tseq);
        System.err.println("Gradients: "+grad);
        ppm.learn(tseq);
        System.err.println("done.");
        System.err.println("Symbols in training: "+ppm.count);
        */
      } else
      if (mode == MODE_INT) {
        System.err.print("Optimizing parameters... ");
        /* Convert byte sequence to integer sequence. */
        Iterable<Integer> iniseq = IOTools.map(biseq,new Function<Byte,Integer>() {
          public Integer eval(Byte b) { return ByteCompressor.byte2int(b); }
        });
        pars = optimize(urn, iniseq, bbase, bset, pars, maxdepth,
                        true, false, btctx);
      } else {
        System.err.println(self+": unsupported mode ("+mode+").");
      }
    } else
    if (bps) {
      if (biseq != null || ciseq != null) {
        System.out.println("# "+idsc);
        switch (mode) {
          case MODE_CHAR:
             System.out.println("# position, bits/symbol");
             ppm.printBPS(ciseq,bps_step,System.out);
             break;
          case MODE_INT:
             System.out.println("# position, bits/byte");
             ppm.printBPS(biseq,bps_step,System.out);
             break;
        }
      } else {
        System.err.println("No input sequence specified.");
      }
    } else {
      //ppm.learn(IOTools.charSequenceFromString(text));
      //ppm.learn(IOTools.charSequenceFromFile("training.txt"));
      long pre = ppm.count;
      double logp = 0.0;
      if (mode == MODE_CHAR) {
        if (ciseq != null) {
          logp = ppm.score(ciseq);
        } else {
          System.err.println("No char input sequence found.");
        }
      } else
      if (mode == MODE_INT) {
        if (biseq != null) {
          Iterable<Integer> iniseq = IOTools.map(biseq,new Function<Byte,Integer>() {
            public Integer eval(Byte b) { return ByteCompressor.byte2int(b); }
          });
          logp = ppm.score(iniseq);
        } else {
          System.err.println("No byte input sequence found.");
        }
      } else {
        System.err.println(self+": unknown input mode (mode="+mode+")");
      }
      long run = ppm.count - pre;
      System.err.println();
      System.err.println("-------------------------------------------------------------");
      System.err.println("Model instance    : "+ppm);
      if (mode == MODE_CHAR) {
        System.err.println("Data unit type    : char");
        System.err.println("Training sequence : "+tdsc+" ("+pre+" symbols)");
        System.err.println("Scoring sequence  : "+idsc+" ("+run+" symbols)");
        System.err.println("Total symbol count: "+ppm.count);
      } else
      if (mode == MODE_INT) {
        System.err.println("Data unit type    : byte");
        System.err.println("Training sequence : "+tdsc+" ("+pre+" bytes)");
        System.err.println("Scoring sequence  : "+idsc+" ("+run+" bytes)");
        System.err.println("Total symbol count: "+ppm.count);
      } else {
        System.err.println("Data unit type    : unknown ("+mode+")");
      }
      System.err.println("Sequence score [e]: "+(-logp)+" nats");
      System.err.println("Sequence score [2]: "+(-logp/Tools.LN2)+" bits");
      System.err.println("Sequence score    : "+(-logp/(8.0*Tools.LN2))+" bytes");
      System.err.println("Bits per symbol   : "+(-logp/(run*Tools.LN2))+" bps");
    }
    // compute error bars, if requested
    if (etol != null) {
      for (Double tol : etol) {
        ArrayList<Double> errors = null;
        // compute limits
        ArrayList<Double> limits = new ArrayList<Double>();
        int half=pars.size()/2;
        for (int k=0; k<half; k++) {
          // alphas first
          limits.add(1000.0);           // upper limit
          limits.add(pars.get(k+half)); // lower limit = -beta[k]
        }
        for (int k=half; k<pars.size(); k++) {
          // alphas first
          limits.add(1.0); // upper limit = 1
          double alpha = pars.get(k-half);
          if (alpha > 0) {
            limits.add(0.0);    // lower limit = 0
          } else {
            limits.add(-alpha); // lower limit = -alpha[k]
          }
        }
        if (mode == MODE_CHAR) {
          errors = errorBars(pars, limits, tol, cbase, cset, tctx, ciseq);
        //} else
        //if (mode == MODE_INT) {
        //  errors = errorBars(pars, limits, tol, bbase, bset, tctx, biseq);
        } else {
          System.err.println(self+": error bars not supported in mode "+mode);
        }
        System.out.println("Error bars ["+tol+"]: "+errors);
        System.out.println("ERRORS ["+tol+"]: ");
        for (int k=0; k<errors.size(); k++) {
          double eb = errors.get(k);
          String es = Double.isInfinite(eb) ? "?" : ""+eb;
          System.out.print("\t"+es);
        }
        System.out.println();
      }
    }

    //System.err.println("p(x | {}) = "+ppm.root.getDist(ppm.base));

    //System.out.println(ppm.urn.toDot(ppm.root));
    //System.err.println(ppm.urn.getContext().toDot());
    //System.err.println(((UrnDPY<Character>) ppm.urn).toDot(ppm.root));
    
    long trainingcount = ppm.count;

    /* Worst-case sequence generator. */
    if (worstlength > 0) {
      if (mode == MODE_CHAR) {
        System.err.println("-------------------------------------------------------------");
        while (worstlength > 0) {
          System.out.print(ppm.least());
          worstlength--;
        }
        System.err.println();
        System.err.println("-------------------------------------------------------------");
      } else {
        System.err.println("Adversarial generation not supported for mode="+mode+".");
      }
    }
    /* Random sampling. */
    if (msglength > 0) {
      if (mode == MODE_CHAR) {
        Random rnd = new Random();
        System.err.println("-------------------------------------------------------------");
        while (msglength > 0) {
          System.out.print(ppm.sample(rnd));
          msglength--;
        }
        System.err.println();
        System.err.println("-------------------------------------------------------------");
      } else {
        System.err.println("Random generation not supported for mode="+mode+".");
      }
    }
  }

}

