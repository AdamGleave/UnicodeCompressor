package compression.tcs27.mlg.eng.cam.ac.uk;/* Automated copy from build process */
/* $Id: Encoder.java,v 1.3 2013/01/03 01:55:15 chris Exp $ */

/** Interface implemented by arithmetic encoders.
  * @see Decoder */
public interface Encoder {

  /** Advances the encoder to the specified region.
    * The specified range is used to rescale the given
    * region intervals to the coder's native coding range.
    * <p>For example, {@code storeRegion(0,1,2)} and
    * {@code storeRegion(1,2,2)} can be used to encode the
    * outcomes of a fair coin flip.</p>
    * @param l low index (min=0, max=h-1)
    * @param h high index (min=l+1, max=t)
    * @param t range
    * @see Decoder#loadRegion(long,long,long) */
  public void storeRegion(long l, long h, long t);
  
  /** Advances the encoder to the specified region.
    * This method performs no rescaling, and is useful when the
    * encoder's current coding range is known.
    * The coding range can be queried with {@code getRange()}.
    * @param l low index (min=0, max=h-1)
    * @param h high index (min=l+1, max=range)
    * @see #getRange()
    * @see Decoder#loadRegion(long,long) */
  public void storeRegion(long l, long h);

  /** Returns the encoder's current coding range.
    * The coding range typically changes after each coding step.
    * Knowing the coder's range can help to reduce rounding
    * errors and maximise performance. */
  public long getRange();

}

