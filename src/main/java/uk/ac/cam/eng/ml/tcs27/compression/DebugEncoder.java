/* Automated copy from build process */
/* $Id: DebugEncoder.java,v 1.5 2014/08/05 17:29:00 chris Exp $ */
package uk.ac.cam.eng.ml.tcs27.compression;

/** A debug wrapper around an existing encoder. */
public class DebugEncoder implements Encoder {

  Encoder ec = null;
  long lastrange = -1; // last known range of the encoder

  public DebugEncoder(Encoder ec) {
    this.ec = ec;
  }

  public void storeRegion(long l, long h, long t) {
    double logp = Math.log((double) (h-l) / t) / Tools.LN2;
    System.err.println("\033[35mEC.stor "+l+", "+h+", "+t
                      +" \t\t(+"+(-logp)+" bits)\033[m");
    ec.storeRegion(l,h,t);
    lastrange = -1; // discard last known range
  }
  
  public void storeRegion(long l, long h) {
    if (lastrange != -1) {
      double logp = Math.log((double) (h-l) / lastrange) / Tools.LN2;
      System.err.println("\033[35mEC.stor "+l+", "+h
                        +" \t(+"+(-logp)+" bits)\033[m");
    } else {
      System.err.println("\033[35mEC.stor "+l+", "+h+"\033[m");
    }
    ec.storeRegion(l,h);
    lastrange = -1; // discard last known range
  }

  public long getRange() {
    long res = ec.getRange();
    lastrange = res; // memorise last known range
    System.err.println("\033[35mEC.getR -> "+res+"\033[m");
    return res;
  }

}
