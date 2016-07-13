/* $Id$ */
package uk.ac.cam.eng.ml.tcs27.compression;

/** A debug wrapper around an existing decoder. */
public class DebugDecoder implements Decoder {

  Decoder dc = null;
  long lastrange = -1; // last known range of the encoder

  public DebugDecoder(Decoder dc) {
    this.dc = dc;
  }

  public void loadRegion(long l, long h, long t) {
    double logp = Math.log((double) (h-l) / t) / Tools.LN2;
    System.err.println("\033[32mDC.load "+l+", "+h+", "+t
                      +" \t\t("+logp+" bits)\033[m");
    dc.loadRegion(l,h,t);
    lastrange = -1; // discard last known range
  }
  
  public void loadRegion(long l, long h) {
    if (lastrange != -1) {
      double logp = Math.log((double) (h-l) / lastrange) / Tools.LN2;
      System.err.println("\033[32mDC.load "+l+", "+h
                        +" \t("+logp+" bits)\033[m");
    } else {
      System.err.println("\033[32mDC.load "+l+", "+h+"\033[m");
    }
    dc.loadRegion(l,h);
    lastrange = -1; // discard last known range
  }

  public long getTarget(long t) {
    long res = dc.getTarget(t);
    System.err.println("\033[34mDC.getT "+t+" -> "+res+"\033[m");
    return res;
  }
  
  public long getTarget() {
    long res = dc.getTarget();
    System.err.println("\033[34mDC.getT -> "+res+"\033[m");
    return res;
  }
  
  public long getRange() {
    long res = dc.getRange();
    System.err.println("\033[34mDC.getR -> "+res+"\033[m");
    lastrange = res; // memorise last known range
    return res;
  }

}
