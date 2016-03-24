/* Automated copy from build process */
/* $Id: RSTree.java,v 1.31 2014/08/07 13:35:34 chris Exp $ */
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Vector;
import java.util.Random;
import java.util.Hashtable;
import java.util.HashMap;
import java.util.Map;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

/** Reverse suffix tree + sequence memoizer. */
public class RSTree<X> extends SimpleMass<X>
                           implements AdaptiveCode<X> {



  public static void debugout(String s) {
    System.err.print(s);
  }
  public static void debugoutln(String s) {
    System.err.println(s);
  }

  /** Representation of contexts used in the reverse suffix tree. */
  public static class Context<X> {
    /** Reference to a shared append-only list. */
    List<X> data = null;
    /** Start position (inclusive). */
    int src = 0;
    /** End position (exclusive). */
    int end = 0;

    /** Constructs an empty context. */ 
    public Context() {
      data = new ArrayList<X>();
    }
    /** Constructs a context initialised to a given List. */
    public Context(List<X> l) {
      this.data = l;
      this.src = 0;
      this.end = l.size();
    }
    /** Constructs a context initialised to a given List and bounds.
      * @param l list
      * @param start position of first symbol (inclusive)
      * @param stop position after last symbol */
    public Context(List<X> l, int start, int stop) {
      if (l != null && start >= 0 && stop <= l.size() && start <= stop) {
        this.data = l;
        this.src = start;
        this.end = stop;
      } else {
        throw new IllegalArgumentException("invalid bounds");
      }
    }
    /** Returns the length of this context. */
    public int length() {
      return end-src;
    }
    /** Returns the kth element. */
    public X get(int k) {
      if (src+k < end) {
        return data.get(src+k);
      } else {
        throw new NoSuchElementException("out of permitted bounds");
      }
    }
    /** Returns a subcontext from a start position (inclusive)
      * to an end position (exclusive). */
    public Context<X> subContext(int a, int z) {
      return new Context<X>(data,src+a,src+z);
    }
    /** Constructs a Character-Context from a String. */
    public static Context<Character> fromString(String s) {
      ArrayList<Character> al = new ArrayList<Character>();
      for (char c : s.toCharArray()) {
        al.add(c);
      }
      return new Context<Character>(al);
    }
    /** Returns a String representation of the current context. */
    public String toString() {
      StringBuilder sb = new StringBuilder();
      for (int k=src; k<end; k++) {
        sb.append(data.get(k).toString());
      }
      return sb.toString();
    }
  }

//   /** Interface representing sets of nodes. */
//   private interface NodeSet<X> extends Iterable<Node<X>> {
//     /** Retrieve a node matching a given symbol. */
//     public Node<X> get(X sym);
//     /** Adds a node to the set, with given link label. */
//     public void add(X x, Node<X> node);
//     /** Replaces one node with another. */
//     public void replace(Node<X> n1, Node<X> n2);
//     /** Returns a collection over symbols supported by nodes in
//       * this set. */
//     public Collection<X> symbols();
//   }

  /** Class implementing a NodeSet based on a Hashtable. */
  public class NodeHashSet<X> extends Hashtable<X,Node<X>> {
    public void add(X x, Node<X> node) {
      put(x, node);
    }
    public void replace(Node<X> n1, Node<X> n2) {
      for (X x : keySet()) {
        if (get(x) == n1) {
          remove(x);
          put(x,n2);
          break;
        }
      }
    }
    public Iterator<Node<X>> iterator() {
      return values().iterator();
    }
    public Collection<X> symbols() {
      return keySet();
    }
  }
  
  /** Symbol counts based on a Hashtable. */
  public static class Counts<X> extends Hashtable<X,Integer> {
    long total = 0;
    /** Constructs a Counts object with all counts initialised to zero. */
    public Counts() {
      super();
      this.total = 0;
    }
    /** Constructs a Counts object from another Counts object. */
    public Counts(Counts<X> counts) {
      super(counts);
      this.total = counts.total;
    }
    /** Increment the count of a symbol by some value. */
    public void inc(X x, int k) {
      Integer i = get(x);
      if (i != null) {
        put(x,i+k);
      } else {
        put(x,k);
      }
      total += k;
    }
    /** Set the count of a symbol to a particular value. */
    public void set(X x, int k) {
      Integer i = get(x);
      put(x,k);
      if (i != null) {
        total += k-i;
      } else {
        total += k;
      }
    }
    /** Retrieve the count for a given symbol. */
    public int count(X x) {
      Integer i = get(x);
      return (i != null) ? i : 0;
    }
    /** Return the total count of all symbols. */
    public long countAll() {
      return total;
    }
    public Counts<X> clone() {
      return new Counts<X>(this);
    }
  }
  
  /** Symbol probabilities based on a Hashtable. */
  public static class Probs<X> extends Hashtable<X,Double> {
    /** Sets the probability of a symbol. */
    public void set(X x, double p) {
      put(x,p);
    }
    /** Check if probabilities sum to one. */
    public void check() {
      double sum = 0.0;
      for (Map.Entry<X,Double> e : entrySet()) {
        sum += e.getValue();
      }
      if (sum < 0.95) {
        debugoutln("probabilities not summing to 1: "+sum);
      }
    }
    /** Returns a String representation. */
    public String toString() {
      int size = size();
      return "DiscreteHashtable["+size+"]";
    }
  }


  public int nodes = 0;

  /** Class of tree nodes in a reverse suffix tree. */
  class Node<X> {

    Context<X> context;

    /** Children of this reverse suffix tree node.
      * The children are LONGER contexts which have this node's context
      * as a suffix. */
    NodeHashSet<X> children;

    /** Customer counts, for each symbol.
        The customer counts recall how often which symbols were seen
        AFTER the context of this node. */
    Counts<X> c;
    /** Table counts, for each symbol. */
    Counts<X> t;

    /** Constructs a new node for the supplied context. */
    public Node(Context<X> ctx) {
      this.context = ctx;
      this.children = new NodeHashSet<X>();
      this.c = new Counts<X>();
      this.t = new Counts<X>();
    }

    /** Returns the node having the longest matching suffix
      * to the supplied context string. */
    public Node<X> find(Context<X> ctx) {
      // find largest matching suffix
      int k = context.length()-1;
      int j = ctx.length()-1;
      while (k >= 0 && j >= 0 && context.get(k).equals(ctx.get(j)) ) {
        k--;
        j--;
      }
      // if (k == -1), proper suffix, search children
      if (k == -1) {
        X last = ctx.get(j);
        Node<X> rec = children.get(last);
        if (rec != null) {
          // recurse on child
          return rec.find(ctx);
        } else {
          // or add new child
          Node<X> newnode = new Node<X>(ctx);
          children.add(last, newnode);
          return newnode;
        }
      } else {
        // otherwise, return this node
        return this;
      }
    }

    public String toString() {
      return "("+context.toString()+",C="+c.countAll()+",T="+t.countAll()+")";
    }

    private void toDot(StringBuilder sb) {
      String hc = "n"+this.hashCode();
      String hl = "("+context.toString()+")";
      String hh = c.toString();
      sb.append("  "+hc+" [label=\""+hl+"\\n"+c.countAll()+": "+hh+"\"];\n");
      for (X x : children.symbols()) {
        String sym = x.toString();
        Node<X> cn = children.get(x);
        String cc = "n"+cn.hashCode();
        sb.append("  "+hc+" -> "+cc+" [label=\" "+sym+" \"];\n");
        cn.toDot(sb);
      }
    }
    
    /** Exports a dot graph description of this subtree. */
    public String toDot() {
      StringBuilder sb = new StringBuilder();
      sb.append("digraph RSTree {\n");
      sb.append("  node [shape=box];\n");
      toDot(sb);
      sb.append("}\n");
      return sb.toString();
    }
    
    private void toASCII(StringBuilder sb, String pre) {
      String hl = "("+context.toString()+")";
      String hh = c.toString();
      sb.append(pre);
      sb.append(hl);
      sb.append(" ");
      sb.append(hh);
      sb.append("\n");
      for (X x : children.symbols()) {
        String sym = x.toString();
        Node<X> cn = children.get(x);
        cn.toASCII(sb," "+pre);
      }
    }
    
    public String toASCII() {
      StringBuilder sb = new StringBuilder();
      toASCII(sb,"");
      return sb.toString();
    }

    /** Traverses the tree to insert a new context, storing the
      * path back to the root in the supplied vector. */
    public void insert(Vector<Node<X>> path, Context<X> ctx) {
      // find largest matching suffix
      int k = context.length()-1;
      int j = ctx.length()-1;
      //X x = ctx.get(ctx.length()-1);
      while (k >= 0 && j >= 0 && context.get(k).equals(ctx.get(j))) {
        k--;
        j--;
      }
      // if (k == -1), proper suffix, search children
      if (k == -1 && j >= 0) {
        //System.err.println("OK: ("+ctx+") is longer than ("+context+")");
        X last = ctx.get(j);
        Node<X> rec = children.get(last);
        if (rec != null) {
          //debugoutln("INS->rec_child");
          // recurse on child
          //System.err.println("Recursing on child node '"+last+"'");
          // TODO: check if this can be done more efficiently,
          //       for example iteratively, without resetting j.
          path.add(this);
          rec.insert(path,ctx);
        } else {
          //debugoutln("INS->add_child "+path);
          // or add new child
          //System.err.println("Adding child ("+ctx+") to ("+context+") with '"+last+"'");
          Node<X> newnode = new Node<X>(ctx);
          children.add(last, newnode);
          path.add(this);
          path.add(newnode);
          //debugoutln("INS<-add_child "+path);
        }
      } else
      if (k == -1 && j == -1) {
        // node already contained
        //debugoutln("INS->we're in the right place: "+ctx);
        path.add(this);
      } else {
        //debugoutln("INS->split");
        //System.err.println("INS->split");
        // otherwise, split current node.
        // extract longest common suffix
        Context<X> subctx = context.subContext(k+1,context.length());
        //System.err.println("Splitting ("+context+") at "+(k+1)+": breaking off ("+subctx+")...");
        Node<X> split = new Node<X>(subctx);
        // The new parent node has the same tables, but only one
        // customer at each table.
        split.t = this.t.clone();
        for (X x : split.t.keySet()) {
          //System.err.println("Setting count of "+x+" from "+split.c.count(x)+" to 1");
          split.c.set(x,1);
        }
        Node<X> newnode = new Node<X>(ctx);
        split.children.add(ctx.get(j), newnode);
        split.children.add(context.get(k), this);
        //X xf = context.get(k);
        //split.c.set(xf, this.c.count(xf));
        // splits can only occur in nodes below the root.
        Node<X> parent = path.get(path.size()-1);  // get parent
        parent.children.replace(this,split);
        //path.add(this);
        //path.set(path.size()-1, split);
        //System.err.println("Now ("+subctx+") has children ("+context+") and ("+ctx+")");
        path.add(split);
        path.add(newnode);
        /*
        double d = getDiscount(this.context.length()-1);
        double newd = getDiscount(newnode.context.length()-1);
        for (X x : this.children.keySet()) {
          int cc = c.count(x);
          int tt = t.count(x);
          if (cc != 0) {
            ArrayList<Integer> phi = partition(cc, tt, d);
            newnode.t.set(x,this.t.count(x));
            this.t.set(x,0);
            for (int h=0; h<phi.size(); h++) {
              int a = drawNewT(phi.get(h), d/newd, -d);
              this.t.inc(x,a);
            }
            newnode.c.set(x,this.t.count(x));
          } else {
            // what?
          }
        }
        */
      }
    }

    /** Checks this subtree for count consistency. */
    public boolean check() {
      // count tables of all children
      int tt = 0;
      for (Map.Entry<X,Node<X>> e : children.entrySet()) {
        tt += e.getValue().t.countAll();
      }
      long cc = c.countAll();
      boolean ok = false;
      if (tt == cc || tt == (cc-1)) {
        // all is ok
        ok = true;
        for (Map.Entry<X,Node<X>> e : children.entrySet()) {
          ok &= e.getValue().check();
        }
      } else {
        ok = false;
        System.err.println("Non-matching counts ("+cc+") at node "+this.context+":");
        System.err.println("Children have "+tt+" tables in total.");
      }
      return ok;
    }

    private int drawNewT(int n, double d, double c) {
      int t = 1;
      for (int i=2; i<=n; i++) {
        double p = (t*d+c) / ((double) i-1+c);
        Bernoulli<Boolean> b = Bernoulli.booleans(p);
        if (b.sample(fixrnd)) {
          t++;
        }
      }
      return t;
    }

    private ArrayList<Integer> partition(int c, int t, double d) {
      //ArrayList<ArrayList<Double>> m = new ArrayList<ArrayList<Double>>();
      if (c == 0 || t == 0) {
        throw new IllegalStateException("attempt to partition with c="+c+" and t="+t);
      }
      double[][] m = new double[t+1][c+1];
      m[t][c]=1.0;
      for (int j=c-1; j>0; j--) {
        for (int i=1; i<t; i++) {
          m[i][j] = m[i+1][j+1] + m[i][j+1]*((double) j - i*d);
        }
        m[t][j] = m[t][j+1]; // FIXME: m[d][j] -- mistake in paper?
      }
      ArrayList<Integer> phi = new ArrayList<Integer>(t); // capacity=t
      phi.add(0);
      phi.add(1);
      for (int h=1; h<=t; h++) {
        phi.add(0);
      }
      /* the original paper seems to be written in a sloppy fashion,
       * for example, no mention is made of k's initial value, and
       * there seems to be a side condition missing in the second loop. */
      int k = 1;
      for (int j=2; j<c && k<t; j++) {
        m[k][j] = m[k][j]+((double) j-1-k*d);
        boolean grow = false;
        if (m[k+1][j] > 0) {
          double p = m[k+1][j] / (m[k+1][j] + m[k][j]);
          Bernoulli<Boolean> b = Bernoulli.booleans(p);
          grow = b.sample(fixrnd);
        }
        if (grow) {
          k++;
          phi.set(k,1);
        } else {
          double[] pp = new double[k];
          double z = j - 1 - (k*d);
          double zz = 0.0;
          for (int h=1; h<k; h++) {
            pp[h] = (phi.get(h)-d);
            zz += pp[h];
          }
          if (zz > 0.0) {
            int l = Samplers.sampleIndex(fixrnd,pp,zz);
            phi.set(l, phi.get(l)+1);
          }
        }
      }
      phi.remove(0);
      return phi;
    }
  }



  public static <X> Probs<X> uniform(Iterable<X> symbols) {
    Probs<X> base = new Probs<X>();
    int count = 0;
    for (X x : symbols) {
      count++;
    }
    double p = 1.0 / count;
    for (X x : symbols) {
      base.set(x,p);
    }
    return base;
  }


  /** Root strength parameter.
    * The strength of CRPs below the root node decays as
    * described in Gasthaus (2010a). */
  double alpha = 0.0;


  /** Return the root node's (global) strength parameter. */
  public double getStrength(int depth) {
    double a = alpha; // root strength
    if (a != 0.0) {
      for (int d=1; d<=depth; d++) {
        a *= getDiscount(d);
      }
    }
    return a;
  }


  /** Discount parameters, in order of ascending context length.
    * The first discount factor is for context depth 0.
    * The final discount is used for all subsequent depths.
    * Context depth means context length, not node depth in the
    * reverse suffix tree (many symbols can be summarised in one
    * node). */
  double[] discounts = { 0.62, 0.69, 0.74, 0.80, 0.95 };
  // double[] discounts = { 0.5, 0.7, 0.8, 0.82, 0.84, 0.88,
  //                        0.91, 0.92, 0.93, 0.94, 0.95 };
  // double[] discounts = { 0.5 };


  /** Return the discount parameter for a given context depth. */
  public double getDiscount(int depth) {
    if (depth < discounts.length) {
      return discounts[depth];
    } else {
      return discounts[discounts.length-1];
    }
  }

  /** Compute symbol probabilities for each node in path. */
  public Vector<DiscreteLookup<X>> pathProb(Vector<Node<X>> path,
                                            Mass<X> base,
                                            Iterable<X> symbols) {
    Vector<DiscreteLookup<X>> pv = new Vector<DiscreteLookup<X>>();
    Mass<X> parentp = base;
    Node<X> pnode = null;
    int k = 0;
    Iterable<X> syms = symbols;
    //syms = path.get(0).children.symbols();   // MAJOR speed-up
    for (Node<X> u : path) {
    //for (int i=path.size()-1; i>=0; i--) {
    //for (int i=0; i<path.size(); i++) {
      //Node<X> u = path.get(i);
      //double d = getDiscount(u.context.length());
      //double d = getDiscount(k); k++;
      double d = 1.0;
      double a;
      // Compute the product of all discounts between
      // start and end symbol of the node's context.
      // NOTE: This is not properly explained in (gasthaus2010a).
      if (pnode != null) {
        //a = getStrength(pnode.context.length());
        //a *= getDiscount(u.context.length()+1);
        a = getStrength(u.context.length());
        for (int m = pnode.context.length(); m < u.context.length(); m++) {
          d *= getDiscount(m+1);
        }
      } else {
        a = getStrength(0);
        d = getDiscount(0);
      }
      //double d = 0.5;
      Hashtable<X,Double> p = new Hashtable<X,Double>();
      long uc = u.c.countAll();
      long ut = u.t.countAll();
      for (X x : syms) {
        double r;
        if (uc != 0) {
          /* r = (double) ((u.c.count(x) - d*u.t.count(x))
                          + ((d*ut)*parentp.mass(x))) / ((double) uc); */
          r = (double) ((u.c.count(x) - d*u.t.count(x))
                          + ((a+d*ut)*parentp.mass(x))) / ((double) uc + a);
        } else {
          r = parentp.mass(x);
        }
        p.put(x,r);
      }
      //p.check();  // check it sums to one
      DiscreteLookup<X> dl = new DiscreteLookup<X>(p);
      //dl.check();
      pv.add(dl);
      parentp = dl;
      pnode = u;
    }
    return pv;
  }
  
  /** Computes the mass of a given symbol, given the current context path. */
  public double pathProb(X x, Vector<Node<X>> path, Mass<X> base) {
    /* for consistency testing:
    System.err.print("!");
    Vector<DiscreteLookup<X>> dl = pathProb(path, base, symbols);
    if (true) {
      return dl.get(dl.size()-1).mass(x);
    }
    */
    double parentm = base.mass(x);
    Node<X> pnode = null;
    Iterable<X> syms = symbols;
    //syms = path.get(0).children.symbols();   // MAJOR speed-up
    for (Node<X> u : path) {
      double d = 1.0;
      double a;
      // Compute the product of all discounts between
      // start and end symbol of the node's context.
      // NOTE: This is not properly explained in (gasthaus2010a).
      if (pnode != null) {
        //if (a != 0.0) { a *= getDiscount(u.context.length()+1); }
        //a = alpha;
        //a *= getDiscount(u.context.length()+1);
        //a = getStrength(u.context.length());
        a = getStrength(pnode.context.length()); // FIXME
        //a = getStrength(u.context.length()+1);
        for (int m = pnode.context.length(); m < u.context.length(); m++) {
          d *= getDiscount(m+1);
          a *= getDiscount(m);
        }
        // FIXME: the calculation of "a" is not correct except for a=0.
        // Gasthaus' paper is not very helpful, and neither is his code;
        // also, what the code does doesn't seem to correspond to 
        // what the paper says.  Maybe let's just leave alpha at zero.
      } else {
        a = getStrength(0);
        d = getDiscount(0);
      }
      long uc = u.c.countAll();
      long ut = u.t.countAll();
      double r;
      if (uc != 0) {
        /* r = (double) ((u.c.count(x) - d*u.t.count(x))
                          + (d*ut*parentp)) / ((double) uc); */
        r = (double) ((u.c.count(x) - d*u.t.count(x))
                        + ((a+d*ut)*parentm)) / ((double) uc + a);
      } else {
        r = parentm;
      }
      pnode = u;
      parentm = r;
    }
    return parentm;
  }
  
  public void updatePath(Vector<Node<X>> path, X x) {
    for (int k=path.size()-1; k>=0; k--) {
    //for (int k=0; k < path.size()-1; k++) {
      Node<X> u = path.get(k);
      u.c.inc(x,1);
      if (u.t.count(x) == 0) {
        u.t.inc(x,1);
      } else {
        //debugoutln("!");
        return;
      }
    }
    //debugoutln("");
  }

  
  public void updatePath(Vector<Node<X>> path, Vector<DiscreteLookup<X>> probs, X x) {
    //debugout("Updating path: ");
    for (int k=path.size()-1; k>=0; k--) {
    //for (int k=0; k < path.size()-1; k++) {
      Node<X> u = path.get(k);
      //System.err.println("Updating count "+x+":"+u.c.count(x)+" in ("+u.context+")...");
      //debugout(u+"{"+x+"++} ");
      //Probs<X> p = probs.get(k);
      u.c.inc(x,1);
      if (u.t.count(x) == 0) {
        u.t.inc(x,1);
      } else {
        //debugoutln("!");
        return;
      }
    }
    //debugoutln("");
  }
  
  public static <X> Counts<X> probsToCounts(Probs<X> p) {
    Counts<X> c = new Counts<X>();
    // find minimum
    double min = Double.POSITIVE_INFINITY;
    for (X x : p.keySet()) {
      double m = p.get(x);
      if (m < min) {
        min = m;
      }
    }
    // find fraction for minimum
    int b = (int) ((double) 1.0 / min);
    for (X x : p.keySet()) {
      double m = p.get(x);
      c.set(x,(int) (m*b));
    }
    return c;
  }


  public static <X> void encode(Probs<X> p, X x, Encoder ec) {
    //Cumulative<X> cm = new Cumulative<X>(p,p.keySet());
    DiscreteLookup<X> dl = new DiscreteLookup<X>(p);
    Codable<X> cd = new CumulativeOnDemand<X>(dl,dl);
    //Codable<X> cd = new CumulativeLookup<X>(dl,dl);
    cd.encode(x,ec);
  }
  
  public static <X> X decode(Probs<X> p, Decoder dc) {
    //Cumulative<X> cm = new Cumulative<X>(p,p.keySet());
    //return cm.decode(dc);
    DiscreteLookup<X> dl = new DiscreteLookup<X>(p);
    //Codable<X> cd = new CumulativeOnDemand<X>(dl,dl);
    Codable<X> cd = new CumulativeLookup<X>(dl,dl);
    return cd.decode(dc);
  }


  /* --------------------------------------------------------------------- */

  /** An iterable collection of symbols. */
  public Iterable<X> symbols;
  
  /** Base probability masses. */
  public Mass<X> base;
  
  /** Internal reverse suffix tree. */
  public Node<X> tree = new Node<X>(new Context<X>()); // empty tree

  /** Complete context of everything seen so far. */
  public ArrayList<X> history = new ArrayList<X>();

  /** Last node path through the tree. */
  Vector<Node<X>> path = null;

  /** Set of symbol probabilities for each node in node path. */
  Vector<DiscreteLookup<X>> pv = null;

  /** Current predictive distribution. */
  public DiscreteLookup<X> predictive;

  /** Pseudo-random number generator with fixed random seed. */
  public Random fixrnd;


  public RSTree(Iterable<X> symbols) {
    this.symbols = symbols;
    DiscreteLookup<X> dl = new DiscreteLookup<X>(symbols);
    this.base = dl;
    this.predictive = dl;
    this.fixrnd = new Random(577);
    this.path = new Vector<Node<X>>();
    this.path.add(tree);
  }
  
  public RSTree(Mass<X> base, Iterable<X> symbols) {
    this.symbols = symbols;
    this.base = base;
    this.predictive = new DiscreteLookup<X>(base,symbols);
    this.fixrnd = new Random(577);
    this.path = new Vector<Node<X>>();
    this.path.add(tree);
  }
  
  public RSTree(Mass<X> base, Iterable<X> symbols, double[] disc) {
    this(base,symbols,0.0,disc);
  }
  
  public RSTree(Mass<X> base, Iterable<X> symbols,
                              double strength, double[] disc) {
    this.symbols = symbols;
    this.base = base;
    this.alpha = strength;
    this.predictive = new DiscreteLookup<X>(base,symbols);
    this.discounts = disc;
    this.fixrnd = new Random(577);
    this.path = new Vector<Node<X>>();
    this.path.add(tree);
  }
  
  /** Constructs a new RSTree with given parameters.
    * <br>
    * Example parameter string: "<tt>b=0.5,0.6,0.95</tt>".
    * <dl><dt><b>Parameters and values:</b></dt><dd><ul>
    *   <li>field: <b>b</b>, type: double, discount parameters (beta).</li>
    * </ul></dd></dl>
    * @param pars parameters in String form
    * @param base base distribution over symbols */
  public static <X> RSTree<X> createNew(Distribution<X> base, Iterable<X> symbols, String pars) {
    // defaults:
    double alpha = 0.0;
    double beta = 0.5;
    ArrayList<Double> betas = null;
    int maxdepth = -1;
    // now parse arguments
    String[] s = pars.split(":");
    for (int k=0; k<s.length; k++) {
      int eq = s[k].indexOf('=');
      if (eq != -1) {
        String key = s[k].substring(0,eq);
        String val = s[k].substring(eq+1);
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
            betas = new ArrayList<Double>();
            betas.add(Double.valueOf(val));
          }
        } else
        if (key.equals("a")) {
          // global ALPHA parameter (strength)
          alpha = Double.valueOf(val);
        } else {
          System.err.println("Warning: RSTree: unknown parameter \""+s[k]+"\"");
        }
      }
    }
    // Now create and return a matching RSTree
    double[] disc = new double[betas.size()];
    for (int k=0; k<betas.size(); k++) {
      disc[k]=betas.get(k);
    }
    return new RSTree<X>(base,symbols,alpha,disc);
  }

  
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("[");
    for (int k=0; k<discounts.length-1; k++) {
      sb.append(discounts[k]+", ");
    }
    sb.append(discounts[discounts.length-1]+"]");
    return "SeqMem(α="+alpha+", d="+sb.toString()+") over "+base.toString();
  }

  public X sample(Random rnd) {
    if (predictive == null) {
      predictive = getPredictiveDistribution();
    }
    return predictive.sample(rnd);
  }

  
  public void encode(X x, Encoder ec) {
    //Cumulative<X> cm = new Cumulative<X>(predictive,predictive.keySet());
    //Cumulative<X> cm = new Cumulative<X>(predictive,symbols);
    //cm.encode(x,ec);
    if (predictive == null) {
      predictive = getPredictiveDistribution();
    }
    //Codable<X> code = new CumulativeOnDemand<X>(predictive,symbols);
    //code.encode(x,ec);
    long budget = ec.getRange();
    HashMap<X,Long> cm = Coding.getDiscreteMass(predictive,symbols,budget);
    Coding.encode(x,cm,symbols,ec);
  }

  public X decode(Decoder dc) {
    //Cumulative<X> cm = new Cumulative<X>(predictive,predictive.keySet());
    //Cumulative<X> cm = new Cumulative<X>(predictive,symbols);
    //return cm.decode(dc);
    //Codable<X> code = new CumulativeLookup<X>(predictive,symbols);
    if (predictive == null) {
      predictive = getPredictiveDistribution();
    }
    //Codable<X> code = new CumulativeOnDemand<X>(predictive,symbols);
    //return code.decode(dc);
    long budget = dc.getRange();
    HashMap<X,Long> cm = Coding.getDiscreteMass(predictive,symbols,budget);
    return Coding.decode(cm,symbols,dc);
  }
 
  public void encode(X x, Collection<X> omit, Encoder ec) {
    throw new UnsupportedOperationException();
  }

  public X decode(Collection<X> omit, Decoder dc) {
    throw new UnsupportedOperationException();
  }

  public void updateLastPath(X lastx) {
    if (path != null) {
      /*
      X lastx = null;
      if (path.size() > 0) {
        Node<X> lastnode = path.get(path.size()-1);
        int l = lastnode.context.length();
        if (l > 0) {
          lastx = lastnode.context.get(l-1);
        }
      }
      */
      if (lastx != null) {
        // update previous path
        updatePath(path,pv,lastx);
      } else {
        //System.err.println("UPDATE("+lastx+") -> there's a path, but no last symbol");
      }
    } else {
      System.err.println("UPDATE(?) -> nullpath");
      path = new Vector<Node<X>>();
      path.add(tree);
      //updatePath(path,pv,x);
    }
  }

  X lastx = null;


  public void learn(X x) {
    /* Learns from observation x[n+1].
       Current predictive distribution is over x[n+1].
       Current path Q is for context x[0]..x[n]
         (1) update Q to increment the counts of x[n+1]
         (2) insert context x[0]..x[n+1], getting new path Q.
         (3) use Q to compute the predictive distribution over x[n+2]
       Current path Q is for context x[0]..x[n+1]
       Current predictive distribution is over x[n+2]
    */
    //Vector<Node<X>> oldpath = path;
    //System.err.println("LEARN("+x+")");
    //System.err.println("Learning: "+x+" ["+path+"] "+predictive.get(x));
    //updatePath(path, pv, x);
    //predictive = null; // FIXME -- just for testing
    updatePath(path, x);
    //System.err.println("LEARNED: "+x+" <<"+path+">> ");
    // construct new path
    path = new Vector<Node<X>>();
    history.add(x);
    tree.insert(path, new Context<X>(history));
    //tree.check();
    // path now contains the nodes which led to the longest prefix
    // of 'history'. 
    // compute symbol probabilities at each node

    //pv = pathProb(path,base,symbols);
    // get new predictive distribution over next symbol
    //predictive = pv.get(pv.size()-1);
    predictive = null;
    //System.err.println("Learned : "+x+" ["+path+"] "+predictive.get(x));
    // Finally, update the counts
    //updatePath(oldpath, pv, x);
  }

  public String getStateInfo() {
    return path.get(path.size()-1).toString();
  }

  public double mass(X x) {
    if (predictive == null) {
      return pathProb(x,path,base);
    } else {
      return predictive.mass(x);
    }
  }
  
  public double logMass(X x) {
    if (predictive == null) {
      return Math.log(pathProb(x,path,base));
    } else {
      return Math.log(predictive.mass(x));
    }
  }
  
  public DiscreteLookup<X> getPredictiveDistribution() {
    if (predictive == null) {
      pv = pathProb(path,base,symbols);
      predictive = pv.get(pv.size()-1);
    }
    return predictive;
  }


  public static void main(String[] args) {
    UniformChar bb = UniformChar.asciiPrintable();
    RSTree<Character> sm = new RSTree<Character>(bb,bb,new double[] { 0.5 } );
    System.out.println("Model = "+sm);
    //Probs<Character> basep = uniform(bb);
    //Context<Character> empty = new Context<Character>();
    //Node<Character> tree = new Node<Character>(empty);
    // gradually increasing context
    ArrayList<Character> ctx = new ArrayList<Character>();
    //String s = "bananinbana";
    //String s = "yabbabybabaya";
    //String s = "ABCDEFABCDEFABCDEF";
    //String s = "abcdefabcdefxabcdefabcdef";
    //String s = "this_is_a_sentence_which_does_not_repeat_itself_even_though_it_is_rep";
    //String s = "this_is_a_sentence_which_does";
    //String s = "this_is_a_sense";
    String s = "abcdefabcdefabcdef";
    //String s = "abcdebcdabcde";
    //String s = "AAAAAABAAAAAABAAAAAAB";
    Iterable<Character> seq = IOTools.charSequenceFromString(s);
    boolean printlabels = true;
    Histogram h = null;
    for (Character c : seq) {
      //Counts<Character> cnts = probsToCounts(sm.predictive);
      //h = Histogram.fromChars(cnts);
      if (printlabels) {
        //System.err.print("  ");
        //h.printLabels(System.err);
        printlabels=false;
      }
      //h.print(System.err,1);
      //System.err.print(" "+c);
      //System.err.print(" "+sm.predictive.get(c)+"\n");
      //System.err.println(" Log prob of "+c+" is "+sm.getPredictiveDistribution().logMass(c)/Tools.LN2+", "+sm.path);
      System.err.println(" LOG MASS of "+c+" is "+sm.logMass(c)/Tools.LN2+", "+sm.path);
      sm.learn(c);
    }
    System.out.println("-----------------------------------");
    /*
    RSTree<Integer> sm2 = new RSTree<Integer>(ByteCompressor.base,
                                              ByteCompressor.base,
                                              new double[] { 0.5 } );
    System.out.println("Model = "+sm2);
    Integer[] nums = { 0+0x61, 0+0x62, 0+0x63, 0+0x64, 0+0x65, 0+0x66,
                       0+0x61, 0+0x62, 0+0x63, 0+0x64, 0+0x65, 0+0x66,
                       0+0x61, 0+0x62, 0+0x63, 0+0x64, 0+0x65, 0+0x66 };
    Iterable<Integer> seq2 = Arrays.asList(nums);
    for (Integer b : seq2) {
      System.err.println(" LOG MASS of "+b+" is "+sm2.logMass(b)/Tools.LN2+", "+sm2.path);
      sm2.learn(b);
    }
    System.out.println("-----------------------------------");
    */
    //sm.updateLastPath();
    //System.out.println(sm.predictive);
    //Counts<Character> c2 = probsToCounts(sm.predictive);
    //Histogram h2 = Histogram.fromChars(c2);
    //h2.print(System.err,1);
    //System.err.print("  ");
    //h2.printLabels(System.err);
    //System.out.print(sm.tree.toDot());
    //System.out.print(sm.tree.toASCII());
    //System.out.println("PATH: "+path);
  }

}

