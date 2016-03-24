/* Automated copy from build process */
/* $Id: PPMTrie.java,v 1.6 2013/01/10 14:12:23 chris Exp $ */
package uk.ac.cam.eng.ml.tcs27.compression;

import java.util.Iterator;
import java.util.Hashtable;
import java.util.Map;
import java.util.Random;
import java.util.Collection;


/** Nodes in a prefix tree / trie.
  * Child-nodes are indexed by objects of class <code>X</code>,
  * and map to an object of class <code>Y</code>.
  * This implementation is based on Hashtables. */
public class PPMTrie<X,Y> {

  /** Map to children of this node. */
  Hashtable<X,PPMTrie<X,Y>> children = null;

  /** Pointer to the tree of the next shorter context.
    * If the current node has symbol "D" and is reached from the
    * root via nodes "A, B, C", then the vine pointer points to
    * the node "D" which is reached from the root via "B, C".
    * The root of the PPM trie is marked by a vine pointer
    * of value null.*/
  PPMTrie<X,Y> vine = null;

  /** Additional data. */
  Y data = null;

  public String toString() {
    return getClass().toString()+":"+hashCode()+":"+vine;
  }

  /** Constructs a new PPMTrie node. */
  public PPMTrie() {
    //this.children = new Hashtable<X,PPMTrie<X,Y>>();
  }
  
  /** Constructs a new PPMTrie node.
    * @param vine vine pointer */
  public PPMTrie(PPMTrie<X,Y> vine) {
    this();
    this.vine = vine;
  }
  
  /** Constructs a new PPMTrie node.
    * @param data data stored at this node */
  public PPMTrie(Y data) {
    this();
    this.data = data;
  }

  /** Constructs a node with given symbol and pointer
    * to the next shorter context. The shortest possible
    * context is the empty context (i.e. a root node).
    * @param data data stored at this node
    * @param vine vine pointer (pointer to next shorter context) */
  public PPMTrie(Y data, PPMTrie<X,Y> vine) {
    this.data = data;
    this.vine = vine;
  }

  /** Constructs a clone of this context tree.
    * Deep copying is used: subnodes are cloned recursively.
    * Vine-pointers in the cloned tree are connected up like in
    * the original tree by using a relocation hashmap. */
  public PPMTrie<X,Y> clone() {
    //return clone(new Hashtable<PPMTrie<X,Y>,PPMTrie<X,Y>>());
    PPMTrie<X,Y> root = findRoot();
    Hashtable<PPMTrie<X,Y>,PPMTrie<X,Y>> reloc = new Hashtable<PPMTrie<X,Y>,PPMTrie<X,Y>>();
    // copy from the root down
    root.cloneDown(reloc);
    // now restore vine pointers
    root.restoreVines(reloc);
    return reloc.get(this);
  }
  
  /** Constructs a clone of this context tree.
    * Deep copying is used: subnodes are cloned recursively.
    * Vine-pointers in the cloned tree are connected up like in
    * the original tree.
    * This method is correct, but can overflow the stack because
    * recursions can get very deep.
    * @param reloc relocation map */
  @SuppressWarnings("unchecked")
  private PPMTrie<X,Y> cloneOld(Hashtable<PPMTrie<X,Y>,PPMTrie<X,Y>> reloc) {
    PPMTrie<X,Y> copy = null;
    Y datacopy = null;
    if (data != null) {
      try {
        //datacopy = (Y) data.getClass().getMethod("clone").invoke(data);
        datacopy = data;
      } 
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    if (vine == null) {
      copy = new PPMTrie<X,Y>(datacopy);
    } else {
      PPMTrie<X,Y> rvine = reloc.get(vine);
      if (rvine == null) {
        //System.err.println("reloc lookup failed: "+vine);
        // we've started cloning from a lower part of the tree...
        // in this case, clone from above and try a look-up.
        rvine = vine.cloneOld(reloc);
        copy = reloc.get(this);
        if (copy == null) {
          copy = new PPMTrie<X,Y>(datacopy, rvine);
        } else {
          return copy;
        }
      } else {
        //System.err.println("reloc lookup succeeded: "+vine);
        copy = new PPMTrie<X,Y>(datacopy, rvine);
      }
    }
    // register the new mapping
    reloc.put(this, copy);
    // recursively clone all children
    if (children == null) {
      copy.children = null;
    } else {
      copy.children = new Hashtable<X,PPMTrie<X,Y>>();
      for (Map.Entry<X,PPMTrie<X,Y>> e : children.entrySet()) {
        PPMTrie<X,Y> c = e.getValue();
        PPMTrie<X,Y> ccopy = reloc.get(c);
        if (ccopy == null) {
          ccopy = c.cloneOld(reloc);
        }
        copy.children.put(e.getKey(),ccopy);
      }
    }
    return copy;
  }
  
  
  
  /** Clones this trie from this node downwards, but without vine pointers.
    * Deep copying is used: subnodes are cloned recursively.
    * The resulting copy must still have its vine pointers reconnected,
    * which can be done with the relocation map that is constructed
    * in the downward cloning process.
    * @param reloc relocation map */
  @SuppressWarnings("unchecked")
  private PPMTrie<X,Y> cloneDown(Hashtable<PPMTrie<X,Y>,PPMTrie<X,Y>> reloc) {
    PPMTrie<X,Y> copy = null;
    // copy the cargo data
    Y datacopy = null;
    if (data != null) {
      try {
        datacopy = (Y) data.getClass().getMethod("clone").invoke(data);
        //((Y) data).getClass().getMethod("equals").invoke(data,datacopy);
      } 
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    copy = new PPMTrie<X,Y>(datacopy);
    // register new mapping
    reloc.put(this,copy);
    // recursively clone all children
    if (children == null) {
      copy.children = null;
    } else {
      copy.children = new Hashtable<X,PPMTrie<X,Y>>();
      for (Map.Entry<X,PPMTrie<X,Y>> e : children.entrySet()) {
        PPMTrie<X,Y> c = e.getValue();
        PPMTrie<X,Y> ccopy = c.cloneDown(reloc);
        copy.children.put(e.getKey(),ccopy);
      }
    }
    return copy;
  }
  
  private void restoreVines(Hashtable<PPMTrie<X,Y>,PPMTrie<X,Y>> reloc) {
    for (Map.Entry<PPMTrie<X,Y>,PPMTrie<X,Y>> e : reloc.entrySet()) {
      PPMTrie<X,Y> node = e.getKey();
      PPMTrie<X,Y> copy = e.getValue();
      if (node.vine != null) {
        copy.vine = reloc.get(node.vine);
      }
    }
  }

  
  /** Finds the root node of this trie.
    * This method exploits that the root can always be found by
    * climbing up the vine pointers. */
  public PPMTrie<X,Y> findRoot() {
    PPMTrie<X,Y> root = this;
    while (root.vine != null) {
      root = root.vine;
    }
    return root;
  }

  /** Adds a child node.
    * @param x the link label
    * @return a pointer to the new child node */
  public PPMTrie<X,Y> add(X x) {
    PPMTrie<X,Y> newvine = null;
    if (vine != null) {
      newvine = this.vine.findOrAdd(x);
    } else {
      newvine = this;
    }
    PPMTrie<X,Y> child = new PPMTrie<X,Y>(newvine);
    if (children == null) {
      children = new Hashtable<X,PPMTrie<X,Y>>();
    }
    children.put(x,child);
    return child;
  }

  /** Finds the child node of given label,
    * or returns <code>null</code> if no such node exists. */
  public PPMTrie<X,Y> find(X x) {
    return children.get(x);
  }
  
  /** Returns the child node of given label.
    * If a matching child node does not exist, it is created. */
  public PPMTrie<X,Y> findOrAdd(X x) {
    if (children != null) {
      PPMTrie<X,Y> node = children.get(x);
      if (node != null) {
        return node;
      } else {
        return add(x);
      }
    } else {
      return add(x);
    }
  }

}



