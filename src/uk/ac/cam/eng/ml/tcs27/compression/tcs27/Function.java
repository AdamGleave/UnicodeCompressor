package uk.ac.cam.eng.ml.tcs27.compression.tcs27;/* Automated copy from build process */
/* $Id: Function.java,v 1.1 2012/04/11 15:38:41 chris Exp $ */

/** An interface for functions from elements of type A to elements
  * of type B. */
public interface Function<A,B> {

  public B eval(A a);

}
