/* Automated copy from build process */
/* $Id: Histogram.java,v 1.48 2013/09/02 16:17:29 chris Exp $ */

import java.util.Random;
import java.util.Vector;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.Comparator;
import java.io.PrintStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.IOException;
import java.io.Externalizable;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

/** A class for handling histograms.
  * <p>This class provides various methods for creating histograms
  * from collections of numbers or from probability distributions.
  * Several operations on histograms are supported, including
  * export, plotting (via gnuplot) and computing a histogram's entropy.
  * Also note the <code>print(...)</code>
  * method which plots histograms to a text terminal.</p> */
public class Histogram implements Externalizable {

  /** Histogram counts. */
  ArrayList<Integer> hist;
  /** Largest count in the histogram. */
  int maxcount = 0;
  /** Minimum value found in data (left start of histogram). */
  double min = Double.NEGATIVE_INFINITY;
  /** Maximum value found in data (right start of histogram). */
  double max = Double.POSITIVE_INFINITY;
  /** Number of values outside the range of the histgram. */
  long outside = 0;
  /** Optional bin labels. */
  ArrayList<String> labels;


  /** Returns the number of bins of this histogram. */
  public int bins() {
    return hist.size();
  }

  /** Recomputes <code>maxcount</code>.
    * @see #maxcount */
  protected void updateMaxcount() {
    maxcount = 0;
    for (int i : hist) {
      if (maxcount < i) { maxcount=i; }
    }
  }

  /** Inverts the histogram. */
  public void invert() {
    for (int k=0; k<hist.size(); k++) {
      hist.set(k, maxcount - hist.get(k));
    }
  }

  /** Adds the counts from a second histogram.
    * The histograms must have precisely the same number of bins
    * and the same <code>min</code> and <code>max</code> values. */
  public void add(Histogram g) throws IllegalArgumentException {
    if (g.bins() == bins() && max == g.max && min == g.min) {
      for (int k=0; k<bins(); k++) {
        hist.set(k, hist.get(k)+g.hist.get(k));
      }
      outside+=g.outside;
      updateMaxcount();
    } else {
      throw new IllegalArgumentException();
    }
  }
  

  protected Histogram() {
    // to enable saving and loading
  }
  
  /** Creates an empty histogram with <code>k</code> bins.
    * @param k number of bins */
  public Histogram(int k) {
    // construct a new ArrayList with capacity k
    hist = new ArrayList<Integer>(k);
    // add k bins with count zero
    for (int j=0; j<k; j++) {
      hist.add(0);
    }
    this.maxcount = 0;
    this.outside = 0;
  }
  
  /** Creates a histogram from an existing histogram.
    * @param h existing histogram. */
  public Histogram(Histogram h) {
    hist = h.hist;
    min = h.min;
    max = h.max;
    maxcount = h.maxcount;
    outside = h.outside;
  }
  
  /** Creates an empty histogram with <code>k</code> bins
    * and given range.
    * @param k number of bins
    * @param min lower boundary
    * @param max upper boundary */
  public Histogram(int k, double min, double max) {
    this(k);
    this.max = max;
    this.min = min;
    this.maxcount = 0;
    this.outside = 0;
  }
  
  /** Sets the range parameters to the minimum and maximum found
    * in the supplied array. */
  private void setRange(double[] data) {
    max = Double.NEGATIVE_INFINITY;
    min = Double.POSITIVE_INFINITY;
    for (double d : data) {
      if (d > max) { max = d; }
      if (d < min) { min = d; }
    }
  }

  /** Sets the range parameters to the minimum and maximum found
    * in the supplied array. */
  private void setRange(Iterable<Double> data) {
    max = Double.NEGATIVE_INFINITY;
    min = Double.POSITIVE_INFINITY;
    for (double d : data) {
      if (d > max) { max = d; }
      if (d < min) { min = d; }
    }
  }


  
  /** Creates a histogram from an array of doubles.
    * @param data values to be histogrammed
    * @param k number of bins
    * @param min lower boundary
    * @param max upper boundary */
  public static Histogram fromDoubles(double[] data, int k,
                                      double min, double max) {
    Histogram h = new Histogram(k);
    h.min = min;
    h.max = max;
    double range = max - min; // 0 .. +anything
    for (double d : data) {
      if (d >= min  && d < max) {
        int i = (int) ((double) ((d-min)*(k))/range);
        h.hist.set(i, h.hist.get(i)+1);
      } else {
        h.outside++;
      }
    }
    h.updateMaxcount();
    return h;
  }
  
  /** Creates a histogram from an iterable collection of Doubles.
    * @param data values to be histogrammed
    * @param k number of bins
    * @param min lower boundary
    * @param max upper boundary */
  public static Histogram fromDoubles(Iterable<Double> data, int k, double min, double max) {
    Histogram h = new Histogram(k);
    h.min = min;
    h.max = max;
    double range = max - min; // 0 .. +anything
    for (double d : data) {
      if (d >= min  && d < max) {
        int i = (int) ((d-min)*k / range);
        h.hist.set(i,h.hist.get(i)+1);
      } else
      if (d == max) {
        h.hist.set(k-1, h.hist.get(k-1)+1);
      } else {
        h.outside++;
      }
    }
    h.updateMaxcount();
    return h;
  }
  
  /** Creates a histogram from an array of doubles.
    * @param k number of bins */
  public static Histogram fromDoubles(double[] data, int k) {
    Histogram h = new Histogram(k);
    h.setRange(data);
    double range = h.max - h.min; // 0 .. +anything
    for (double d : data) {
      if (d != h.max) {
        int i = (int) (((d-h.min) * k) / range);
        h.hist.set(i,h.hist.get(i)+1);
      } else {
        h.hist.set(k-1,h.hist.get(k-1)+1);
      }
    }
    h.updateMaxcount();
    return h;
  }
    
  
  /** Creates a histogram from an iterable collection of Doubles.
    * @param k number of bins */
  public static Histogram fromDoubles(Iterable<Double> data, int k) {
    Histogram h = new Histogram(k);
    h.setRange(data);
    double range = h.max - h.min; // 0 .. +anything
    for (double d : data) {
      if (d != h.max) {
        int i = (int) (((d-h.min) * k) / range);
        h.hist.set(i,h.hist.get(i)+1);
      } else {
        h.hist.set(k-1,h.hist.get(k-1)+1);
      }
    }
    h.updateMaxcount();
    return h;
  }
  
  
  /** Creates a histogram by drawing <var>n</var> samples from
    * a Sampler <var>s</var>.
    * Samples between between <code>min</code> and <code>max</code>
    * are placed into <code>k</code> bins.
    * @param s any sampler or distribution over double-precision floating
    *          point numbers
    * @param rnd random source
    * @param min minimum
    * @param max maximum
    * @param k number of bins
    * @param n number of draws */
  public static Histogram fromDoubles(Sampler<Double> s, Random rnd,
                                      double min, double max, int k, int n) {
    assert max >= min;
    Histogram h = new Histogram(k);
    h.min = min;
    h.max = max;
    double range = max - min;
    for (int i=0; i<n; i++) {
      double sample = s.sample(rnd);
      if (sample >= min  && sample < max) {
        int j = (int) ((sample-h.min)*k / range);
        h.hist.set(j, h.hist.get(j)+1);
      } else
      if (sample == max) {
        h.hist.set(k-1, h.hist.get(k-1)+1);
      } else {
        h.outside++;
      }
    }
    h.updateMaxcount();
    return h;
  }
  
  /** Creates a histogram by drawing <var>n</var> samples from
    * a Sampler <var>s</var>.
    * Samples between between <code>min</code> and <code>max</code>
    * are placed into <code>k</code> bins.
    * @param s any sampler or distribution over double-precision floating
    *          point numbers
    * @param min minimum
    * @param max maximum
    * @param k number of bins
    * @param n number of draws */
  public static Histogram fromDoubles(Sampler<Double> s,
                                      double min, double max, int k, int n) {
    return fromDoubles(s, new Random(), min, max, k, n);
  }
  

  /** Creates a histogram from an array of integers.
    * Creates one bin per integer. */
  public static Histogram fromIntegers(int[] data) {
    int imax = Integer.MIN_VALUE;
    int imin = Integer.MAX_VALUE;
    for (int i : data) {
      if (i > imax) { imax = i; }
      if (i < imin) { imin = i; }
    }
    Histogram h = new Histogram(imax-imin+1);
    h.max = (double) imax;
    h.min = (double) imin;
    for (int i : data) {
      h.hist.set(i-imin,h.hist.get(i-imin)+1);
    }
    h.updateMaxcount();
    return h;
  }
  
  /** Creates a histogram from an interable collection of integers.
    * Creates one bin per integer. */
  public static Histogram fromIntegers(Iterable<Integer> data) {
    int imax = Integer.MIN_VALUE;
    int imin = Integer.MAX_VALUE;
    for (int i : data) {
      if (i > imax) { imax = i; }
      if (i < imin) { imin = i; }
    }
    Histogram h = new Histogram(imax-imin+1);
    h.max = (double) imax;
    h.min = (double) imin;
    for (int i : data) {
      h.hist.set(i-imin,h.hist.get(i-imin)+1);
    }
    h.updateMaxcount();
    return h;
  }
  
  
  /** Creates a histogram from an array of integers.
    * @param data array of integer values
    * @param min lower boundary (inclusive) 
    * @param max upper boundary (inclusive) */
  public static Histogram fromIntegers(int[] data, int min, int max) {
    int k = max-min+1;
    Histogram h = new Histogram(k);
    h.min = (double) min;
    h.max = (double) max;
    for (int i : data) {
      if (i >= min && i <= max) {
        h.hist.set(i-min,h.hist.get(i-min)+1);
      } else {
        h.outside++;
      }
    }
    h.updateMaxcount();
    return h;
  }
  
  /** Creates a histogram from an iterable collection of integers.
    * The histogram is formed from values between <code>min</code>
    * and <code>max</code> (inclusive), using one bin per integer.
    * @param col any iterable collection of integers
    * @param min inclusive minimum
    * @param max inclusive maximum */
  public static Histogram fromIntegers(Iterable<Integer> col, int min, int max) {
    int k = max-min+1;
    Histogram h = new Histogram(k);
    h.min = (double) min;
    h.max = (double) max;
    for (int i : col) {
      if (i >= min && i <= max) {
        h.hist.set(i-min,h.hist.get(i-min)+1);
      } else {
        h.outside++;
      }
    }
    h.updateMaxcount();
    return h;
  }

  /** Creates a histogram by drawing <var>n</var> samples from
    * a Sampler <var>s</var>.
    * The histogram is formed from samples between <code>min</code>
    * and <code>max</code>, using one bin per integer.
    * @param s   any sampler over integers
    * @param rnd random source
    * @param min inclusive minimum
    * @param max inclusive maximum
    * @param n   number of draws */
  public static Histogram fromIntegers(Sampler<Integer> s, Random rnd,
                                       int min, int max, int n) {
    assert max >= min;
    Histogram h = new Histogram(max-min+1);
    h.max = (double) max;
    h.min = (double) min;
    for (int i=0; i<n; i++) {
      int sample = s.sample(rnd);
      if (sample >= min && sample <= max) {
        int j = sample-min;
        h.hist.set(j, h.hist.get(j)+1);
      } else {
        h.outside++;
      }
    }
    h.updateMaxcount();
    return h;
  }
  
  /** Creates a histogram by drawing <var>n</var> samples from
    * a Sampler <var>s</var>.
    * The histogram is formed from samples between <code>min</code>
    * and <code>max</code>, using one bin per integer.
    * @param s   any sampler over integers
    * @param min inclusive minimum
    * @param max inclusive maximum
    * @param n   number of draws */
  public static Histogram fromIntegers(Sampler<Integer> s,
                                       int min, int max, int n) {
    return fromIntegers(s, new Random(), min, max, n);
  }


  /** Creates a histogram from an iterable collection of long integers.
    * The histogram creates a specified number of bins, grouping
    * together as many longs as needed.
    * @param data iterable collection of longs
    * @param bins number of bins */
  public static Histogram fromLongs(Iterable<Long> data, int bins) {
    long min = Long.MAX_VALUE;
    long max = Long.MIN_VALUE;
    int count = 0;
    for (long i : data) {
      if (i > max) { max = i; }
      if (i < min) { min = i; }
      count++;
    }
    Histogram h = new Histogram(bins);
    if (count == 0) {
      return h;
    }
    long range = max - min;
    long width;
    if (bins >= range) {
      bins = (int) range;
      width = 1;
    } else {
      width = range / (long) bins + 1;
    }
    h.min = (double) min;
    h.max = (double) max;
    for (long i : data) {
      int bin = (int) ((i-min) / width);
      h.hist.set(bin, h.hist.get(bin) + 1);
    }
    h.updateMaxcount();
    return h;
  }
  
  /** Creates a histogram from an iterable collection of long integers.
    * The histogram is created over the specified range only,
    * and into the specified number of bins.
    * @param data iterable collection of longs
    * @param min inclusive minimum
    * @param max inclusive maximum
    * @param bins number of bins */
  public static Histogram fromLongs(Iterable<Long> data, long min, long max, int bins) {
    Histogram h = new Histogram(bins);
    long range = max - min;
    long width;
    if (bins >= range) {
      bins = (int) range;
      width = 1;
    } else {
      width = range / (long) bins + 1;
    }
    h.min = (double) min;
    h.max = (double) max;
    for (long i : data) {
      if (i >= min && i <= max) {
        int bin = (int) ((i-min) / width);
        h.hist.set(bin, h.hist.get(bin) + 1);
      } else {
        h.outside++;
      }
    }
    h.updateMaxcount();
    return h;
  }


  /** Creates a histogram from an iterable collection of bytes.
    * The boundaries of the histogram are auto-detected.
    * @param col any iterable collection of bytes */
  public static Histogram fromBytes(Iterable<Byte> col) {
    byte bmin = Byte.MAX_VALUE;
    byte bmax = Byte.MIN_VALUE;
    for (byte b : col) {
      if (b > bmax) { bmax = b; }
      if (b < bmin) { bmin = b; }
    }
    int k = (int) bmax - (int) bmin + 1; // number of bins
    Histogram h = new Histogram(k);
    h.max = (double) bmin;
    h.min = (double) bmax;
    for (byte b : col) {
      h.hist.set(b-bmin, h.hist.get(b-bmin)+1);
    }
    h.updateMaxcount();
    return h;
  }
  
  /** Creates a histogram from an iterable collection of bytes.
    * @param col any iterable collection of bytes
    * @param min inclusive lower limit
    * @param max inclusive upper limit */
  public static Histogram fromBytes(Iterable<Byte> col, int min, int max) {
    int k = (int) max - (int) min + 1; // number of bins
    Histogram h = new Histogram(k);
    h.max = (double) max;
    h.min = (double) min;
    for (byte b : col) {
      if (b >= min && b <= max) {
        h.hist.set(b-min, h.hist.get(b-min)+1);
      } else {
        h.outside++;
      }
    }
    h.updateMaxcount();
    return h;
  }
  
  /** Creates a histogram from an iterable collection of characters.
    * This adds label annotations to the histogram bins.
    * @param col any iterable collection of chars
    * @param acc list of acceptable characters */
  public static Histogram fromChars(Iterable<Character> col,
                                    Vector<Character> acc) {
    int k = acc.size();
    Histogram h = new Histogram(k);
    h.min = (double) 0;
    h.max = (double) (k-1);
    h.labels = new ArrayList<String>(k);
    // create labels
    int j=0;
    for (char c : acc) {
      h.labels.add(Character.toString(c));
    }
    // create histogram
    for (char c : col) {
      int i = acc.indexOf(c);
      if (i > 0) {
        h.hist.set(i, h.hist.get(i)+1);
      } else {
        h.outside++;
      }
    }
    h.updateMaxcount();
    return h;
  }
  
  /** Creates a histogram from an iterable collection of characters.
    * This adds label annotations to the histogram bins.
    * @param col any iterable collection of chars */
  public static Histogram fromChars(Iterable<Character> col) {
    Histogram h = new Histogram();
    h.hist = new ArrayList<Integer>();
    h.labels = new ArrayList<String>();
    // create histogram and labels
    for (char c : col) {
      int i = h.labels.indexOf(Character.toString(c));
      if (i >= 0) {
        h.hist.set(i, h.hist.get(i)+1);
      } else {
        h.hist.add(1);
        h.labels.add(Character.toString(c));
      }
    }
    h.outside = 0;
    h.min = (double) 0;
    h.max = (double) (h.hist.size()-1);  // check semantics of max
    h.updateMaxcount();
    return h;
  }
  
  /** Creates a histogram by sampling from a distribution over
    * characters.
    * Label annotations are added to the histogram bins automatically.
    * @param s any sampler which returns characters
    * @param n number of samples to draw */
  public static Histogram fromChars(Sampler<Character> s, int n, Random rnd) {
    TreeMap<Character,Integer> tm = new TreeMap<Character,Integer>();
    for (int i=0; i<n; i++) {
      Character x = s.sample(rnd);
      // TODO: this could be more efficient...
      Integer c = tm.get(x);
      if (c != null) {
        tm.put(x,c+1);
      } else {
        tm.put(x,1);
      }
    }
    return fromChars(tm);
  }
  
  
  /** Creates a histogram from a map of characters to integer counts.
    * The counts from the map are imported directly, no sanity checks
    * are done.
    * @param map a map from chars to counts */
  public static Histogram fromChars(Map<Character,Integer> map) {
    Histogram h = new Histogram();
    h.hist = new ArrayList<Integer>();
    h.labels = new ArrayList<String>();
    // create histogram and labels
    for (Map.Entry<Character,Integer> e : map.entrySet()) {
      h.hist.add(e.getValue());
      h.labels.add(e.getKey().toString());
    }
    h.outside = 0;
    h.min = (double) 0;
    h.max = (double) (h.hist.size()-1);  // check semantics of max
    h.updateMaxcount();
    return h;
  }


  
  /** Creates a histogram from an iterable collection and a labelling
    * function.  The objects in the collection are counted and placed
    * into bins assigned by the labelling function.
    * If the labelling function assigns the same label to different
    * objects, bins will be merged so that there is exactly one bin
    * per label.
    * @param col an iterable collection of objects (with repetitions)
    * @param label a function from objects to String labels */
  public static <X> Histogram fromIterable(Iterable<X> col,
                                           Function<X,String> label) {
    Histogram h = new Histogram();
    h.hist = new ArrayList<Integer>();
    h.labels = new ArrayList<String>();
    // create histogram and labels
    for (X x : col) {
      String s = label.eval(x);
      int i = h.labels.indexOf(s);
      if (i >= 0) {
        h.hist.set(i, h.hist.get(i)+1);
      } else {
        h.hist.add(1);
        h.labels.add(s);
      }
    }
    h.outside = 0;
    h.min = (double) 0;
    h.max = (double) (h.hist.size()-1);  // TODO: check semantics of max
    h.updateMaxcount();
    return h;
  }
  
  /** Creates a histogram from a Map of counts and a labelling function.
    * The counts from the map are imported directly into a histogram,
    * and the labelling function is only used to label the bins.
    * If the labelling function assigns the same label to different
    * objects, there will be multiple bins with the same label.
    * @param map any map from objects to counts
    * @param label a function from objects to String labels */
  public static <X> Histogram fromMap(Map<X,Integer> map,
                                      Function<X,String> label) {
    Histogram h = new Histogram();
    h.hist = new ArrayList<Integer>();
    h.labels = new ArrayList<String>();
    // create histogram and labels
    for (Map.Entry<X,Integer> e : map.entrySet()) {
      h.hist.add(e.getValue());
      h.labels.add(label.eval(e.getKey()));
    }
    h.outside = 0;
    h.min = (double) 0;
    h.max = (double) (h.hist.size()-1);  // TODO: check semantics of max
    h.updateMaxcount(); // TODO: inefficient -- do this while unmapping
    return h;
  }


  /** Deletes a bin from the histogram,
    * adding its count to the "outside" counter.
    * @param k index of bin to be removed
    * @see #deleteOutside() */
  private synchronized void deleteBin(int k) {
    int mm = hist.get(k);
    outside += mm;
    hist.remove(k);
    if (labels != null) {
      labels.remove(k);
    }
    if (maxcount == mm) {
      updateMaxcount();
    }
    int w = hist.size();
    if (min == 0.0 && max == (double) w) {
      max = (w-1);
    }
  }

  /** Merges labelled bins with given labels.
    * @param src source bin (to be deleted)
    * @param trg target bin (to be kept) */
  public void mergeBins(String src, String trg) {
    if (labels!=null) {
      int ksrc = labels.indexOf(src);
      int ktrg = labels.indexOf(trg);
      if (ksrc != -1 && ktrg != -1 && ksrc != ktrg) {
        hist.set(ktrg, hist.get(ktrg)+hist.get(ksrc));
        hist.set(ksrc, 0);
        if (labels != null) {
          labels.set(ktrg, labels.get(ksrc));
        }
        deleteBin(ksrc);
        updateMaxcount();
      }
    } else {
      throw new IllegalArgumentException();
    }
  }

  /** Removes all empty bins from a labelled histogram. */
  public void deleteEmptyBins() {
    if (labels != null) {
      for (int j=hist.size()-1; j>0; j--) {
        if (hist.get(j) == 0) {
          // FIXME: this is super ugly and O(n^2)
          deleteBin(j);
        }
      }
    } else {
      throw new IllegalStateException("deletion only permitted for labelled data");
    }
  }

  /** Delete the bin matching a given label, adding
    * its count value to the outside counter.
    * @param label label of the bin
    * @see #deleteBin(int)
    * @see #deleteOutside() */
  public void deleteBin(String label) {
    if (labels != null) {
      // find and delete matching bin (if found)
      int k = labels.indexOf(label);
      if (k != -1) {
        deleteBin(k);
      }
    } else {
      throw new IllegalStateException("not supported for unlabelled histograms");
    }
  }

  /** Zero the count of elements which lie outside
    * the histogram.
    * @see #deleteBin(int) */
  public void deleteOutside() {
    outside = 0;
  }
  
  /** Sort the histogram by puttings labels in alphabetical order. */
  public void sortByLabel() {
    if (labels != null) {
      // sort labels  and bins (by labels, alphabetically)
      Tools.sort2(labels,hist);
    } else {
      throw new IllegalStateException("not supported for unlabelled histograms");
    }
  }

  /** Sort the histogram by putting labels in a custom order. */
  public void sortByLabel(Comparator<String> cmp) {
    if (labels != null) {
      // sort labels  and bins (by labels, using given comparator)
      Tools.sort2(labels,hist,cmp);
    } else {
      throw new IllegalStateException("not supported for unlabelled histograms");
    }
  }

  
  /** Sort the histogram by bin size. */
  public void sortByBinSize() {
    if (labels != null) {
      Tools.revsort2(hist,labels);
    } else {
      throw new IllegalStateException("not supported for unlabelled histograms");
    }
  }


  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeDouble(min);
    out.writeDouble(max);
    out.writeObject(hist);
    out.writeInt(maxcount);
    out.writeLong(outside);
    out.writeObject(labels);
  }

  @SuppressWarnings("unchecked")
  public void readExternal(ObjectInput in) throws IOException,
                                                  ClassNotFoundException {
    min      = in.readDouble();
    max      = in.readDouble();
    hist     = (ArrayList) in.readObject();
    maxcount = in.readInt();
    outside  = in.readLong();
    labels   = (ArrayList) in.readObject();
  }

  
  /** Prints the histogram counts to the specified PrintStream,
    * in plain text.
    * This format can be read by gnuplot. */
  public void printNumbers(PrintStream out) {
    double range = max - min;
    int len = bins();
    double center = range / (double) (2*len);
    for (int k=0; k<len; k++) {
      double v = ((range*k) / (double) len)+min+center;
      out.println(v+"\t"+hist.get(k));
    }
  }
  
  /** Prints the histogram counts to the specified PrintStream,
    * including labels.
    * This format can be read by gnuplot. */
  public void printNumbersAndLabels(PrintStream out) {
    int len = bins();
    double range = max - min;
    double center = range / (double) (2*len);
    long total = getTotal();
    for (int k=0; k<len; k++) {
      double v = ((range*k) / (double) len)+min+center;
      String label = labels.get(k);
      label = label.replace("\\","\\\\");
      label = label.replace("\"","\\\"");
      double prob = (double) hist.get(k) / (double) total;
      out.println(v+"\t"+hist.get(k)+"\t"+prob+"\t"+label);
    }
  }
  
  
  public static char[] cr_grade
          = new char[] {'·','▁','▂','▃','▄','▅','▆','▇','█'};
  public static char[] cr_oldgrade
          = new char[] {' ','▄','█'};
  public static char[] cr_dense
          = new char[] {' ','░','▒','▓','█'};
  public static char[] cr_ascii
          = new char[] {' ','#'};
  public static char[] cr_hashdot
          = new char[] {'.','#'};
  public static char[] cr_hashcdot
          = new char[] {'·','#'};
  public static char[] cr_aa
          = new char[] {'.','a','A'};
  public static char[] cr_mm
          = new char[] {'.','m','M'};
  public static char[] cr_hwkatakana
          = new char[] {'.','ｫ','ｵ'};
  public static char[] cr_trigram
          = new char[] {'☷','☳','☱','☰'};
  public static char[] cr_braille
          = new char[] {'⠀','⣀','⣤','⣶','⣿'};
  public static char[] cr_spike
          = new char[] {' ','╻','┃'};
  public static char[] cr_linecut
          = new char[] {'─','┼','╫','█'};
  public static char[] cr_ogham
          = new char[] {' ','ᚋ','ᚌ','ᚍ','ᚎ','ᚏ'};
  
  /** Prints a 1-line support indicator.
    * All bins with exactly 0 elements will be represented by
    * <code>cr[0]</code>. All others by <code>cr[n]</code>
    * if available, and <code>cr[max]</code> otherwise.
    * @param out PrintStream, e.g. <code>System.out</code>
    * @param cr characters used for printing the indicator. */
  public void printbot(PrintStream out, char[] cr) {
    String s="";
    for (int i : hist) {
      if (i >= cr.length) {
        s+=cr[cr.length-1];
      } else {
        s+=cr[i];
      }
    }
    out.println(s);
  }
  
  /** Prints a 1-line support indicator.
    * @param out PrintStream, e.g. <code>System.out</code> */
  public void printbot(PrintStream out) {
    printbot(out,cr_linecut);
  }
 
  /** Prints the bin labels in a single String of text.
    * If a label has more than one symbol, only the first
    * symbol is used.  If a label is undefined, a space
    * character is printed instead.
    * If no labels are assigned, nothing is printed. */
  public void printLabels(PrintStream out) {
    if (labels != null) {
      StringBuilder sb = new StringBuilder();
      for (String s : labels) {
        if (s != null) {
          char c = s.charAt(0);
          // adjust unprintable characters
          if (c >= 0x00 && c < 0x20) {
            c+=0x2400;
          } else
          if (c == 0x7F) {
            c=0x2421;
          }
          sb.append(c);
        } else {
          sb.append(' ');
        }
      }
      out.println(sb);
    }
  }
  
  /** Prints this histogram to the specified PrintStream,
    * using <code>height</code> lines of unicode characters
    * specified by array <code>cr</code>.
    * @param out PrintStream, e.g. <code>System.out</code>
    * @param height height of the histogram, in lines
    * @param cr characters used for printing the histogram */
  public void print(PrintStream out, int height, char[] cr) {
    int h = height;
    while (h > 0) {
      String s="";
      for (int i : hist) {
        double m = ((double) i / maxcount);
        if (m * height < h-1) {
          s+=cr[0];            // empty
        } else
        if (m * height > h) {
          s+=cr[cr.length-1];  // solid
        } else {
          char g = cr[(int) (Math.floor((m*height - h+1)*(cr.length-1)))];
          s+=g;
        }
      }
      out.println(s);
      h--;
    }
  }

  /** Prints this histogram to the specified PrintStream,
    * using <code>height</code> lines of unicode characters.
    * Note: uses <code>IOTools.getEncoding()</code> to determine
    * which charset to use, and encodes the output accordingly.
    * @see IOTools#getEncoding() */
  public void print(PrintStream out, int height) {
    String enc = IOTools.getEncoding();
    char[] cr = cr_grade;  // default (needs Unicode)
    if (enc.equals("US-ASCII") || enc.equals("ASCII")) {
      cr = cr_mm;
    } else
    if (enc.equals("ISO-8859-1")) {
      cr = cr_hashcdot;
    } else
    if (enc.startsWith("windows-31j")) {
      cr = cr_hwkatakana;
    } else
    if (enc.startsWith("windows-")) {
      cr = cr_mm;
    } else
    if (enc.equals("MacRoman")) {
      cr = cr_aa; // MacRoman suggests trouble
    } else
    if (enc.equals("KOI8-R")) {
      cr = cr_oldgrade;
    } else
    if (enc.equals("UTF-8")) {
      cr = cr_grade;
    } else
    if (enc.equals("")) {
      cr = cr_mm;
      enc = null; // switch off recoding
    } else {
      cr = cr_mm; // ASCII - safe default
    }
    if (enc != null) {
      try {
        PrintStream encout = new PrintStream(out, true, enc);
        this.print(encout,height,cr);
      }
      catch (UnsupportedEncodingException e) {
        System.err.println("Histogram: unsupported encoding: \""+enc+"\"");
        // just send ascii sequences and hope for the best...
        this.print(out,height,cr_mm);
      }
    } else {
      // just send raw sequences
      this.print(out,height,cr);
    }
  }
  
  /** Prints this histogram to the specified PrintStream,
    * using 10 lines of unicode characters.
    * @see #print(PrintStream,int) */
  public void print(PrintStream out) {
    this.print(out,10);
  }
  
  /** Writes HTML code for this histogram to a designated OutputStream.
    * @param height height of histgram (in units)
    * @param unit base unit of measure (e.g. ex or em)
    * @param out output stream */
  public void writeHTML(int height, String unit, OutputStream out) {
    updateMaxcount();
    PrintStream ps = new PrintStream(out);
    ps.println("<style type=\"text/css\">");
    ps.println("  .histg {");
    ps.println("     width: 1"+unit+";");
    ps.println("     padding: 0pt; margin: 0pt;");
    ps.println("     text-align: center;");
    ps.println("  }");
    if (labels != null) {
      ps.println("  .histl {");
      ps.println("     width: 1"+unit+";");
      ps.println("     padding: 0pt; margin: 0pt;");
      ps.println("     text-align: center;");
      ps.println("  }");
    }
    ps.println("  .histb {");
    ps.println("     width: 0.9"+unit+";");
    ps.println("     padding: 0pt; margin: 0pt;");
    ps.println("     background-color: buttonface;");
    ps.println("     border: 1px solid buttonshadow;");
    ps.println("  }");
    ps.println("</style>");
    ps.println("<table cols="+bins()+" border=0 cellspacing=0 cellpadding=0"
              +" style=\"border: 0px; border-collapse: collape;\">");
    ps.println("<tr>");
    int len = bins();
    for (int k=0; k<len; k++) {
      ps.println("<td valign=bottom class=\"histg\" title=\""+hist.get(k)+"\">"
                +"<div class=\"histb\" style=\"height: "
                +((int) ((double) (1000.0*hist.get(k)*height) / maxcount)/1000.0)
                +unit+";\"><img height=1 width=1></div></td>");
    }
    if (labels != null) {
      ps.println("</tr><tr>");
      for (int k=0; k<len; k++) {
        if (labels.get(k) != null) {
          String s = labels.get(k);
          s = s.replace("&","&amp;");
          s = s.replace("<","&lt;");
          s = s.replace(">","&gt;");
          ps.println("<td class=\"histl\">"+s+"</td>");
        } else {
          ps.println("<td></td>");
        }
      }
    }
    ps.println("</tr></table>");
    ps.flush();
  }

  /** Writes <tt>gnuplot</tt> code for this histogram
    * to a designated OutputStream. */
  public void writeGnuplotCode(OutputStream out) {
    boolean percent = true;
    updateMaxcount();
    PrintStream ps = new PrintStream(out);
    ps.println("set xrange ["+min+":"+max+"];");
    if (!percent) {
      ps.println("set yrange [0:"+(maxcount*1.05)+"];");
    }
    if (labels == null) {
      if (percent) {
        ps.println("plot '-' using 1:($2/"+getTotal()+") with boxes");
      } else {
        ps.println("plot '-' with boxes");
      }
      this.printNumbers(ps);
    } else {
      if (percent) {
        ps.println("plot '-' using 1:($2/"+getTotal()
                  +"):xticlabels(4) with boxes");
      } else {
        ps.println("plot '-' using 1:2:xticlabels(4) with boxes");
      }
      this.printNumbersAndLabels(ps);
    }
    ps.flush();
  }

  /** Makes a OS call to <tt>gnuplot</tt> to plot this histogram.
    * Note: gnuplot must be installed for this to work.
    * Terminal setting is "x11 persist". */
  public void gnuplot() throws RuntimeException {
    try {
      updateMaxcount();
      Runtime rt = Runtime.getRuntime();
      Process gnuplot = rt.exec("gnuplot");
      PrintStream pipe = new PrintStream(gnuplot.getOutputStream());
      pipe.println("set terminal x11 persist;");
      this.writeGnuplotCode(pipe);
      pipe.println("e");
      pipe.println("q");
      pipe.flush();
      gnuplot.waitFor();
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  /** Returns the total count of the histogram. */
  public long getTotal() {
    long total = 0;
    for (int k=0; k<bins(); k++) {
      total += hist.get(k);
    }
    return total;
  }

  /** Returns the information entropy of this histogram.
    * Here, the histogram is interpreted to be a probability
    * distribution. */
  public double entropy() {
    long total = getTotal();
    double entr = 0;
    for (int k=0; k<bins(); k++) {
      if (hist.get(k) > 0) {
        entr -= ((double) hist.get(k)/total)*Math.log((double) hist.get(k)/total);
      }
    }
    return entr;
  }

  public static void main(String[] args) throws Exception {
    /** Filename. */
    String fnm = args[0];
    /** Column. */
    String sep = "\\s*";  // default: white space
    /** Column of interest. */
    int col = 1;
    ArrayList<Double> data = new ArrayList<Double>();
    if (fnm.equals("-")) { fnm = ""; }
    Iterable<String> ss = IOTools.stringSequenceFromFile(fnm);
    for (String s : ss) {
      String[] cols = s.split(sep);
      data.add(Double.parseDouble(cols[col]));
    }
    Histogram h = Histogram.fromDoubles(data,2000,-15,+15);
    h.gnuplot();
  }

}
