/* $Id$ */
package uk.ac.cam.eng.ml.tcs27.compression;

/** Interface implemented by arithmetic decoders.
  * @see Encoder */
public interface Decoder {
  
  /** Returns the decoder's target within the specified range.
    * @return a number between 0 and t-1 (inclusive). */
  public long getTarget(long t);

  /** Advances the decoder to the specified region.
    * @param l low index (min=0, max=h-1)
    * @param h high index (min=l+1, max=t)
    * @param t range
    * @see Encoder#storeRegion(long,long,long) */
  public void loadRegion(long l, long h, long t);

  /** Returns the decoder's current coding range.
    * The coding range typically changes after each coding step.
    * Knowing the coder's range can help to reduce rounding
    * errors and maximise performance. */
  public long getRange();
  
  /** Returns the decoder's target in the decoder's current
    * coding range.
    * @return the current, unscaled decoding target
    * @see #getRange() */
  public long getTarget();
  
  /** Advances the decoder to the specified region.
    * This method performs no rescaling, and is useful when the
    * decoder's current coding range is known.
    * The coding range can be queried with {@code getRange()}.
    * @param l low index (min=0, max=h-1)
    * @param h high index (min=l+1, max=range)
    * @see #getRange()
    * @see Encoder#storeRegion(long,long) */
  public void loadRegion(long l, long h);
  
}
