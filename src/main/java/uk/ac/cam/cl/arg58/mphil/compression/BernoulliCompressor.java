package uk.ac.cam.cl.arg58.mphil.compression;

import java.io.IOException;
import java.util.Iterator;

import uk.ac.cam.eng.ml.tcs27.compression.*;

/**
 * Created by adam on 04/03/16.
 */
public class BernoulliCompressor {
    public static void main(String[] args) throws IOException {
        if (args.length < 2 || args.length > 4) {
            System.err.println("USAGE: <COMPRESS|DECOMPRESS> <bias> [in] [out]\n");
            System.exit(-1);
        }

        Coder arith = new Arith();
        double bias = Double.valueOf(args[1]);
        Bernoulli<Bit> bernoulli = new Bernoulli<Bit>(bias, Bit.ZERO, Bit.ONE);

        BitReader in = null;
        if (args.length >= 3) {
            in = IOTools.getBitReader(args[2]);
        } else {
            in = new InputStreamBitReader(System.in);
        }

        BitWriter out = null;
        if (args.length >= 4) {
            out = IOTools.getBitWriter(args[3]);
        } else {
            out = new OutputStreamBitWriter(System.out);
        }

        if (args[0].equals("COMPRESS")) {
            arith.start_encode(out);

            Iterator<Bit> it = IOTools.bitIteratorFromBitReader(in);

            for (int i = 0; i < 80; i++) {
                Bit b = it.next();
                bernoulli.encode(b, arith);
            }

            arith.finish_encode();
            out.close(); // shouldn't be necessary, but there's no destructor
        } else if (args[0].equals("DECOMPRESS")) {
            arith.start_decode(in);

            for (int i = 0; i < 80; i++) {
                Bit b = bernoulli.decode(arith);
                out.writeBit(b);
            }

            arith.finish_decode();
            out.close(); // shouldn't be necessary, but there's no destructor
        } else {
            System.err.println("Illegal command " + args[0] + " expected either COMPRESS or DECOMPRESS.");
            System.exit(-1);
        }
    }
}
