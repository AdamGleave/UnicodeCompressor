/* $Id$ */
package uk.ac.cam.eng.ml.tcs27.compression;

import java.util.Iterator;
import java.util.Hashtable;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Arrays;
import java.util.Vector;
import java.util.Random;
import java.util.Collection;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintStream;
import java.io.IOException;
import java.io.FileNotFoundException;

/** Implementation of PPM (Prediction by Partial Match). */
public class PPM<X> extends SimpleMass<X> implements DSP<X>, AdaptiveCode<X> {

  /** Base distribution. */
  Distribution<X> base = null;

  /** Root of context tree. */
  Node<X> root = null;

  /** Current context tree. */
  Node<X> context = null;

  /** Maximum context depth. */
  int maxdepth = 5;

  /** Current context depth. */
  protected int d = 0;
  
  /** Length of total context, total number of symbols. */
  public long count = 0;

  /** Cached symbol distribution. */
  private Distribution<X> cachedDist = null;

  /** Cached context pointer. */
  private Node<X> cachedContext = null;



  /** Minimal constructor. */
  private PPM() {
  }

  /** Constructs a new PPM process of given context depth and base
    * distribution.
    * @param maxdepth maximum depth of the context tree
    * @param alpha PPM alpha parameter (this will be converted to a fraction)
    * @param beta PPM beta parameter (this will be converted to a fraction)
    * @param base base distribution over symbols */
  public PPM(int maxdepth, double alpha, double beta, Distribution<X> base) {
    this.maxdepth = maxdepth;
    this.base = base;
    this.context = new Node<X>(alpha,beta);
    this.root = this.context;
  }
  
  /** Constructs a new PPM process of given context depth and base
    * distribution.
    * @param maxdepth maximum depth of the context tree
    * @param a1 PPM alpha parameter (numerator)
    * @param a2 PPM alpha parameter (denominator)
    * @param b1 PPM beta parameter (numerator)
    * @param b2 PPM beta parameter (denominator)
    * @param base base distribution over symbols */
  public PPM(int maxdepth, int a1, int a2, int b1, int b2,
                                                   Distribution<X> base) {
    this.maxdepth = maxdepth;
    this.base = base;
    this.context = new Node<X>(a1,a2,b1,b2);
    this.root = this.context;
  }
  
  /** Constructs a new PPM process with given parameters.
    * <br>
    * Example parameter string: "<tt>d=5:a=0:b=0.5</tt>".
    * @param pars parameters in String form
    * @param base base distribution over symbols */
  public PPM(String pars, Distribution<X> base) {
    // defaults:
    this.base = base;
    this.maxdepth = 5;
    double alpha = 0.0;
    double beta = 0.5;
    int a1 = 0;
    int a2 = 1;
    int b1 = 1;
    int b2 = 2;
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
        if (key.equals("a")) {
          alpha = Double.valueOf(val);
        } else
        if (key.equals("b")) {
          beta = Double.valueOf(val);
        } else {
          System.err.println("Warning: PPM: unknown parameter \""+s[k]+"\"");
        }
      }
    }
    this.context = new Node<X>(alpha,beta);
    this.root = this.context;
  }

  /** Constructs a new PPM process of given context depth and base
    * distribution.
    * @param maxdepth maximum depth of the context tree
    * @param sa1 standard PPM alpha parameter (numerator)
    * @param sa2 standard PPM alpha parameter (denominator)
    * @param sb1 standard PPM beta parameter (numerator)
    * @param sb2 standard PPM beta parameter (denominator)
    * @param ea1 exception PPM alpha parameter (numerator)
    * @param ea2 exception PPM alpha parameter (denominator)
    * @param eb1 exception PPM beta parameter (numerator)
    * @param eb2 excetiopn PPM beta parameter (denominator)
    * @param eset set of "exception characters"
    * @param base base distribution over symbols */
  public PPM(int maxdepth, int sa1, int sa2, int sb1, int sb2,
                           int ea1, int ea2, int eb1, int eb2, 
                           Set<X> eset, Distribution<X> base) {
    this.maxdepth = maxdepth;
    this.base = base;
    this.context = new Node<X>(sa1,sa2,sb1,sb2,
                               ea1,ea2,eb1,eb2,eset);
    this.root = this.context;
  }

  /** Clones this PPM process. */
  public PPM<X> clone() {
    PPM<X> copy = new PPM<X>();
    // copy additional information
    copy.base = this.base;
    copy.maxdepth = this.maxdepth;
    copy.count = this.count;
    copy.d = this.d;
    // clone the context tree and find its root
    copy.context = this.context.clone();
    copy.root = copy.context.findRoot();
    // cachedDist and cachedContext can be safely ignored
    return copy;
  }

  
  /** Interface representing sets of nodes. */
  public static interface NodeSet<X> extends Iterable<Node<X>> {
    /** Retrieve a node matching a given symbol. */
    public Node<X> get(X sym);
    /** Adds a node to the set. */
    public void add(Node<X> node);
    /** Returns a collection over symbols supported by nodes in
      * this set. */
    public Collection<X> symbols();
  }

  /** Class implementing a NodeSet based on a Hashtable. */
  public static class NodeHashSet<X> extends Hashtable<X,Node<X>>
                                               implements NodeSet<X> {
    public void add(Node<X> node) {
      put(node.sym, node);
    }
    public Iterator<Node<X>> iterator() {
      return values().iterator();
    }
    public Collection<X> symbols() {
      return keySet();
    }
  }


  /** Class representing nodes in a PPM context tree.
    * <p>Each node holds separate values of PPM parameters
    * <var>alpha</var> and <var>beta</var>, represented by integer
    * fractions <var>a<sub>1</sub>/a<sub>2</sub></var> and
    * <var>b<sub>1</sub>/b<sub>2</sub></var>.</p> */
  public static class Node<X> {
    /** Symbol associated with this node. */
    X sym = null;

    /** Set of all children of this node. */
    NodeSet<X> children = null;

    /** Pointer to the tree of the next shorter context.
      * If the current node has symbol "D" and is reached from the
      * root via nodes "A, B, C", then the vine pointer points to
      * the node "D" which is reached from the root via "B, C". */
    Node<X> vine = null;

    /** Count under update exclusion. */
    int ucount = 0;

    /** PPM parameter alpha (numerator). */
    int a1 = 0;
    /** PPM parameter alpha (denominator). */
    int a2 = 1;
    /** PPM parameter beta (numerator). */
    int b1 = 1;
    /** PPM parameter beta (denominator). */
    int b2 = 2;

    Set<X> eset = null;
    int xa1 = 0;
    int xa2 = 100;
    int xb1 = 42;
    int xb2 = 100;
    
    int ya1 = 224;
    int ya2 = 100;
    int yb1 = 42;
    int yb2 = 100;
    

    /** Constructs a root node.
      * Root nodes have an empty context.
      * @param a1 PPM alpha parameter (numerator)
      * @param a2 PPM alpha parameter (denominator)
      * @param b1 PPM beta parameter (numerator)
      * @param b2 PPM beta parameter (denominator) */
    public Node(int a1, int a2, int b1, int b2) {
      this.sym = null;
      this.a1 = a1;
      this.a2 = a2;
      this.b1 = b1;
      this.b2 = b2;
    }
    
    /** Constructs a root node with exception set <var>s</var>.
      * Root nodes have an empty context.
      * @param sa1 standard PPM alpha parameter (numerator)
      * @param sa2 standard PPM alpha parameter (denominator)
      * @param sb1 standard PPM beta parameter (numerator)
      * @param sb2 standard PPM beta parameter (denominator)
      * @param ea1 exception PPM alpha parameter (numerator)
      * @param ea2 exception PPM alpha parameter (denominator)
      * @param eb1 exception PPM beta parameter (numerator)
      * @param eb2 exception PPM beta parameter (denominator)
      * @param s set of exception characters */
    public Node(int sa1, int sa2, int sb1, int sb2,
                int ea1, int ea2, int eb1, int eb2, Set<X> s) {
      this.sym = null;
      this.xa1 = sa1;
      this.xa2 = sa2;
      this.xb1 = sb1;
      this.xb2 = sb2;
      this.ya1 = ea1;
      this.ya2 = ea2;
      this.yb1 = eb1;
      this.yb2 = eb2;
      this.eset = s;
    }

    /** Constructs a root node.
      * Guesses fractions for alpha and beta. 
      * @deprecated */
    public Node(double alpha, double beta) {
      this.sym = null;
      Tuple<Long,Long> afrac = Tools.fraction(alpha,0x1000);
      Tuple<Long,Long> bfrac = Tools.fraction(beta, 0x1000);
      this.a1 = afrac.get0().intValue();
      this.a2 = afrac.get1().intValue();
      this.b1 = bfrac.get0().intValue();
      this.b2 = bfrac.get1().intValue();
    }

    /** Constructs a node with given symbol and pointer
      * to the next shorter context. The shortest possible
      * context is the empty context (i.e. a root node).
      * @param sym symbol stored at this node
      * @param vine vine pointer (pointer to next shorter context)
      * @param a1 PPM alpha parameter (numerator)
      * @param a2 PPM alpha parameter (denominator)
      * @param b1 PPM beta parameter (numerator)
      * @param b2 PPM beta parameter (denominator) */
    public Node(X sym, Node<X> vine, int a1, int a2, int b1, int b2) {
      this.sym = sym;
      this.vine = vine;
      this.ucount = 0;
      this.a1 = a1;
      this.a2 = a2;
      this.b1 = b1;
      this.b2 = b2;
      this.eset = vine.eset;
      this.xa1 = vine.xa1; this.xa2 = vine.xa2;
      this.xb1 = vine.xb1; this.xb2 = vine.xb2;
      this.ya1 = vine.ya1; this.ya2 = vine.ya2;
      this.yb1 = vine.yb1; this.yb2 = vine.yb2;
    }
    
    /** Constructs a node with given symbol and pointer
      * to the next shorter context. 
      * Fractions for alpha and beta are guessed.
      * @deprecated */
    public Node(X sym, Node<X> vine, double alpha, double beta) {
      this.sym = sym;
      this.vine = vine;
      this.ucount = 0;
      Tuple<Long,Long> afrac = Tools.fraction(alpha,0x1000);
      Tuple<Long,Long> bfrac = Tools.fraction(beta, 0x1000);
      this.a1 = afrac.get0().intValue();
      this.a2 = afrac.get1().intValue();
      this.b1 = bfrac.get0().intValue();
      this.b2 = bfrac.get1().intValue();
      this.eset = vine.eset;
      this.xa1 = vine.xa1; this.xa2 = vine.xa2;
      this.xb1 = vine.xb1; this.xb2 = vine.xb2;
      this.ya1 = vine.ya1; this.ya2 = vine.ya2;
      this.yb1 = vine.yb1; this.yb2 = vine.yb2;
    }

    /** Returns the root node of the context tree. */
    public Node<X> findRoot() {
      Node<X> root = this;
      while (root.vine != null) {
        root = root.vine;
      }
      return root;
    }

    /** Adds a child node with given symbol. */
    public Node<X> add(X sym) {
      Node<X> newvine = null;
      if (vine != null) {
        newvine = this.vine.findOrAdd(sym);
      } else {
        newvine = this;
      }
      if (children == null) {
        children = new NodeHashSet<X>();
      }
      Node<X> child = null;
      if (eset == null) {
        child = new Node<X>(sym, newvine, a1, a2, b1, b2);
      } else {
        if (eset.contains(sym)) {
          child = new Node<X>(sym, newvine, ya1, ya2, yb1, yb2);
        } else {
          child = new Node<X>(sym, newvine, xa1, xa2, xb1, xb2);
        }
      }
      children.add(child);
      return child;
    }

    /** Finds the child node of given symbol,
      * or returns <code>null</code> if no such node exists. */
    public Node<X> find(X sym) {
      return children.get(sym);
    }
    
    /** Returns the child node of given symbol.
      * If a matching child node does not exist, it is created. */
    public Node<X> findOrAdd(X sym) {
      if (children != null) {
        Node<X> node = children.get(sym);
        if (node != null) {
          return node;
        } else {
          return add(sym);
        }
      } else {
        return add(sym);
      }
    }

    /** Update counts of symbol <var>sym</var> in this
      * context tree.
      * @return this context's node for <var>sym</var>. */
    public Node<X> update(X sym) {
      Node<X> node = findOrAdd(sym);
      for (Node<X> v = node; v != null; v = v.vine) {
        v.ucount++;
        if (v.ucount>1) break;
      }
      /*
      Node<X> v = node;
      do {
        v.ucount++;
        v = v.vine;
      } while (v != null && v.ucount == 0);
      */
      return node;
    }


    /** Computes the probability of symbol <var>x</var> in this
      * node's context.
      * @param x symbol
      * @param excl set of excluded symbols, or null
      * @param base base distribution
      * @return probability of symbol <var>x</var>. */
    public double p(X x, NodeSet<X> excl, Distribution<X> base) {
      if (children != null) {
        Node<X> found = null;
        long n = 0;
        int k = 0;
        for (Node<X> c : children) {
          if (excl == null || excl.get(c.sym) == null) {
            if (c.sym.equals(x)) {
              found = c;
            }
            n += c.ucount;
            k++;
          }
        }
        if (found != null) {
          return a2*((long)b2*found.ucount - b1) / (b2 * (a2*n + a1));
        } else
        if (n > 0) {
          // escape symbol contribution...
          double esc = ((long)b2*a1 + (long)b1*a2*k) / (b2 * (a2*n + a1));
          if (vine != null) {
            return esc * vine.p(x,children,base);
          } else {
            // compute probability mass of excluded symbols
            double exm = 0.0;
            if (excl != null) {
              for (Node<X> z : excl) {
                exm += base.p(z.sym);
              }
            }
            // TODO: verify this.
            return base.p(x) * esc / (1-exm);
            //return esc + base.logp(x);
          }
        } // otherwise...
      }
      // escape symbol contributes no mass
      if (vine != null) {
        return vine.p(x,null,base);
      } else {
        return base.p(x);
      }
    }
    
    /** Computes the log probability mass of symbol <var>x</var>
      * in this node's context.
      * @param x symbol
      * @param excl set of excluded symbols, or null
      * @param base base distribution
      * @return log probability of symbol <var>x</var>. */
    public double logMass(X x, NodeSet<X> excl, Distribution<X> base) {
      if (children != null) {
        Node<X> found = null;
        long n = 0;
        int k = 0;
        for (Node<X> c : children) {
          if (excl == null || excl.get(c.sym) == null) {
            if (c.sym.equals(x)) {
              found = c;
            }
            n += c.ucount;
            k++;
          }
        }
        if (found != null) {
          //return a2*(b2*found.ucount - b1) / (b2 * (a2*n + a1));
          long bb = (long)b2*found.ucount - b1;
          if (bb == 0) {
            return Double.NEGATIVE_INFINITY;
          } else {
            return Math.log(a2) + Math.log(bb) 
                 - Math.log(b2) - Math.log(a2*n + a1);
          }
        } else
        if (n > 0) {
          // escape symbol contribution...
          double esc = Math.log((long)b2*a1 + (long)b1*a2*k)
                        - Math.log(b2) - Math.log(a2*n + a1);
          if (vine != null) {
            return esc + vine.logMass(x,children,base);
          } else {
            // compute probability mass of children (for exclusion)
            double exm = 0.0;
            for (Node<X> c : children) {
              exm += base.mass(c.sym);
              if (x.equals(c.sym)) {
                // excluded element: zero mass
                return Double.NEGATIVE_INFINITY;
              }
            }
            if (exm != 0.0) {
              return esc + base.logMass(x) - Math.log(1.0-exm);
            } else {
              return esc + base.logMass(x);
            }
          }
        }
        // otherwise...
      }
      // escape symbol contributes no mass
      if (vine != null) {
        // No children, no added exclusions: just pass up existing exclusions
        if (children != null) {
          return vine.logMass(x,children,base);
        } else {
          return vine.logMass(x,excl,base);
        }
      } else {
        double exm = 0.0;
        //return base.logMassWithout(x,excl);
        if (excl != null) {
          for (Node<X> z : excl) {
            exm += base.p(z.sym);
            if (x.equals(z)) {
              // excluded element: zero mass
              return Double.NEGATIVE_INFINITY;
            }
          }
          if (exm == 0.0) {
            return base.logMass(x);
          } else {
            return base.logMass(x) - Math.log(exm);
          }
        } else {
          return base.logMass(x);
        }
      }
    }
    
    /** Computes the log probability mass of symbol <var>x</var>
      * in this node's context.
      * @param x symbol
      * @param excl set of excluded symbols, or null
      * @param base base distribution
      * @return log probability of symbol <var>x</var>. */
    public double mass(X x, NodeSet<X> excl, Distribution<X> base) {
      return Math.exp(logMass(x,excl,base));
    }


    /** Computes the predictive distribution over symbols in this
      * context, given a base distribution.
      * @param base distribution over unseen symbols */
    public Distribution<X> getDist(Distribution<X> base) {
      // TODO: check this method is still correct
      // maybe we should cache the total number of known symbols
      Hashtable<X,Double> m = new Hashtable<X,Double>();
      double budget = 1.0;
      for (Node<X> level = this; level != null; level=level.vine) {
        if (children != null) {
          long total = 0;
          for (Node<X> c : children) {
            total+=c.ucount;
          }
          if (total != 0) {
            double slice = budget;
            for (Node<X> c : children) {
              double p = ((double) (slice * (c.ucount-(double) b1/b2))
                                 / (total + (double) a1/a2));
              budget -= p;
              Double px = m.get(c.sym);
              if (px == null) {
                px = 0.0;
              }
              px += p;
              m.put(c.sym, px);
            }
          }
        }
      }
      if (m.size() > 0) {
        Discrete<X> dm = new Discrete<X>(m,1.0-budget);
        //System.err.println("SUM: "+dm.getSum());
        return new Mixture<X>(budget,base,dm);
      } else {
        return base;
      }
    }
    

    /** Encodes symbol <var>x</var>.
      * Uses the probability distribution over symbols at this node.
      * Uses this node's local <var>alpha</var> and <var>beta</var>
      * parameters (a1, a2, b1, b2).  Update exclusion is used.
      * @param x symbol to encode
      * @param excl a set of symbols which should be excluded
      * @param base base distribution over symbols
      * @param ec arithmetic encoder */
    public void encode(X x, NodeSet<X> excl, Codable<X> base, Encoder ec) {
      /*           D (Bn-A)             BC + ADu      *
       * symbol:   --------     escape: ---------     *
       *           B (DN+C)             B (DN+C)      */
      // so each symbol adds "BD(ucount) - AD" to the cumulative count.
      int k = 0;   // number of unique symbols
      long l = -1;
      long h = 0;
      long n = 0;  // total count
      if (children != null) {
        // iterate over children
        for (Node<X> cc : children) {
          if (excl == null || excl.get(cc.sym) == null) {
            if (cc.sym.equals(x)) {
              // Generalised approach
              l = a2 * ((long)b2*n - k*b1);
              h = l + a2*((long)b2*cc.ucount - b1);
            }
            n += cc.ucount;
            k ++;
          }
        }
      }
      // NOTE: we've only incremented n and k for non-excluded children...
      // TODO: is this way of incrementing "correct"?
      long t = b2*(a2*n + a1);  // total range: B * (D*N + C)
      if (l == -1) {
        // if the symbol wasn't found...
        if (n > 0) {
          // ...but if other symbols exist...
          ec.storeRegion(t - (long)b2*a1 - (long)b1*a2*k, t, t);  // code escape symbol
        }
        if (vine != null) {
          /* We're escaping to the next shorter context, and we're
           * passing up our set of symbols -- to remember that these
           * symbols have already been checked.  Since the next
           * smaller context is guaranteed to contain all of ours,
           * it can again just pass its symbol set up. */
          vine.encode(x,children,base,ec);
        } else {
          // if we're in the empty context, use base distribution
          if (children != null) {
            // and exclude symbols occurring in our context, because
            // we've already checked those.
            base.encode(x,children.symbols(),ec);
          } else {
            base.encode(x,ec);
          }
        }
      } else {
        ec.storeRegion(l,h,t);      // normal symbol
      }
    }


    /** Decodes a symbol.
      * Uses the probability distribution over symbols at this node.
      * Uses this node's local <var>alpha</var> and <var>beta</var>
      * parameters (a1, a2, b1, b2).  Update exclusion is used.
      * @param excl a set of symbols which should be excluded
      * @param base base distribution over symbols
      * @param dc arithmetic decoder */
    public X decode(NodeSet<X> excl, Codable<X> base, Decoder dc) {
      /*           BDn - AD             BC + ADu      *
       * symbol:   --------     escape: ---------     *
       *           B (DN+C)             B (DN+C)      */
      // so each symbol adds "D(Bn - A)" to the cumulative count.
      // [where A=b1, B=b2, C=a1, D=a2]
      long n = 0;  // total count
      if (children != null) {
        for (Node<X> c : children) {
          if (excl == null || excl.get(c.sym) == null) {
            n += c.ucount;
          }
        }
      }
      long t = b2*(a2*n + a1);  // total range: B * (D*N + C)
      if (n > 0) {
        long f = dc.getTarget(t);
        // reconstruct the original symbol and (l,h) from f
        long l = 0;
        long h = 0;
        Node<X> found = null;
        for (Node<X> c : children) {
          if (excl == null || excl.get(c.sym) == null) {
            l = h;
            h = l + a2*((long)b2*c.ucount - b1); // D * (B*n - A)
            if (f >= l && f < h) {
              found = c;
              break;
            }
          }
        }
        if (found != null) {
          // symbol found
          dc.loadRegion(l,h,t);
          return found.sym;  // DONE
        } else {
          // escape symbol...
          dc.loadRegion(h,t,t);
          // ...climb up vine
        }
      }
      if (vine != null) {
        /* We're escaping to the next smaller context, and we're
         * passing up our set of symbols -- to remember that these
         * symbols have already been checked.  Since the next
         * smaller context is guaranteed to contain all of ours,
         * it can again just pass its symbol set up. */
        return vine.decode(children,base,dc);
      } else {
        if (children != null) {
          // and exclude symbols occurring in our context, because
          // we've already checked those.
          return base.decode(children.symbols(),dc);
        } else {
          return base.decode(dc);
        }
      }
    }


    /** PPMD encoding (alpha=0, beta=0.5) */
    public void encodePPMD(X x, Codable<X> base, Encoder ec) {
      int k = 0;
      long l = -1;
      long h = -1;
      long t = 0;  // total range (2 * total count)
      if (children != null) {
        // iterate over children
        for (Node<X> c : children) {
          if (c.sym.equals(x)) {
            // PPMD method:
            l = t - k;
            h = l + 2*c.ucount - 1;
          }
          t += c.ucount << 1;
          k += 1;
        }
      }
      if (l == -1) {
        if (t > 0) {
          ec.storeRegion(t-k, t, t);  // escape symbol
        }
        if (vine != null) {
          // try shorter context:
          vine.encodePPMD(x,base,ec);
        } else {
          // if we're in the empty context, use base distribution:
          base.encode(x,ec);
        }
      } else {
        ec.storeRegion(l,h,t);      // normal symbol
      }
    }
    
    /** PPMD decoding (alpha=0, beta=0.5) */
    public X decodePPMD(Codable<X> base, Decoder dc) {
      long t = 0;  // total range
      if (children != null) {
        for (Node<X> c : children) {
          t += c.ucount;
        }
      }
      t <<= 1;
      if (t > 0) {
        long f = dc.getTarget(t);
        // reconstruct the original symbol and (l,h) from f
        long l = 0;
        long h = 0;
        Node<X> found = null;
        for (Node<X> c : children) {
          // PPMD method:
          l = h;
          h = l + (c.ucount<<1) - 1;
          if (f >= l && f < h) {
            found = c;
            break;
          }
        }
        if (found != null) {
          // symbol found
          dc.loadRegion(l,h,t);
          return found.sym;  // DONE
        } else {
          // escape symbol...
          dc.loadRegion(h,t,t);
          // ...climb up vine
        }
      }
      if (vine != null) {
        return vine.decodePPMD(base,dc);
      } else {
        return base.decode(dc);
      }
    }

    /** Samples a symbol from this context.
      * @param base base distribution over symbols
      * @param excl symbols to exclude
      * @param rnd random source */
    public X sample(Distribution<X> base, NodeSet<X> excl, Random rnd) {
      // FIXME: maybe, instead of going through all this trouble,
      //        we should just use "decode" with a random bit source...
      if (children != null) {
        long t = 0;
        for (Node<X> c : children) {
          if (excl == null || excl.get(c.sym) != null) {
            t += c.ucount;
          }
        }
        long z = b2*(a2*t+a1);
        long r;
        if (z <= Integer.MAX_VALUE) {
          r = rnd.nextInt((int) z);
        } else {
          r = rnd.nextLong() % z;
        }
        long m = 0;
        for (Node<X> c : children) {
          if (excl == null || excl.get(c.sym) != null) {
            m += a2*(b2*c.ucount - b1);
            if (r <= m) {
              return c.sym;
              // found and done!
            }
          }
        }
        // if we got here, we've hit the "escape symbol"... so climb up!
      }
      if (vine != null) {
        return vine.sample(base, children, rnd);
      } else {
        return base.sample(rnd);
        // FIXME: excluded character not passed to base distribution!
      }
    }

    /** Returns a clone of this context tree.
      * Deep copying is used: subnodes are cloned recursively.
      * Vine-pointers in the cloned tree are connected up like in
      * the original tree by using a relocation hashmap.
      * @param reloc relocation map */
    protected Node<X> clone(Hashtable<Node<X>,Node<X>> reloc) {
      Node<X> copy = null;
      if (vine == null) {
        copy = new Node<X>(sym, null, a1, a2, b1, b2);
      } else {
        Node<X> rvine = reloc.get(vine);
        if (rvine == null) {
          // we've started cloning from a lower part of the tree...
          // in this case, clone form above and try a look-up.
          rvine = vine.clone(reloc);
          copy = reloc.get(this);
          if (copy == null) {
            copy = new Node<X>(sym, rvine, a1, a2, b1, b2);
          } else {
            return copy;
          }
        } else {
          copy = new Node<X>(sym, rvine, a1, a2, b1, b2);
        }
      }
      // register the new mapping
      reloc.put(this,copy);
      // add missing details
      copy.ucount = ucount;
      // recursively clone all children
      if (children == null) {
        copy.children = null;
      } else {
        copy.children = new NodeHashSet<X>();
        for (Node<X> c : children) {
          Node<X> cc = c.clone(reloc);
          copy.children.add(cc);
        }
      }
      return copy;
    }
    
    /** Returns a clone of this context tree.
      * Deep copying is used: subnodes are cloned recursively.
      * Vine-pointers in the cloned tree are connected up like in
      * the original tree by using a relocation hashmap. */
    public Node<X> clone() {
      return this.clone(new Hashtable<Node<X>,Node<X>>());
      /*
      Node<X> top = this;
      // climb up the vine to find the top of the tree
      while (top.vine != null) {
        top = top.vine;
      }
      Hashtable<Node<X>,Node<X>> reloc = new Hashtable<Node<X>,Node<X>>();
      Node<X> rtop = top.clone(reloc);
      // finally, look up ourselves in the finished tree
      return reloc.get(this);
      */
    }

    /** Adds a dot-description of the current node and its links
      * to a StringBuilder instance. */
    protected void toDot(StringBuilder sb, String name,
                         Map<Node<X>,String> lookup, Node<X> flag) {
      String next = name;
      if (name == null || name.equals("")) {
        name = "ROOT";
        next = "";
      }
      if (this == flag) {
        sb.append("  "+name+" [label=\""
                      +(sym != null ? sym : "{}")+"\",style=filled];\n");
      } else {
        sb.append("  "+name+" [label=\""
                         +(sym != null ? sym : "{}")+"\"];\n");
      }
      lookup.put(this,name);
      if (this.vine != null) {
        String vname = lookup.get(this.vine);
        if (vname == null) {
          this.vine.toDot(sb,name.substring(1),lookup,flag);
          vname = lookup.get(this.vine);
        }
        sb.append("  "+name+" -> "+vname+" [style=dotted,constraint=false];\n");
      }
      if (children != null) {
        for (Node<X> n : children) {
          String cname = lookup.get(n);
          if (cname == null) {
            n.toDot(sb, next+n.sym, lookup, flag);
            cname = lookup.get(n);
          }
          sb.append("  "+name+" -> "+cname+" [label=\" "+n.sym+":"+n.ucount+" \",weight=9];\n");
        }
      }
    }

    /** Returns a dot-format description of this context tree,
      * flagging a given node. */
    public String toDot(Node<X> flag) {
      StringBuilder sb = new StringBuilder();
      sb.append("digraph PPMtree {\n");
      this.toDot(sb, "", new HashMap<Node<X>,String>(), flag);
      sb.append("}");
      return sb.toString();
    }

    /** Returns a dot-format description of this context tree. */
    public String toDot() {
      return toDot(null);
    }

  }



  /** Trains this model on a single symbol. */
  public void learn(X x) {
    context = context.update(x);
    count++;
    if (d >= maxdepth) {
      context = context.vine;
    } else {
      d++;
    }
    cachedContext = null;  // invalidate the cache
    cachedDist = null;
  }

  /** Trains this model on a sequence of symbols. */
  public void learn(Iterable<X> seq) {
    for (X x : seq) {
      learn(x);
    }
  }
  
  /** Returns the probability distribution over symbols
    * in the current context. */
  public Distribution<X> getDist() {
    // cache the distribution of the current context
    if (cachedContext != context) {
      cachedContext = context;
      cachedDist = context.getDist(base);
    }
    return cachedDist;
  }
  
  /** Returns the probability mass of the next symbol,
    * given the current context. */
  public double p(X x) {
    return context.p(x,null,base);
  }
  
  /** Returns the probability mass of the next symbol,
    * given the current context. */
  public double mass(X x) {
    return context.mass(x,null,base);
  }
  
  /** Returns the log probability mass of the next symbol,
    * given the current context. */
  public double logMass(X x) {
    return context.logMass(x,null,base);
  }
  

  /** Learns the given symbol sequence and returns its total
    * information in bits (starting from the current context). */
  public double learnAndMeasure(Iterable<X> sequence) {
    double total = 0.0;
    for (X x : sequence) {
      total += logMass(x);
      learn(x);
    }
    return - total / Tools.LN2;
  }

  /** Samples a symbol (without advancing the context). */
  public X sample(Random rnd) {
    return context.sample(base,null,rnd);
  }

  /** Samples a symbol and advances the context. */
  public X next(Random rnd) {
    //X x = getDist().sample(rnd);
    X x = context.sample(base,null,rnd);
    learn(x);   // this advances the context
    return x;
  }

  /** Returns if this distribution is defined on a finite set of
    * elements. For PPM, this property is inherited from the base
    * distribution. */
  public boolean isFinite() {
    return base.isFinite();
  }

  /** Returns a String representation of this process. */
  public String toString() {
    return "PPM(depth="+maxdepth+",alpha="
           +(double) context.a1/context.a2+",beta="
           +(double) context.b1/context.b2+") over "+base;
  }

  /** Return a Codable object of the symbol base distribution.
    * @see #base
    * @see #getCodable() */
  @SuppressWarnings("unchecked")
  public Codable<X> getBaseCodable() {
    if (base instanceof Codable) {
      return (Codable<X>) base;
    } else {
      throw new UnsupportedOperationException("uncodable base distribution");
    }
  }

  /** Return a Codable object for the current predictive
    * distribution.
    * Note that <code>getDist()</code> does not guarantee that
    * its return value implements Codable.
    * @see #getDist() */
  @SuppressWarnings("unchecked")
  public Codable<X> getCodable() {
    Distribution<X> dist = getDist();
    if (dist instanceof Codable) {
      return ((Codable <X>) dist);
    } else {
      // try to build a discrete distribution, using some trickery
      if (base instanceof Discrete) {
        return new Discrete<X>(dist,((Discrete<X>) base).values);
      } else
      if (base instanceof UniformChar) {
        return (Codable<X>) (new Discrete<Character>((Distribution<Character>) getDist(),((UniformChar) base).toDiscrete().values));
      } else {
        return null;
      }
    }
  }

  public void encode(X x, Encoder ec) {
    //Codable<X> cd = getCodable();
    //cd.encode(x,ec);
    //context.encodePPMD(x,getBaseCodable(),ec);
    context.encode(x,null,getBaseCodable(),ec);
  }
  
  public void encode(X x, Collection<X> without, Encoder ec) {
    Codable<X> cd = getCodable();
    cd.encode(x,without,ec);
  }
  
  public X decode(Decoder dc) {
    //Codable<X> cd = getCodable();
    //return cd.decode(dc);
    //return context.decodePPMD(getBaseCodable(),dc);
    return context.decode(null,getBaseCodable(),dc);
  }
  
  public X decode(Collection<X> without, Decoder dc) {
    Codable<X> cd = getCodable();
    return cd.decode(without,dc);
  }
  
  public Distribution<X> getPredictiveDistribution() {
    return getDist();
  }
  
  public static String bitsToString(double bits) {
    return ((int)(1000*bits))/1000.0+" bits"
           +"  ("+((int) Math.ceil(bits/8))+" bytes)";
  }

  private static String leadzero(String s) {
    return (s.length() == 1 ? "0"+s : s);
  }

  public static String colorToHex(double r, double g, double b) {
    return "#"
       + leadzero(Integer.toHexString((int) (255*r)))
       + leadzero(Integer.toHexString((int) (255*g)))
       + leadzero(Integer.toHexString((int) (255*b)));
  }


  /** Runs some tests / demos. */
  public static void main(String[] args) throws FileNotFoundException {
    String self = "java PPM";
    // training sequence
    String text = "";
    String desc = text;
    /* Training sequence */
    Iterable<Character> tseq = null;
    /* Evaluation sequence */
    Iterable<Character> mseq = null;
    // PPM parameters
    int maxdepth = 3;
    double alpha = 0.0;
    double beta = 0.5;
    // DOT output
    String dotfnm = null;
    Distribution<Character> base = UniformChar.ascii();
    // actions and options
    int msglength = 0;
    boolean html = false;

    // parse commandline arguments:
    for (int a=0; a<args.length; a++) {
      if (args[a].equals("-s")) {
        // set training sequence, from string
        a++;
        text = args[a];
        desc = text;
        tseq = IOTools.charSequenceFromString(text);
      } else
      if (args[a].equals("-f")) {
        // set training sequence, from file
        a++;
        String tfnm = args[a];
        desc = "<"+tfnm+">";
        tseq = IOTools.charSequenceFromFile(tfnm);
      } else
      if (args[a].equals("-d")) {
        a++;
        maxdepth = Integer.decode(args[a]);
      } else
      if (args[a].equals("-a")) {
        a++;
        alpha = Double.valueOf(args[a]);
      } else
      if (args[a].equals("-b")) {
        a++;
        beta = Double.valueOf(args[a]);
      } else
      if (args[a].equals("-g")) {
        a++;
        msglength = Integer.decode(args[a]);
      } else
      if (args[a].equals("-h")) {
        html = true;
      } else
      if (args[a].equals("-mf")) {
        a++;
        String mfnm = args[a];
        mseq = IOTools.charSequenceFromFile(mfnm);
      } else
      if (args[a].equals("-ms")) {
        a++;
        mseq = IOTools.charSequenceFromString(args[a]);
      } else
      if (args[a].equals("-u")) {
        a++;
        base = new Discrete<Character>(IOTools.charSequenceFromString(args[a]));
      } else
      if (args[a].equals("-dt")) {
        a++;
        dotfnm = args[a];
      } else
      if (args[a].equals("-h") || args[a].equals("--help")) {
        System.out.println("Usage: "+self+" [options]");
        System.out.println("\t -h, --help\t a helpful message not unlike this one");
        System.out.println("\t -a alpha  \t alpha value (e.g. "+alpha+")");
        System.out.println("\t -b beta   \t beta value (e.g. "+beta+")");
        System.out.println("\t -d N      \t maximum tree depth (e.g. "+maxdepth+")");
        System.out.println("\t -u STRING \t custom alphabet (default is "+base+")");
        System.out.println("\t -f FILE   \t train model on FILE");
        System.out.println("\t -s STRING \t train model on STRING");
        System.out.println("\t -g N      \t get N symbols from the trained model");
        System.out.println("\t -h        \t output HTML format instead of plain text (for -g)");
        System.out.println("\t -dt FILE   \t write final tree to FILE (in dot-format)");
        System.out.println("\t -mf FILE   \t measure logp of FILE (in current context)");
        System.out.println("\t -ms STRING \t measure logp of STRING (in current context)");
        return;
      } else {
        System.err.println(self+": Unknown argument '"+args[a]+"'.  Try '--help' for advice.");
        return;
      }

    }

    PPM<Character> ppm = new PPM<Character>(maxdepth, alpha, beta, base);

    if (tseq != null) {
      System.err.print("Training... ");
      ppm.learn(tseq);
      System.err.println("done.");
      System.err.println("Symbols in training: "+ppm.count);
    }
    //ppm.learn(IOTools.charSequenceFromString(text));
    //ppm.learn(IOTools.charSequenceFromFile("training.txt"));
    /*
    double logp =
        ppm.learnAndMeasure(IOTools.charSequenceFromFile("training.txt"));
    System.err.println("LOGP[e] of training sequence: "+logp);
    System.err.println("LOGP[2] of training sequence: "+logp/Tools.LN2);
    */

    //System.err.println("p(x | {}) = "+ppm.root.getDist(ppm.base));
    System.err.println(ppm);
    System.err.println("p(x | "+desc+") = "+ppm.context.getDist(ppm.base));

    //System.out.println(ppm.root.toDot());
    //System.err.println(ppm.context.toDot());
    
    long trainingcount = ppm.count;

    if (mseq != null) {
      double bits = ppm.learnAndMeasure(mseq);
      System.err.println("Information content: "+bitsToString(bits));
      System.err.println("Symbols in sequence: "+(ppm.count-trainingcount));
      System.err.println("Bits per symbol    : "+bits / (ppm.count - trainingcount));
      Distribution<Character> dist = ppm.getDist();
      if (dist instanceof Discrete) {
        ((Discrete<Character>) dist).sort();
      } else
      if (dist instanceof Mixture) {
        ((Discrete<Character>) ((Mixture<Character>) dist).dists.get(1)).sort();
      }
      System.err.println("Symbol distribution: "+dist);
    }

    /* DOT-format output. */
    if (dotfnm != null) {
      PrintStream dps = new PrintStream(dotfnm);
      dps.println(ppm.root.toDot(ppm.context));
      dps.close();
      System.err.println("Wrote DOT-output to '"+dotfnm+"'");
    }

    /* Random sampling. */
    if (msglength > 0) {
      Random r = new Random();
      System.err.println("-------------------------------------------------------------");
      System.err.print(desc);
      if (html) {
        // HTML output
        for (int j=0; j<=20; j++) {
          double p = (double) j/20;
          /*
          String color=colorToHex(p <= 0.5 ? 2*p : 1,
                                  p >  0.5 ? 1.66*(p-0.5) : 0,
                                  0.5-p/2);
          */
          String color=colorToHex(p <= 0.75 ? 1.33*p : 1,
                                  p >  0.5 ? 1.66*(p-0.5) : 0,
                                  p < 0.5 ? p*4*(1-p*2) : 0);
          System.out.print("<span style=\"background: "+color+";\">&nbsp;"
                           +p+"&nbsp;</span>");
        }
        System.out.println("<br><br>");
        for (int k=0; k<msglength; k++) {
          Character c = ppm.sample(r);
          double p = Math.exp(ppm.logMass(c));
          int size = (int) ((1-p)*200.0);
          if (size==0) { size = 1; }
          /*
          String color=colorToHex(p <= 0.5 ? 2*p : 1,
                                  p >  0.5 ? 1.66*(p-0.5) : 0,
                                  0.5-p/2);
          */
          String color=colorToHex(p <= 0.75 ? 1.33*p : 1,
                                  p >  0.5 ? 1.66*(p-0.5) : 0,
                                  p < 0.5 ? p*4*(1-p*2) : 0);
          String pre = "<span style=\"color: "+color+";\">";
          String suf = "</span>";
          switch (c) {
          case '\"': System.out.print(pre+"&quot;"+suf); break;
          case '<': System.out.print(pre+"&lt;"+suf); break;
          case '>': System.out.print(pre+"&gt;"+suf); break;
          case '\n':
          // System.out.print("<br>"); break;
             System.out.println(pre+"⏎"+suf+"<br>"); break;
          default: 
            System.out.print(pre+c+suf);
          }
          ppm.learn(c);
        }
      } else {
        // plain text output
        for (int k=0; k<msglength; k++) {
          System.out.print(ppm.next(r));
        }
      }
      System.err.println();
      System.err.println("-------------------------------------------------------------");
    }
  }

}

