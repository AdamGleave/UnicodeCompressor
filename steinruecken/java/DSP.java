/* $Id$ */
package uk.ac.cam.eng.ml.tcs27.compression;
import java.util.Random;

/** Interface common to discrete stochastic processes.
  * */
public interface DSP<X> {
  
  /** Advances the process and returns the next sample. */
  public X next(Random r);

}
