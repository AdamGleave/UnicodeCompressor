package compression.tcs27.mlg.eng.cam.ac.uk;/* Automated copy from build process */
/* $Id: Codable.java,v 1.6 2011/09/13 23:55:22 chris Exp $ */

import java.util.Collection;
import java.util.Collections;

/** An interface for connecting a data model to an arithmetic coder. */
public interface Codable<X> {

  /** Encodes a symbol.
    * <p><b>Note:</b> Encoding must not affect the state of the
    * underlying distribution or process.</p>
    * @param x  the symbol to be encoded
    * @param ec the encoder to be used
    * @see #decode(Decoder) */
  public void encode(X x, Encoder ec);

  /** Encodes a symbol, excluding elements in the specified collection.
    * This method allows saving space by excluding elements
    * which are known in advance not to occur.
    * It is recommended to implement this method, but if this cannot
    * be done, at least place a call to the ordinary encode method
    * (which will work, though at the expense of wasting bandwidth).
    * @param omit collection of elements to be omitted
    * @param ec the encoder to be used
    * @see #decode(Collection,Decoder) */
  public void encode(X x, Collection<X> omit, Encoder ec);

  /** Decodes and returns a symbol.
    * <p><b>Note:</b> Decoding must not affect the state of the
    * underlying distribution or process.</p>
    * @param dc the decoder to be used
    * @return the decoded symbol
    * @see #encode(Object,Encoder) */
  public X decode(Decoder dc);
  
  /** Decodes and returns a symbol, excluding elements in
    * the specified collection.
    * This method allows saving space by excluding elements
    * which are known in advance not to occur.
    * It is recommended to implement this method, but if this cannot
    * be done, at least place a call to the ordinary decode method
    * (which will work, though at the expense of wasting bandwidth).
    * @param omit collection of elements to be omitted
    * @param dc the decoder to be used
    * @return the decoded symbol
    * @see #encode(Object,Collection,Encoder) */
  public X decode(Collection<X> omit, Decoder dc);

}
