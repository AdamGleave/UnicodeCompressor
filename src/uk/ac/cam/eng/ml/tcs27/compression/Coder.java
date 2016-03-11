package uk.ac.cam.eng.ml.tcs27.compression;
/* Automated copy from build process */
/* $Id: Coder.java,v 1.4 2012/10/22 21:46:49 chris Exp $ */

import java.io.IOException;

/** Interface for arithmetic coders. */
public interface Coder extends Encoder, Decoder {

    public void start_encode(BitWriter bw);
    public void finish_encode() throws IOException;

    public void start_decode(BitReader br) throws IOException;
    public void start_decode(BitReader br, boolean pad) throws IOException;
    public void finish_decode();

}
