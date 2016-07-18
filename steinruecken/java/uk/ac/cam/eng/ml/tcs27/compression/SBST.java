/* $Id$ */
package uk.ac.cam.eng.ml.tcs27.compression;

import java.util.Random;
import java.util.Collection;
import java.util.TreeSet;
import java.util.NavigableSet;

/** A stochastic binary search tree, as a discrete stochastic process
  * over a fixed range of integers.
  * Each draw from the process is a random walk down a binary search
  * tree, where the decision to branch left or right is determined
  * by a Bernoulli coin flip.  The bias of each coin is updated
  * every time a sample is drawn. */
public class SBST extends SimpleMass<Integer>
                                implements AdaptiveCode<Integer> {

  /** Internal binary search tree. */
  Node tree = null;

  /** Number of observations / samples drawn from this process. */
  int n = 0;

  short g1 = 1;
  short g2 = 2;

  class Node {

    int min;
    int max;
    int mid;
    /** Subtree of elements less than <var>mid</var>. */
    Node le;
    /** Subtree of elements greater than or equal to <var>mid</var>. */
    Node ge;
    /** Stochastic process over branch decisions. */
    BernoulliProcess<Boolean> proc = null;

    // cached data
    NavigableSet<Integer> cached_elts = null;
    double cached_mass = 0.0;

    /** Constructs a new tree node.
      * @param min inclusive minimum
      * @param max inclusive maximum
      * @param le tree branch for lesser integers (min..mid-1)
      * @param ge  tree branch for greater or equal integers (mid..max) */
    public Node(int min, int max, Node le, Node ge) {
      this.min = min;
      this.max = max;
      this.mid = min + (max-min+1)/2;
      //System.err.println("\033[36mCreating node ("+min+","+mid+","+max+")\033[m");
      this.le = le;
      this.ge = ge;
      //this.proc = new BernoulliProcess<Boolean>(true,false);
      this.proc = new BernoulliProcess<Boolean>(false,true, g1,g2, g1,g2);
//      if (max == min) {
//        // One-element node, there's no choice.
//        // By convention, always take the 'right-hand' branch (true)
//        this.proc = new BernoulliProcess<Boolean>(false, true,
//                                                  (short)0, (short)1, (short)1, (short)1);
//      } else {
//        this.proc = new BernoulliProcess<Boolean>(false,true, g1,g2, g1,g2);
//      }
    }

    /** Samples an integer from the range <var>min</var> to <var>max</var>
      * (inclusive).
      * Instantiates new branches of the tree as needed. */
    public int sample(Random rnd) {
      boolean b = (proc.sample(rnd));
      if (max-min > 1) {
        if (b) {
          if (ge == null) {
            ge = new Node(mid, max, null, null); 
          }
          return ge.sample(rnd);
        } else {
          if (le == null) {
            le = new Node(min, mid-1, null, null); 
          }
          return le.sample(rnd);
        }
      } else {
        return b ? max : min;
      }
    }

    /** Learns a new integer and updates the tree accordingly. */
    public void learn(int k) {
      cached_elts = null; // invalidate cache

      if (max-min > 1) {
        if (k < mid) {
          //System.err.println("\033[36mLearning le...\033[m");
          //System.err.println("\033[30;1mLearning "+k+", < "+mid+"\033[m");
          proc.learn(false);
          if (le == null) {
            le = new Node(min, mid-1, null, null); 
          }
          le.learn(k);
        } else {
          //System.err.println("\033[31mLearning ge...\033[m");
          //System.err.println("\033[30;1mLearning "+k+", >= "+mid+"\033[m");
          proc.learn(true);
          if (ge == null) {
            ge = new Node(mid, max, null, null); 
          }
          ge.learn(k);
        }
      } else {
        if (k == max) {
          proc.learn(true);
          //System.err.println("\033[30;1mUpped " + k + ", right of " + mid + "\033[m");
        } else if (k == min) {
          proc.learn(false);
          //System.err.println("\033[30;1mUpped " + k + ", left of " + mid + "\033[m");
        } else {
          throw new IllegalArgumentException("Invalid element.");
        }
      }
    }

    private int depth(int k) {
      int a=min;
      int m=mid;
      int z=max;
      int depth=1;
      while (z-a > 1) {
        if (k < m) { z=m-1; } else { a=m; }
        m=a+(z-a+1)/2;
        depth++;
      }
      return depth;
    }

    public double mass(int k) {
      if (max-min > 1) {
        if (k < mid) {
          if (le != null) {
            return proc.mass(false) * le.mass(k);
          } else {
            // TODO: verify this
            return proc.mass(false) * Math.pow(0.5,depth(k)-1);
          }
        } else {
          if (ge != null) {
            return proc.mass(true) * ge.mass(k);
          } else {
            // TODO: verify this
            return proc.mass(true) * Math.pow(0.5,depth(k)-1);
          }
        }
      } else {
        return proc.mass(k >= mid);
      }
    }

    public double mass(NavigableSet<Integer> elts) {
      if (cached_elts != null && cached_elts.equals(elts)) {
        return cached_mass;
      } else {
        double mass = 0.0;
        if (max - min > 1) {
          NavigableSet<Integer> leftElts = elts.headSet(mid, false);
          NavigableSet<Integer> rightElts = elts.tailSet(mid, true);
          if (!leftElts.isEmpty()) {
            mass += proc.mass(false) * le.mass(leftElts);
          }
          if (!rightElts.isEmpty()) {
            mass += proc.mass(true) * ge.mass(rightElts);
          }
        } else {
          if (elts.contains(max)) {
            mass += proc.mass(true);
          }
          if (min != max && elts.contains(min)) {
            mass += proc.mass(false);
          }
        }
        //System.err.println("mass of " + elts + " between [" + min + "," + max + "] = " + mass);
        cached_elts = elts;
        cached_mass = mass;
        return mass;
      }
    }
    
    public double logMass(int k) {
      if (max-min > 1) {
        if (k < mid) {
          if (le != null) {
            return proc.logMass(false) + le.logMass(k);
          } else {
            // TODO: verify this
            return proc.logMass(false) - Math.log(max-min+1);
          }
        } else {
          if (ge != null) {
            return proc.logMass(true) + ge.logMass(k);
          } else {
            // TODO: verify this
            return proc.logMass(true) - Math.log(max-min+1);
          }
        }
      } else {
        return proc.logMass(k >= mid);
      }
    }

    public void encode(int k, Encoder ec) {
      //System.err.println("Node: encoding " + k + " / proc: " + proc);
      //System.err.println("\033[33mEncoding ("+min+":"+mid+":"+max+")\033[m");
      if (max-min > 1) {
        if (k < mid && k >= min) {
          proc.encode(false,ec);
          //System.err.println("\033[36mTook le branch\033[m");
          if (le == null) {
            le = new Node(min, mid-1, null, null); 
          }
          //System.err.println("\033[33mEncoding le ("+min+"-"+(mid-1)+")\033[m");
          le.encode(k,ec);
        } else
        if (k >= mid && k <= max) {
          proc.encode(true,ec);
          //System.err.println("\033[31mTook ge branch\033[m");
          if (ge == null) {
            ge = new Node(mid, max, null, null); 
          }
          //System.err.println("\033[33mEncoding ge ("+mid+"-"+max+")\033[m");
          ge.encode(k,ec);
        } else {
          throw new RuntimeException("attempt to encode invalid element");
        }
      } else {
        //proc.encode(k >= mid, ec);
        //System.err.println("\033[33mEncoding "+k+".\033[m");
        if (k == max) {
          //System.err.println("Coding true, " + proc.mass(true));
          proc.encode(true, ec);
        } else
        if (k == min) {
          //System.err.println("Coding false, " + proc.mass(false));
          proc.encode(false, ec);
        } else {
          throw new RuntimeException("attempt to encode invalid element");
        }
      }
    }
    
    public void encode(int k, NavigableSet<Integer> omit, Encoder ec) {
      //System.err.println("Node: exclusion coding " + k + " / proc: " + proc);
      //System.err.println("Encoding: "+k+" in ("+min+","+max+") with omit set "+omit);
      if (max-min > 1) {
        NavigableSet<Integer> leftOmit = omit.headSet(mid, false);
        NavigableSet<Integer> rightOmit = omit.tailSet(mid, true);
        double leftMass = proc.mass(false);
        if (!leftOmit.isEmpty()) {
          leftMass *= (1 - le.mass(leftOmit));
        }
        double rightMass = proc.mass(true);
        if (!rightOmit.isEmpty()) {
          rightMass *= (1 - ge.mass(rightOmit));
        }
        double p = rightMass / (rightMass + leftMass);
        //System.err.println("[" + min + "," + max + "]: p = " + p);
        Bernoulli<Boolean> b = new Bernoulli<>(true, false, p);
        if (k < mid && k >= min) {
          b.encode(false, ec);
          //System.err.println("\033[36mTook le branch\033[m");
          if (le == null) {
            le = new Node(min, mid-1, null, null);
          }
          //System.err.println("\033[33mEncoding le ("+min+"-"+(mid-1)+")\033[m");
          le.encode(k, leftOmit, ec);
        } else
        if (k >= mid && k <= max) {
          b.encode(true, ec);
          //System.err.println("\033[31mTook ge branch\033[m");
          if (ge == null) {
            ge = new Node(mid, max, null, null);
          }
          //System.err.println("\033[33mEncoding ge ("+mid+"-"+max+")\033[m");
          ge.encode(k, rightOmit, ec);
        } else {
          throw new RuntimeException("attempt to encode invalid element");
        }
      } else {
        // simpler case
        //System.err.println("\033[33mExclusion coding "+k+".\033[m.");
        boolean emin = omit.contains(min);
        boolean emax = omit.contains(max);
        if (emin && !emax || emax && !emin) {
          //System.err.println("No coding needed.");
          // no action needed.
        } else if (!emin && !emax) {
          if (k == max) {
            //System.err.println("Coding true, " + proc.mass(true));
            proc.encode(true, ec);
          } else
          if (k == min) {
            //System.err.println("Coding false, " + proc.mass(true));
            proc.encode(false, ec);
          } else {
            throw new RuntimeException("attempt to encode invalid element");
          }
        } else {
          throw new AssertionError("omit contains all possible elements");
        }
      }
    }
    
    public int decode(Decoder dc) {
      //System.err.println("\033[33mDecoding ("+min+":"+mid+":"+max+")\033[m");
      boolean b = proc.decode(dc);
      if (max-min > 1) {
        if (b) {
          //System.err.println("\033[31mTook ge branch\033[m");
          if (ge == null) {
            ge = new Node(mid, max, null, null); 
          }
          //System.err.println("\033[33mDecoding ge ("+mid+"-"+max+")\033[m");
          return ge.decode(dc);
        } else {
          //System.err.println("\033[36mTook le branch\033[m");
          if (le == null) {
            //System.err.println("\033[33mCreating le node ("+min+"-"+(mid-1)+")...");
            le = new Node(min, mid-1, null, null); 
          }
          //System.err.println("\033[33mDecoding le ("+min+"-"+(mid-1)+")\033[m");
          return le.decode(dc);
        }
      } else {
        //System.err.println("\033[33mDecoded "+(b ? max : min)+"...");
        return b ? max : min;
      }
    }

    public int decode(NavigableSet<Integer> omit, Decoder dc) {
      //System.err.println("Encoding: "+k+" in ("+min+","+max+") with omit set "+omit);
      if (max-min > 1) {
        NavigableSet<Integer> leftOmit = omit.headSet(mid, false);
        NavigableSet<Integer> rightOmit = omit.tailSet(mid, true);
        double leftMass = proc.mass(false);
        if (!leftOmit.isEmpty()) {
          leftMass *= (1 - le.mass(leftOmit));
        }
        double rightMass = proc.mass(true);
        if (!rightOmit.isEmpty()) {
          rightMass *= (1 - ge.mass(rightOmit));
        }
        double p = rightMass / (rightMass + leftMass);
        //System.err.println("[" + min + "," + max + "]: p = " + p);
        Bernoulli<Boolean> bernoulli = new Bernoulli<>(true, false, p);
        boolean b = bernoulli.decode(dc);
        if (b) {
          if (ge == null) {
            ge = new Node(mid, max, null, null);
          }
          return ge.decode(omit.tailSet(mid,true), dc);
        } else {
          if (le == null) {
            le = new Node(min, mid-1, null, null);
          }
          return le.decode(omit.headSet(mid,false), dc);
        }
      } else {
        // simpler case
        boolean emin = omit.contains(min);
        boolean emax = omit.contains(max);
        if (emin && !emax) {
          return max;  // certain event
        } else
        if (emax && !emin) {
          return min;  // certain event
        } else
        if (!emin && !emax) {
          boolean b = proc.decode(dc);
          return b ? max : min;
        } else {
          throw new AssertionError("omit contains all possible elements");
        }
      }
    }
    
    private void toDotString(StringBuilder sb) {
      String node = "n"+hashCode();
      if (max-min > 1) {
        sb.append(node+" [label=\""+mid+"\\n"+min+".."+max+"\"];\n");
      } else {
        sb.append(node+" [label=\""+mid+"\", style=\"filled\"];\n");
      }
      if (le != null) {
        sb.append(node+" -> n"+le.hashCode()+";\n");
        le.toDotString(sb);
      } else {
        //sb.append(min+" [shape=none];\n");
        //sb.append(node+" -> "+min+" [style=\"dotted\"];\n");
        sb.append("l"+hashCode()+" [label=\""+min+".."+(mid-1)+"\",shape=none];\n");
        sb.append(node+" -> l"+hashCode()+" [style=\"dotted\"];\n");
      }
      //sb.append(node+" -> "+min+" [style=\"dotted\"];\n");
      if (ge != null) {
        sb.append(node+" -> n"+ge.hashCode()+";\n");
        ge.toDotString(sb);
      } else {
        sb.append("g"+hashCode()+" [label=\""+mid+".."+max+"\",shape=none];\n");
        sb.append(node+" -> g"+hashCode()+" [style=\"dotted\"];\n");
      }
      //sb.append(node+" -> "+max+" [style=\"dotted\"];\n");
    }

    public String toDotString() {
      StringBuilder sb = new StringBuilder();
      sb.append("digraph toi {\n");
      toDotString(sb);
      sb.append("}\n");
      return sb.toString();
    }

  }
  
  /** Constructs a new SBST process over the
    * range <var>min</var> to <var>max</var> (inclusive)
    * with a given concentration.
    * @param g1 concentration parameter numerator
    * @param g2 concentration parameter denominator */
  public SBST(int min, int max, short g1, short g2) {
    this.g1 = g1;
    this.g2 = g2;
    this.tree = new Node(min, max, null, null);
    this.n = 0;
  }

  /** Constructs a new SBST process over the
    * range <var>min</var> to <var>max</var> (inclusive)
    * with a given concentration.
    * @param alpha concentration parameter */
  public SBST(int min, int max, double alpha) {
    Tuple<Long,Long> t = Tools.fraction(alpha,4096);
    this.g1 = t.get0().shortValue();
    this.g2 = t.get1().shortValue();
    this.tree = new Node(min, max, null, null);
    this.n = 0;
  }

  /** Constructs a new SBST process over the
    * range <var>min</var> to <var>max</var> (inclusive). */
  public SBST(int min, int max) {
    this.tree = new Node(min, max, null, null);
    this.n = 0;
  }

  /** Returns if this distribution is defined over a finite set
    * of elements.  For this process, this is always true.
    * (At least until someone extends it to the infinite case.)
    * @return true */
  public boolean isFinite() {
    return true;
  }

  /** Learns a new integer and updates the internal tree. */
  public void learn(Integer k) {
    if (k > tree.max || k < tree.min) {
      throw new IllegalArgumentException("integer out of range");
    } else {
      tree.learn(k);
    }
    n++;
  }

  public Integer sample(Random rnd) {
    n++;
    return tree.sample(rnd);
  }

  public double mass(Integer k) {
    return tree.mass(k);
  }
  
  public double logMass(Integer k) {
    return tree.logMass(k);
  }

  public String toString() {
    return "SBST("+tree.min+".."+tree.max+", n="+n+")";
  }

  public void encode(Integer k, Encoder ec) {
    //System.err.println("SBST: encoding " + k);
    tree.encode(k,ec);
  }
  
  public Integer decode(Decoder dc) {
    return tree.decode(dc);
  }
  
  public void encode(Integer k, Collection<Integer> omit, Encoder ec) {
    //System.err.println("SBST: exclusion coding " + k);
    if (omit instanceof NavigableSet) {
      tree.encode(k, (NavigableSet<Integer>) omit, ec);
    } else {
      tree.encode(k, new TreeSet<Integer>(omit), ec);
    }
  }

  public Integer decode(Collection<Integer> omit, Decoder dc) {
    if (omit instanceof NavigableSet) {
      return tree.decode((NavigableSet<Integer>) omit, dc);
    } else {
      return tree.decode(new TreeSet<Integer>(omit), dc);
    }
  }
  
  public Distribution<Integer> getPredictiveDistribution() {
    throw new UnsupportedOperationException();
  }

}
