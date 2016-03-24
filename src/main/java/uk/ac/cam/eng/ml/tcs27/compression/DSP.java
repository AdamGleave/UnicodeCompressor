/* Automated copy from build process */
/* $Id: DSP.java,v 1.1 2010/05/22 08:38:20 chris Exp $ */
import java.util.Random;

/** Interface common to discrete stochastic processes.
  * */
public interface DSP<X> {
  
  /** Advances the process and returns the next sample. */
  public X next(Random r);

}
