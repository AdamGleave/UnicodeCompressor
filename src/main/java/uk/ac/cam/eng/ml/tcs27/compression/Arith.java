package uk.ac.cam.eng.ml.tcs27.compression;
/* Automated copy from build process */
/* $Id: Arith.java,v 1.28 2015/08/11 11:28:16 chris Exp $ */

import java.io.IOException;

/** Implements arithmetic coding (TCS 2011).
  * <p>
  * This implementation combines ideas and code from the
  * <a href="#witten1987a">Witten-Neal-Cleary coder</a>,
  * and the <a href="#moffat1998a">Moffat-Neal-Witten coder</a>.
  * </p>
  * <dl><dt><b>References</b></dt><dd><ol>
  * <li><a name="witten1987a">Ian H. Witten, Radford Neal, John G. 
  Cleary.&nbsp; <i>Arithmetic Coding for Data Compression,</i> 1987-06. In 
  Communications of the ACM, Vol. 30, No. 6, pp. 520-540.</a></li>
  * <li><a name="moffat1998a">Alistair Moffat; Radford M. Neal; Ian H. 
  * Witten.&nbsp; <i>Arithmetic coding revisited,</i> 1998. In ACM 
  * Transactions on Information Systems, Vol. 16, No. 3, pp. 256-294.
  * </a></li>
  * <li><a name="steinruecken2014a">Christian Steinruecken.&nbsp;
  * <i>Lossless Data Compression,</i> 2014. PhD Thesis. University of
  * Cambridge.</a></li></ol></dd></dl> */
public class Arith implements Coder {

  /** Number of bits used for the discrete representation of
    * the arithmetic coding interval. */
  static final long b = Long.SIZE-2;
  /** Index of lower quarter of the number space. */
  static final long lb = (long) 1 << (b-2);
  /** Index of midpoint of the number space. */
  static final long hb = (long) 1 << (b-1);
  /** Top point in the number space. */
  static final long tb = ((long) 1 << b) - 1;
  /** Mask of <var>b</var> 1-bits. */
  static final long mask = ((long) 1 << b) - 1;
  
  /** Range of interval. */
  long R;
  /** Low-point of interval. */
  long L;
  /** Target location in interval. */
  long D;
  /** Number of opposite-valued bits waiting to be output after the
    * the next output bit. */
  long bits_waiting;

  BitWriter output = null;
  BitReader  input = null;
  
  /** Returns the current coding range. */
  public long getRange() {
    return R;
  }
  
  public long getTarget() {
    return D-L;
  }
  
  protected void narrow_region(long l, long h) {
    L = L + l;
    R = h - l;
  }

  /** Scaled version of narrow_region.
    * Narrows the interval to the segment (l/t, h/t). */
  protected void narrow_region(long l, long h, long t) {
    long r = R / t;
    L = L + r*l;
    R = h < t ? r * (h-l) : R - r*l;
  } // Moffat-Neal-Cleary (1998)
  
  /** Scaled version of getTarget. */
  public long getTarget(long t) {
    long r = R / t;
    long dr = (D-L) / r;
    return (t-1 < dr) ? t-1 : dr;
  } // Moffat-Neal-Cleary (1998)
  

  /** Narrows the codable interval to the region specified by (l,h,t),
    * generates output bits (if possible) and rescales.
    * @see #encode(Codable,Object) */
  public void encode(long l, long h, long t) throws IOException {
    debugout("\t\033[34m"+l+", "+h+"; "+t+"\033[m\n");
    debugout("\t\033[33m/("+L+" - "+(L+R)+"; +"+bits_waiting+")\033[m\n");

    narrow_region(l,h,t);
    debugout("\t\033[33m|("+L+" - "+(L+R)+"; +"+bits_waiting+")\033[m\n");
    output_bits();
    debugout("\t\033[33m\\("+L+" - "+(L+R)+"; +"+bits_waiting+")\033[m\n");
  }
  

  /** Output processed bits. */
  protected void output_bits() throws IOException {
    // renormalization, so R lies between lb and hb:
    while (R <= lb) {
      if (L+R <= hb) {
        // in lower half: output zero
        bit_plus_follow((byte) 0);
      } else
      if (L >= hb) {
        // in upper half: output one
        bit_plus_follow((byte) 1);
        L = L - hb;
      } else {
        bits_waiting++;
        L = L - lb;
      }
      // zoom in
      L = L << 1;
      R = R << 1;
    }
  }

  /** Discard processed bits.
    * This is the decoder's counterpart of <code>output_bits</code>. */
  protected void discard_bits() throws IOException {
    while (R <= lb) {
      if (L >= hb) {
        // in upper half
        L -= hb;
        D -= hb;
        debugout("\033[35m+\033[m");
      } else
      if (L+R <= hb) {
        // in lower half: nothing to do
        debugout("\033[35m-\033[m");
      } else {
        // middle
        L -= lb;
        D -= lb;
        debugout("\033[35m:\033[m");
      }
      // zoom in
      L <<= 1;
      R <<= 1;
      D <<= 1;
      D &= mask;
      D += input.readBit();
    }
  }

  public void storeRegion(long l, long h, long t) {
    if (l==h) {
      throw new ZeroMassException("storeRegion called with "
                                  +"(l=h="+h+", t="+t+")");
    }
    narrow_region(l,h,t);
    try {
      output_bits();
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  public void storeRegion(long l, long h) {
    narrow_region(l,h);
    try {
      output_bits();
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  public void loadRegion(long l, long h, long t) {
    narrow_region(l,h,t);
    try {
      discard_bits();
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void loadRegion(long l, long h) {
    narrow_region(l,h);
    try {
      discard_bits();
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }


  /** Encode a symbol <var>x</var> using codable interface <var>e</var>.
    * Ideally you shouldn't use this method, call <code>encode</code>
    * on the Codable object directly.
    * @see #encode(long,long,long)
    * @see Codable#encode(Object,Encoder) 
    * @deprecated */
  public <X> void encode(Codable<X> e, X x) {
    debugout("\033[34mEncoding symbol: "+x+"\033[m\n");
    e.encode(x,this);
  }

  /** Decodes a symbol using decodable interface <var>d</var>.
    * @deprecated */
  public <X> X decode(Codable<X> d) {
    X symbol = d.decode(this);
    return symbol;
  }


  int position = 0;


  /** Writes a bit and as many bits of opposite value
    * as specified by <code>bits_waiting</code>. */
  private void bit_plus_follow(byte bit) throws IOException {
    output.writeBit(bit);
    while (bits_waiting > 0) {
      output.writeBit((byte) (1-bit));
      bits_waiting--;
    }
  }

  /** Starts an encoding process. */
  public void start_encode(BitWriter output) {
    this.output = output;
    L = 0;   // lowest possible point
    R = tb;  // full range
    bits_waiting = 0;
  }

  /** Finishes an encoding process. */
  public void finish_encode() throws IOException {
    debugout("\033[34mFinishing...\033[m\n");
    while (true) {
      if (L + (R>>1) >= hb) {
        bit_plus_follow((byte) 1);
        if (L < hb) {
          R -= hb - L;
          L  = 0;
        } else {
          L -= hb;
        }
      } else {
        bit_plus_follow((byte) 0);
        if (L+R > hb) {
          R = hb - L;
        }
      }
      if (R == hb) {
        break; // end condition reached
      } else {
        //System.err.println("R="+R+", L="+L+", R-hb="+(R-hb));
        if (R==L) {
          throw new IllegalStateException("R = L = "+R);
        }
      }
      L <<= 1;
      R <<= 1;
    }
  }

  /** Starts a decoding process.
    * @param pad1 prefixes the input bit sequence with a 1 bit */
  public void start_decode(BitReader input, boolean pad1) throws IOException {
    this.input = input;
    debugout("\033[34mDecoding...\033[m\n");
    /*
    if (pad1) {
      // assume first bit (always 1) was suppressed
      // [this concerned an old implementation: ignore]
      D = 1;
    } else {
      // read first bit (must be 1)
      D = input.readBit();
      if (D != 1) {
        throw new RuntimeException("Illegal start sequence");
      }
    }
    */
    // fill data pointer with bits
    for (int k=0; k<b; k++) {
      D <<= 1;
      D += input.readBit();
    }
    L = 0;  // hb
    R = tb; // WNC use "tb", MNW use "hb"
    //assert (D >= L && D < L+R);
  }

  /** Starts a decoding process. */
  public void start_decode(BitReader input) throws IOException {
    start_decode(input, false);
  }

  /** Finishes a decoding process. */
  public void finish_decode() {
  }
  
  /** Returns the name of this arithmetic coding algorithm. */
  public String toString() {
    return "AC 2011 [TCS]";
  }

  public static void debugout(String d) {
    //System.err.print(d);
  }


}
