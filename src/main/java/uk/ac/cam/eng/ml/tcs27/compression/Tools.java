/* Automated copy from build process */
/* $Id: Tools.java,v 1.26 2015/12/07 04:53:29 chris Exp $ */
package uk.ac.cam.eng.ml.tcs27.compression;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;
import java.math.BigInteger;

/** A collection of mathematical tools. */
public class Tools {

  /** Natural logarithm of <var>2</var>: ln(2). */
  public static final double LN2 = 0.6931471805599453;
  
  /** Natural logarithm of <var>10</var>: ln(10). */
  public static final double LN10 = 2.302585092994046;

  /** Base-2 logarithm of <var>e</var>: log₂(Math.E). */
  public static final double LOG2E = 1.4426950408889634;

  /** Approximates the natural logarithm of the Gamma function.
    * @return log Γ(x) */
  public static double logGamma(double x) {
    double tmp = (x - 0.5) * Math.log(x + 4.5) - (x + 4.5);
    double ser = 1.0 + 76.18009173    / (x + 0)   - 86.50532033    / (x + 1)
                     + 24.01409822    / (x + 2)   -  1.231739516   / (x + 3)
                     +  0.00120858003 / (x + 4)   -  0.00000536382 / (x + 5);
    return tmp + Math.log(ser * Math.sqrt(2 * Math.PI));
  }
  
  /** Approximates the Gamma function.
    * Works by calling logGamma and exponentiating.
    * @return Γ(x)
    * @see #logGamma(double) */
  public static double gamma(double x) {
    return Math.exp(logGamma(x));
  }

  /** Approximates the Digamma function.
    * Works by computing a small differential of logGamma.
    * @see #logGamma(double) */
  public static double digamma(double x) {
    double ep = 1e-3;
    return 0.5*((logGamma(x+ep)-logGamma(x-ep))/ep);
  }

  public static double lambertW(double x) {
    // http://mathoverflow.net/questions/12828/inverse-gamma-function
    final double inve = 0.3678794411714423215955237701614608;
    final double eps = 4.0e-14;
    assert (x >= -inve);
    if (x < -inve || Double.isInfinite(x)) {
      throw new IllegalArgumentException("lambertW("+x+")");
    }
    double w = 0.0;
    if (x == 0.0) {
      return 0.0;
    }
    if (x < -inve+1e-4) {
      double q = x+inve;
      double r = Math.sqrt(q);
      double q2 = q*q;
      double q3 = q*q2;
      return -1.0
             +2.331643981597124203363536062168*r
             -1.812187885639363490240191647568*q
             +1.936631114492359755363277457668*r*q
             -2.353551201881614516821543561516*q2
             +3.066858901050631912893148922704*q2*r
             -4.175335600258177138854984177460*q3
             +5.858023729874774148815053846119*q3*r
             -8.401032217523977370984161688514*q3*q;
    }
    if (x < 1.0) {
      double p = Math.sqrt(2.0 * (Math.E*x + 1.0));
      w = -1.0+p*(1.0+p*(-1/3 + p*0.152777777777777777777777));
    } else {
      w = Math.log(x);
    }
    if (x > 3.0) { w -= Math.log(w); }
    // approx:
    for (int k=0; k<12; k++) {
      //System.err.println("k="+k+", w = "+w);
      double e = Math.exp(w);
      double t = w*e - x;
      double p = w+1.0;
      t /= e*p - 0.5*(p+1.0)*t/p;
      w -= t;
      if (Math.abs(t) < eps*(Math.abs(w)+1.0)) { return w; }
    }
    throw new java.lang.IllegalArgumentException("unsupported: "+x);
  }

  /** Returns (an approximation of) the inverse Gamma function. */
  public static double invGamma(double x) {
    // location of the gamma function's minimum value
    final double k = 1.461632144968362341262659542325;
    //           c = Math.sqrt(2 * Math.PI)/Math.E - gamma(k);
    final double c = 0.03653381448490041660033584716880044616984284440438;
    final double sqrt2pi = 2.50662827463100050241576528481104525300698674;
    double l = Math.log((x+c) / sqrt2pi);
    return 0.5 + l / lambertW(l/Math.E);
  }

  /** Approximates the natural logarithm of the Beta function.
    * Uses logGamma.
    * @return log Β(a,b)
    * @see #logGamma(double) */
  public static double logBeta(double a, double b) {
    return logGamma(a)+logGamma(b)-logGamma(a+b);
  }

  /** Approximates the Beta function.
    * Works by calling logBeta and exponentiating.
    * @return Β(a,b)
    * @see #logBeta(double,double) */
  public static double beta(double a, double b) {
    return Math.exp(logBeta(a,b));
  }
  
  /** Computes generalised logBeta for coefficients <code>xs</code>.
    * logΒ(xs) = (SUM[x∈xs] logΓ(x)) - logΓ(SUM[x∈xs] x).
    * @see #logBeta(double,double)
    * @see #logBeta(Collection) */
  public static double logBeta(double[] xs) {
    double lbx = 0.0;
    double sum = 0.0;
    for (double x : xs) {
      lbx += logGamma(x);
      sum += x;
    }
    return lbx - logGamma(sum);
  }
  
  /** Computes generalised logBeta for coefficients <code>xs</code>.
    * logΒ(xs) = (SUM[x∈xs] logΓ(x)) - logΓ(SUM[x∈xs] x).
    * @see #logBeta(double,double)
    * @see #logBeta(double[]) */
  public static double logBeta(Collection<Double> xs) {
    double lbx = 0.0;
    double sum = 0.0;
    for (double x : xs) {
      lbx += logGamma(x);
      sum += x;
    }
    return lbx - logGamma(sum);
  }
  
  /** Computes generalised Beta for coefficients <code>xs</code>.
    * @see #logBeta(double[]) */
  public static double beta(double[] xs) {
    return Math.exp(logBeta(xs));
  }
  
  /** Computes generalised Beta for coefficients <code>xs</code>.
    * @see #logBeta(Collection) */
  public static double beta(Collection<Double> xs) {
    return Math.exp(logBeta(xs));
  }
  
  
  /** Computes the logit of argument <var>θ</var>.
    * Definition: logit(θ) = log( θ / (1-θ) ). */
  public static double logit(double theta) {
    return Math.log(theta/(1-theta));
  }
  
  /** Computes the inverse logit of argument <var>x</var>.
    * Definition: invlogit(x) = 1 / (exp(-x) + 1). */
  public static double invlogit(double x) {
    return 1.0 / (Math.exp(-x)+1.0);
  }

  /** Computes (unsigned) Stirling cycle numbers.
    * These are also called unsigned Stirling numbers of the first kind.
    * Works by simple recursive computation. */
  public static long scn(int n, int k) {
    if (n == k) {
      return 1;
    }
    if (k == 0) {
      return 0;
    }
    long a = scn(n-1,k-1);
    long b = scn(n-1,k);
    return a + (n-1)*b;  // use "-" for signed variant
  }
  
  /** Returns the factorial of <var>k</var>.
    * Simplistic iterative algorithm, performs <var>k</var>-1
    * multiplications.<br>
    * <b>Note:</b><ul>
    * <li>There are no provisions against overflow, so apply with care.</li>
    * <li>Consider using the Gamma function: fact(k) = gamma(k+1).</li>
    * </ul>
    * @see #doubleFact(int)
    * @see #bigFact(long)
    * @see #gamma(double)
    * @return <var>k</var>! */
  public static long longFact(int k) {
    long res = 1;
    while (k > 1) {
      res *= k;
      k--;
    }
    return res;
  }
  
  /** Returns the factorial of <var>k</var>.
    * Simplistic iterative algorithm, performs <var>k</var>-1
    * double precision floating point multiplications.<br>
    * <b>Note:</b><ul>
    * <li>No provisions against overflow are made, so expect
    *     positive infinity on large inputs.</li>
    * <li>Consider using the Gamma function: fact(k) = gamma(k+1).</li>
    * </ul>
    * @see #longFact(int)
    * @see #bigFact(long)
    * @see #gamma(double)
    * @return <var>k</var>! */
  public static double doubleFact(int k) {
    double res = 1.0;
    while (k > 1) {
      res *= k;
      k--;
    }
    return res;
  }
  
  /** Returns the factorial of <var>k</var>, as BigInteger.
    * Simplistic iterative computation, performs <var>k</var>-1
    * BigInteger arithmetic multiplications.<br>
    * @see #gamma(double)
    * @see #doubleFact(int)
    * @see #longFact(int)
    * @return <var>k</var>! */
  public static BigInteger bigFact(long k) {
    BigInteger res = BigInteger.ONE;
    while (k > 1) {
      res = res.multiply(BigInteger.valueOf(k));
      k--;
    }
    return res;
  }
  
  /** Returns the log factorial of <var>k</var>.
    * Severely disrecommended iterative algorithm, which
    * makes <var>k</var>-1 calls to <code>Math.log</code>.
    * @deprecated please use <code>logGamma</code> instead
    * @see #logFact(int)
    * @see #logGamma(double) */
  public static double bruteForceLogFact(int k) {
    double res = 0.0;
    while (k > 1) {
      res += Math.log(k);
      k--;
    }
    return res;
  }
  
  /** Returns the log factorial of <var>k</var>.
    * Calls <code>logGamma</code>.
    * @see #logGamma(double)
    * @return <code>logGamma(k+1)</code> */
  public static double logFact(int k) {
    return logGamma((double) (k+1));
  }
  
  /** Computes the binomial coefficient (<var>n</var> over <var>k</var>).
    * Uses an iterative multiplicative approach. */
  public static long choose(int n, int k) {
    if (k > n) {
      return 0;
    }
    if (k > n/2) {
      k = n-k;  // smaller k is better
    }
    double acc = 1.0;
    for (int j = 1; j <= k; j++) {
      acc *= ((double) (n-k+j));
      acc /= ((double) (j));
    }
    return (long) (acc + 0.5);
  }
  
  /** Computes the logarithm of the binomial coefficient (<var>n</var>
    * over <var>k</var>).
    * Implemented as an interative sum of logarithms. */
  public static double logChoose(int n, int k) {
    if (k > n) {
      return Double.NEGATIVE_INFINITY;
    }
    if (k > n/2) {
      k = n-k;  // bigger k is better
    }
    double acc = 0.0;
    for (int b = 1; b <= k; b++) {
      acc += Math.log(b) - Math.log(n-k+b);
    }
    return -acc;
  }

  /** Computes the binomial coefficient for real-valued inputs.
    * Uses the Gamma function.
    * @return Γ(x+1)/(Γ(y+1)·Γ(x-y+1))
    * @see #gamma(double) */
  public static double choose(double x, double y) {
    return gamma(x+1)/(gamma(y+1)*gamma(x-y+1));
  }
  
  /** Computes the log binomial coefficient for real-valued inputs.
    * Uses the log-Gamma function.
    * @return log Γ(x+1) - log Γ(y+1) - log Γ(x-y+1)
    * @see #logGamma(double) */
  public static double logChoose(double x, double y) {
    return logGamma(x+1) - logGamma(y+1) - logGamma(x-y+1);
  }
 
  /** An array containing Bell numbers B(0) to B(25).
    * Subsequent Bell numbers cannot be represented in a Java integer
    * of size <code>long</code>. */
  protected static long[] bell = new long[] { 1, 1, 2, 5, 15, 52, 203, 877,
    4140, 21147, 115975, 678570, 4213597, 27644437, 190899322, 1382958545,
    10480142147L, 82864869804L, 682076806159L, 5832742205057L,
    51724158235372L, 474869816156751L, 4506715738447323L, 44152005855084346L,
    445958869294805289L, 4638590332229999353L };


  /** Returns the <var>n</var>th Bell number as <code>long</code>,
    * if <var>n</var> is less than 26.
    * Lookup via static array, runs in contant time.<br>
    * <b>Note:</b> Bell numbers greater than 25 cannot be represented
    * in a Java integer of type <code>long</code>.
    * @return B(n), when 0 ≤ <var>n</var> ≤ 25
    * @throws IllegalArgumentException if <var>n</var> is out of range.
    * @see #bigBell(int)
    * @see #doubleBell(int)
    * @see #logBell(int) */
  public static long longBell(int n) {
    if (n >=0 && n < 26) {
      return bell[n];
    } else {
      throw new IllegalArgumentException();
    }
  }


  /** Returns the <var>n</var>th Bell number.
    * Iterative computation via Peirce's triangle.
    * Runs in O(<var>n</var>²) time and O(<var>n</var>) space,
    * not taking into account the complexity of the use of
    * arbitrary precision integer arithmetic.
    * @see #doubleBell(int)
    * @see #longBell(int)
    * @see #logBell(int)
    * @return B(n)
    * @throws IllegalArgumentException if <var>n</var> is negative. */
  public static BigInteger bigBell(int n) {
    BigInteger[] a = new BigInteger[n];
    if (n > 0) {
      a[0]=BigInteger.ONE;
      for (int k=1; k<n; k++) {
        // copy front element to last position
        a[k] = a[0];
        for (int j=k-1; j>=0; j--) {
          a[j]=a[j].add(a[j+1]);
        }
      }
      return a[0];
    } else
    if (n == 0) {
      return BigInteger.ONE;
    } else {
      throw new IllegalArgumentException();
    }
  }


  /** Returns the <var>n</var>th Bell number.
    * Iterative computation via Peirce's triangle.
    * Runs in O(<var>n</var>²) time and O(<var>n</var>) space.<br>
    * <b>Note:</b> Bell numbers greater than 218 cannot be represented
    * in a Java double precision floating point number; for values
    * greater than 218, positive infinity is returned.
    * @see #bigBell(int)
    * @see #longBell(int)
    * @see #logBell(int)
    * @return B(n), when <var>n</var> ≤ 218, otherwise +∞
    * @throws IllegalArgumentException if <var>n</var> is negative. */
  public static double doubleBell(int n) {
    double[] a = new double[n];
    if (n > 0 && n < 219) {
      a[0]=1.0;
      for (int k=1; k<n; k++) {
        // copy front element to last position
        a[k] = a[0];
        for (int j=k-1; j>=0; j--) {
          a[j]+=a[j+1];
        }
      }
      return a[0];
    } else
    if (n > 218) {
      return Double.POSITIVE_INFINITY;
    } else
    if (n == 0) {
      return 1.0;
    } else {
      throw new IllegalArgumentException();
    }
  }

  /** Returns the logarithm of the <var>n</var>th Bell number.
    * Calls <code>bigBell(n)</code> and therefore inherits its
    * space and time complexity.
    * @see #longBell(int)
    * @see #bigBell(int)
    * @see #doubleBell(int)
    * @return ≈ ln(B(n)) */
  public static double logBell(int n) {
    return Tools.log(bigBell(n));
  }

  /** Crudely approximates the logarithm of a BigInteger.
    * @return ≈ ln(b) */
  public static double log(BigInteger b) {
    int sign = b.signum();
    if (sign == 1) { 
      double d = b.doubleValue();
      if (d == Double.POSITIVE_INFINITY) {
        return (double) (b.bitLength()-1) / LOG2E;
      } else {
        return Math.log(d);
      }
    } else
    if (sign == 0) {
      return Double.NEGATIVE_INFINITY;
    } else {
      return Double.NaN;
    }
  }

  /** Finds the maximum value in an array.
    * @throws IllegalArgumentException if the array has length zero 
    * @return the maximum value */
  public static double max(double[] xs) {
    if (xs.length == 0) {
      throw new IllegalArgumentException();
    }
    double max = Double.NEGATIVE_INFINITY;
    for (int k=0; k<xs.length; k++) {
      if (xs[k] > max) { max = xs[k]; }
    }
    return max;
  }

  /** Computes the log of the sum of exponentials. */
  /*
  public static double logSumExp(double[] xs) {
    if (xs.length == 0) return 0.0;
    if (xs.length == 1) return xs[0];
    double max = max(xs);
    double sum = 0.0;
    for (int k=0; k<xs.length; k++) {
      if (xs[k] != Double.NEGATIVE_INFINITY) {
        sum += Math.exp(xs[k] - max);
      }
    }
    return max + Math.log(sum);
  }
  */
  
  /** Computes the log of the sum of exponentials. */
  public static double logSumExp(double... xs) {
    if (xs.length == 0) return 0.0;
    if (xs.length == 1) return xs[0];
    double max = max(xs);
    double sum = 0.0;
    for (int k=0; k<xs.length; k++) {
      if (xs[k] != Double.NEGATIVE_INFINITY) {
        sum += Math.exp(xs[k] - max);
      }
    }
    return max + Math.log(sum);
  }
  
  /** Computes the greatest common divisor.
    * Assumes <code>a</code> and <code>b</code> are strictly positive.
    * Uses Euclid's algorithm.
    * @see #gcd(long,long) */
  public static int gcd(int a, int b) {
    int r;
    if (a > b) {
      a = a - b;
      b = b + a;
      a = b - a;
    }
    if (b == 0) {
      return a;
    }
    while (true) {
      r = a % b;
      if (r == 0) {
        return b;
      } else {
        a = b;
        b = r;
      }
    }
  }
  
  /** Computes the greatest common divisor.
    * Assumes <code>a</code> and <code>b</code> are strictly positive.
    * Uses Euclid's algorithm.
    * @see #gcd(int,int) */
  public static long gcd(long a, long b) {
    long r;
    if (a > b) {
      a = a - b;
      b = b + a;
      a = b - a;
    }
    if (b == 0) {
      return a;
    }
    while (true) {
      r = a % b;
      if (r == 0) {
        return b;
      } else {
        a = b;
        b = r;
      }
    }
  }

  /** Returns a fraction (<var>a</var>/<var>b</var>) approximation
    * of floating point number <var>r</var>.
    * @param r floating point number
    * @param max maximum value for denominator <var>b</var>
    * @return fraction approximating <var>r</var>. */
  public static Tuple<Long,Long> fraction(double r, long max) {
    if (Double.isNaN(r)) {
      throw new IllegalArgumentException("NaN");
    } else
    if (Double.isInfinite(r)) {
      if (r > 0) {
        return Tuple.of((long) 1, (long) 0);
      } else {
        return Tuple.of((long) -1, (long) 0);
      }
    }
    long a = 1; long b = 0;
    long c = 0; long d = 1;
    long ai = (long) r;
    while (c * ai + d <= max) {
      long tmp = a * ai + b;
      b = a;
      a = tmp;
      tmp = c * ai + d;
      d = c;
      c = tmp;
      if (r != ai) {
        r = 1.0/(r - (double) ai);
        if (r > (double) (0x01 << 31 - 1)) {
          break;
        }
      } else {
        break;
      }
      ai = (long) r;
    }
    if (c < 0) {
      return Tuple.of(-a,-c);
    } else {
      return Tuple.of(a,c);
    }
  }
  
  /** Returns the most significant bit of <var>x</var>,
    * in its original position.
    * For example, msb(7) equals 4 and msb(9) equals 8. */
  public static int msb(int x) {
    x |= (x >> 1);
    x |= (x >> 2);
    x |= (x >> 4);
    x |= (x >> 8);
    x |= (x >> 16);
    return x & ~(x >> 1);
  }

  /** Returns the next power-of-two bigger than <var>x</var>.
    * @see #nbe2p(int) */
  public static int nb2p(int x) {
    x |= (x >> 1);
    x |= (x >> 2);
    x |= (x >> 4);
    x |= (x >> 8);
    x |= (x >> 16);
    return x+1;
  }
  
  /** Returns the next power-of-two bigger or equal to <var>x</var>.
    * @see #nb2p(int) */
  public static int nbe2p(int x) {
    return nb2p(x-1);
  }

  /** Returns the information entropy of a discrete random
    * variable whose probability distribution is given by the
    * <var>k</var>-length collection <var>pp</var>.
    * Note that <var>pp</var>:s components must sum to 1.
    * @return the entropy (in nats) */
  public static double entropy(Collection<Double> pp) {
    double entr = 0.0;
    for (double p : pp) {
      entr -= p * Math.log(p);
    }
    return entr;
  }
  
  /** Returns the information entropy of a discrete random
    * variable whose probability distribution is given by the
    * <var>k</var>-length array <var>pp</var>.
    * Note that <var>pp</var>:s components must sum to 1.
    * @return the entropy (in nats) */
  public static double entropy(double[] pp) {
    double entr = 0.0;
    for (double p : pp) {
      entr -= p * Math.log(p);
    }
    return entr;
  }

  /** Returns the information entropy of a discrete distribution.
    * @param m probability mass function
    * @param set supporting set of elements
    * @return the entropy (in nats) */
  public static <X> double entropy(Mass<X> m, Iterable<X> set) {
    double e = 0.0;
    double p;
    for (X x : set) {
      p = m.mass(x);
      e -= p * Math.log(p);
    }
    return e;
  }

  /** Fraction to Unicode String. */
  public static String fr2s(int a, int b) {
    switch (b) {
      case 1: return ""+a;
      case 2: return "½";
      case 3: if (a==1) { return "⅓"; } else { return "⅔"; }
      case 4: if (a==1) { return "¼"; } else { return "¾"; }
      case 6: if (a==1) { return "⅙"; } else { return "⅚"; }
      case 5: switch (a) {
                case 1: return "⅕";
                case 2: return "⅖";
                case 3: return "⅗";
                case 4: return "⅘";
                default: return a+"/"+b;
              }
      case 8: switch (a) {
                case 1: return "⅛";
                case 3: return "⅜";
                case 5: return "⅝";
                case 7: return "⅞";
                default: return a+"/"+b;
              }
      default: return a+"/"+b;
    }
  }
  
  /** Fraction to Unicode String. */
  public static String fr2s(double r) {
    Tuple<Long,Long> frac = Tools.fraction(r,0x10000);
    int a = frac.get0().intValue();
    int b = frac.get1().intValue();
    return fr2s(a,b);
  }
  
  
  /** Merge-sorts two lists in ascending order of the first list.
    * The values of the first list determine the sort order,
    * the second list is permuted along, so its entries still match
    * up with those of the first list.
    * The algorithm used is a basic merge sort.
    * @param xl list to be sorted 
    * @param yl list to be permuted along
    * @param c  a Comparator for elements of the first list */
  public static <X,Y> void sort2(List<X> xl, List<Y> yl, Comparator<X> c) {
    int length = xl.size();
    if (length <= 1) {
      return;
    } else
    if (length == 2) {
      int cmp = c.compare(xl.get(0),xl.get(1));
      if (cmp > 0) {
        // swap both x:s and both y:s
        Collections.swap(xl,0,1);
        Collections.swap(yl,0,1);
      }
      // we're done
      return;
    } else {
      int m = length/2;
      // sort sublists recursively
      ArrayList<X> xlower = new ArrayList<X>(xl.subList(0,m));
      ArrayList<X> xhigher = new ArrayList<X>(xl.subList(m,length));
      ArrayList<Y> ylower = new ArrayList<Y>(yl.subList(0,m));
      ArrayList<Y> yhigher = new ArrayList<Y>(yl.subList(m,length));
      sort2(xlower,ylower,c);
      sort2(xhigher,yhigher,c);
      // and merge them
      int plo = 0;
      int phi = 0;
      X xlo = xlower.get(plo);
      X xhi = xhigher.get(phi);
      for (int mp = 0; mp < length; mp++) {
        int cmp = c.compare(xlo,xhi);
        if (cmp <= 0) {
          // take from lower list, advance plo
          xl.set(mp,xlo);
          yl.set(mp,ylower.get(plo));
          plo++;
          if (plo < xlower.size()) {
            xlo = xlower.get(plo);
          } else {
            // if the end is reached, copy rest of high array
            xlo = null;
            while (phi < xhigher.size()) {
              mp++;
              xl.set(mp,xhigher.get(phi));
              yl.set(mp,yhigher.get(phi));
              phi++;
            }
            break;
          }
        } else {
          // take from higher list, advance phi
          xl.set(mp,xhi);
          yl.set(mp,yhigher.get(phi));
          phi++;
          if (phi < xhigher.size()) {
            xhi = xhigher.get(phi);
          } else {
            // if the end is reached, copy rest of low array
            xhi = null;
            while (plo < xlower.size()) {
              mp++;
              xl.set(mp,xlower.get(plo));
              yl.set(mp,ylower.get(plo));
              plo++;
            }
            break;
          }
        }
      }
    }
  }
  
  /** Merge-sorts two lists in ascending natural order of the first list.
    * The values of the first list determine the sort order,
    * the second list is permuted along, so its entries still match
    * up with those of the first list.
    * The algorithm used is a basic merge sort.
    * @param xl list to be sorted 
    * @param yl list to be permuted along */
  public static <X extends Comparable<? super X>,Y>
                 void sort2(List<X> xl, List<Y> yl) {
    sort2(xl,yl,new Comparator<X>() {
      public int compare(X x1, X x2) {
        return x1.compareTo(x2);
      }
    });
  }
  
  /** Merge-sorts two lists in descending natural order of the first list.
    * The values of the first list determine the sort order,
    * the second list is permuted along, so its entries still match
    * up with those of the first list.
    * The algorithm used is a basic merge sort.
    * @param xl list to be sorted in reverse order
    * @param yl list to be permuted along */
  public static <X extends Comparable<? super X>,Y>
                 void revsort2(List<X> xl, List<Y> yl) {
    sort2(xl,yl,Collections.reverseOrder(new Comparator<X>() {
      public int compare(X x1, X x2) {
        return x1.compareTo(x2);
      }
    }));
  }


  /** Copies an array, omitting a specified element.
    * @param src source array
    * @param trg target array
    * @param k   index of element to be omitted
    * @param len length of source array */
  private static void arrayCopyWithout(Object src, Object trg, int len, int k) {
    System.arraycopy(src,0,trg,0,k);
    System.arraycopy(src,k+1,trg,k,len-k-1);
  }
  
}
