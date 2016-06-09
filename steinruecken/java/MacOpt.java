/* $Id$ */
package uk.ac.cam.eng.ml.tcs27.compression;

import java.util.ArrayList;

/** Conjugate gradient multivariate function optimizer.
  * This is a recoded JAVA implementation of MacOpt (MacKay 2001). */
public class MacOpt {

  /** Dimension. */
  int dim = 0;
  
  
  /** Interface for multivariate function gradient evaluation. */
  interface MVG {
    /** Evaluates the gradient of a multivariate function at
      * a given point. */
    public ArrayList<Double> getGradient(ArrayList<Double> point);

    /** Evaluates the multivariate function at a given point;
      * optional. Feel free to throw UnsupportedOperationException. */
    public double eval(ArrayList<Double> point);
  }


  /** The RMS gradient for linmin. */
  double gtyp = 0.0;

  /** Tolerance. */
  double tol = 0.001;


  /* Operational variables: */

  /** Defines the role of tol -- alternative is end_on_small_grad. */
  boolean end_if_small_step = false;

  /** Whether to restart macopt - fresh conjugate gradient directions. */
  int restart = 0;

  /** Verbosity. */
  int verbose = 3;

  /** Rich operating mode. */
  boolean rich = false;
  
  /** Vectors. */
  ArrayList<Double> g, h;

  /** Current line direction. */
  ArrayList<Double> xi;
  ArrayList<Double> pt;
  /** Scratch gradients. */
  ArrayList<Double> gx;
  /** Used by linmin and macprod. */
  ArrayList<Double> gy;
  /** Point (current guess of minimum). */
  ArrayList<Double> p;

  /** Temporary number shared between functions. */
  double tmpd = 0.0;

  /* Probably not worth fiddling with: */

  /** If gradient is less than this, we definitely stop, even
    * if we are not using gradient tolerance. */
  double grad_tol_tiny = 1e-16;

  /** If step is less than this, we stop, even if we are not using
    * a step tolerance. */
  double step_tol_tiny = 0.0;
  
  /** Maximum number of macopt iterations. */
  int itmax = 150;

  /** Number of iterations after which rich is set to true. */
  int itrich = 80;

  /** Maximal number of linmin iterations. */
  double linmin_maxits = 20;

  /** Typical distance in parameter space at which line minimum
    * is expected: default value, used when a reset is needed. */
  double lastx_default = 0.001;

  /** Keeps track of typical step length. */
  double lastx = lastx_default;

  /* Do not change these settings unless you really mean it: */

  /** Factor for growing the interval. */
  double linmin_g1 = 2.0;

  /** Factor for growing the interval. */
  double linmin_g2 = 1.25;

  /** Factor for shrinking the interval. */
  double linmin_g3 = 0.5;



  MVG mvg = null;


  public MacOpt(MVG mvg, ArrayList<Double> initialp, int n) {
    this.dim = n;
    this.p = initialp;
    this.mvg = mvg;
    assert (p.size() == dim);
    g = initVector();
    h = initVector();
    xi = initVector();
    pt = initVector(); /* scratch vector for macprod */
    gx = initVector(); /* scratch gradients */
    gy = initVector(); /* used by linmin and macprod */
  }

  /** Creates a new vector of correct dimension,
    * with all components initialised to 0. */
  protected ArrayList<Double> initVector() {
    ArrayList<Double> a = new ArrayList<Double>();
    for (int k=0; k<dim; k++) {
      a.add(0.0);
    }
    return a;
  }

  public void macopt() {
    double gg;
    double dgg;
    double gam;
    boolean end_if_small_grad = !end_if_small_step;
    double step;
    xi = mvg.getGradient(p);
    macoptRestart(1);
    // main loop
    for (int its = 0; its < itmax; its++) {
      if (its > itrich) {
        // force set rich to true after a certain number of
        // iterations
        rich = true;
      }
      gg = 0.0;
      for (int j=0; j<dim; j++) {
        double gj = g.get(j);
        gg += gj * gj;
      }
      gtyp = Math.sqrt(gg / (double) dim);

      if (verbose > 0) {
        System.err.println("it "+its+" of "+itmax+": gg="+gg+", tol="+tol);
      }

      if ((end_if_small_grad && gg <= tol) || gg <= grad_tol_tiny) {
        if (verbose > 0) {
          System.err.println("Gradient: "+gg);
          System.err.println("| End condition reached. Stopping.");
        }
        return;
      }

      if (verbose > 1) {
        System.err.println();
        System.err.println("-------------------------------------------------------------");
        System.err.println("STARTING LINE SEARCH...");
        System.err.println("DIRECTION: "+xi);
      } else
      if (verbose == 1) {
        System.err.println("  --");
      }
      step = maclinmin();
      if (restart > 0) {
        if (verbose > 1) { System.err.print(" (step "+step+")"); }
        if (verbose > 0) { System.err.println(); }
      }

      if ((end_if_small_step && step <= tol) || step <= step_tol_tiny) {
        System.err.println("| Step tolerance reached. Stopping.");
        return;
      }

      if (rich || restart > 0) {
        xi = mvg.getGradient(p);
      }

      if (restart > 0) {
        if (verbose > 0) { System.err.println("Restarting macopt"); }
        macoptRestart(0);
      } else {
        dgg = 0.0;
        for (int j=0; j<dim; j++) {
          double xij = xi.get(j);
          dgg += (xij + g.get(j)) * xij;
        }
        gam = dgg / gg;

        tmpd = 0.0;
        for (int j=0; j<dim; j++) {
          g.set(j,-xi.get(j)); // negated recent most gradient
          double newv = g.get(j) + gam * h.get(j);
          xi.set(j,newv);
          h.set(j,newv);
          /* check that the inner product of gradient and line search
           * is smaller than zero: */
          tmpd -= newv * g.get(j);
        }
        if (tmpd > 0.0 || verbose > 2) {
          System.err.println("New line search has inner product "+tmpd);
        }
        if (tmpd > 0.0) {
          if (!rich) {
            System.err.print("Setting rich to true; ");
            rich = true;
          }
          restart = 2;
          if (verbose > 0) {
            System.err.println("Restarting macopt (2)");
          }
        }

      }
    }
    if (verbose > 0) {
      System.err.println("| Reached iteration limit in macopt. Stopping.");
    }
    return;
  }

  private void macoptRestart(int start) {
    if (start == 0) {
      lastx = lastx_default;
    }
    if (restart != 2) {
      for (int j=0; j<dim; j++) {
        double mj = -xi.get(j);
        g.set(j, mj);
        h.set(j, mj);
        xi.set(j, mj);
      }
    } else {
      for (int j=0; j<dim; j++) {
        double gj = g.get(j);
        xi.set(j, gj);
        h.set(j, gj);
      }
    }
    restart = 0;
  }

  public double maclinmin() {
    double x;
    double y;
    double s = 0.0;
    double t = 0.0;
    int its = 0;
    double step;
    if (verbose > 2) {
      System.err.println("Inner product at 0 = "+macprod(p,gy,0.0));
      // FIXME: this use of tmpd is messy and a bit opaque
      if (tmpd > 0.0) {
        restart = 1;
        return 0.0;
      }
    }
    x = lastx / gtyp;
    y = x; // to avoid compiler complaints
    s = macprod(p, gx, x); // get slope
    if (s < 0) {
      // we need to go further
      while (its < linmin_maxits) {
        y = x * linmin_g1;
        t = macprod(p, gy, y);
        if (verbose > 1) {
          System.err.println("s="+s+", t="+t+", x="+x+", y="+y);
        }
        if (t >= 0.0) { break; }
        x = y;
        s = t;
        // pointer swap gx and gy (to save cost of reallocation)
        ArrayList<Double> swap = gx; gx = gy; gy = swap;
        its++;
      }
    } else
    if (s > 0) {
      // need to step back inside interval
      while (its < linmin_maxits) {
        y = x * linmin_g3;
        t = macprod(p, gy, y);
        if (verbose > 1) {
          System.err.println("s="+s+", t="+t+", x="+x+", y="+y);
        }
        if (t <= 0.0) { break; }
        x = y;
        s = t;
        // pointer swap gx and gy (to save cost of reallocation)
        ArrayList<Double> swap = gx; gx = gy; gy = swap;
        its++;
      }
    } else {
      // hole in one s = 0.0
      t = 1.0;
      y = x;
    }
    if (its >= linmin_maxits) {
      System.err.println("Warning: maclinmin overran!");
      /* this can happen where the function goes \_ and doesn't
         buck up again; it also happens if the initial `gradient'
         does not satisfy gradient · `gradient' > 0, so that there
         is no minimum in the supposed downhill direction.
         I don't know if this actually happens... If it does then
         I guess "rich" should be set to true.
         
         If the overrun is because too big a step was taken then
         the interpolation should be made between zero and the most 
         recent measurement.
         
         If the overrun is because too small a step was taken then 
         the best place to go is the most distant point. 
         I will assume that this doesn't happen for the moment.
         
         Also need to check up what happens to t and s in the case of
         overrun.  And gx and gy. 
         
         Maybe sort this out when writing a macopt that makes use of
         the gradient at zero?
      */
      tmpd = macprod(p, gy, 0.0);
      System.err.println("- inner product at 0 = "+tmpd);
      if (tmpd > 0.0 && !rich) {
        System.err.println("Setting rich to true.");
        rich = true;
      }
      if (tmpd > 0.0) {
        restart = 1;
      }
    }
    // linear interpolation between the last two.
    // This assumes that x and y DO bracket the minimum.
    if (s < 0.0) { s = -s; }
    if (t < 0.0) { t = -t; }
    double m = (s + t);
    s /= m;
    t /= m;
    m = s*y + t*x;
    // evaluate the step length, not that it necessarily means anything
    step = 0.0;
    if (verbose > 2) {
      System.err.println(">> s: "+s);
      System.err.println(">> t: "+t);
      System.err.println(">> x: "+x);
      System.err.println(">> y: "+y);
      System.err.println(">> m: "+m);
      System.err.println(">> p: "+p);
      System.err.println(">> xi: "+xi);
    }
    for (int i=0; i<dim; i++) {
      double tmp = m * xi.get(i);
      if (Double.isNaN(tmp)) {
        System.err.println("NaN detected.");
        throw new RuntimeException();
      }
      // this is the point where the parameter vector steps:
      p.set(i, p.get(i) + tmp);
      step += Math.abs(tmp);
      xi.set(i, s*gy.get(i) + t*gx.get(i));
    }
    /* we've updated xi to contain our estimated new gradient */
    lastx = m * linmin_g2 * gtyp;
    return (step / (double) dim);
  }

  public double macprod(ArrayList<Double> p, ArrayList<Double> a, double y) {
    double s = 0.0;
    for (int i=0; i<dim; i++) {
      pt.set(i, p.get(i) + y*xi.get(i));
    }
    ArrayList<Double> grad = mvg.getGradient(pt);
    for (int i=0; i<dim; i++) {
      double g = grad.get(i);
      a.set(i, g);
      s += g * xi.get(i);
    }
    //System.err.println("MACPROD("+p+", "+gy+") = "+s);
    return s;
  }

  public void checkgrad(ArrayList<Double> p, double epsilon, int stopat) {
    // evaluate at point
    System.out.println("Testing gradient evaluation at "+p);
    double f1 = mvg.eval(p);
    ArrayList<Double> grad = mvg.getGradient(p);
    System.out.println("Gradient = "+grad);
    if (stopat <= 0 || stopat > dim) {
      stopat = dim;
    };
    ArrayList<Double> h = initVector();
    System.out.println("     Gradient   Numeric  Difference");
    for (int j=0; j<stopat; j++) {
      double tmp = p.get(j);
      p.set(j, tmp+epsilon);
      h.set(j, mvg.eval(p) - f1);
      p.set(j, tmp);
      System.out.printf("%2d %9.5g %9.5g %9.5g\n", j, grad.get(j),
                        h.get(j)/epsilon,
                        grad.get(j) - h.get(j)/epsilon);
    }
    System.out.printf("     --------   -------\n");
  }

  public static void main(String[] args) {
    
  }


}
