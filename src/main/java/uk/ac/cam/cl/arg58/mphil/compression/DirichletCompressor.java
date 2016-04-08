package uk.ac.cam.cl.arg58.mphil.compression;

import uk.ac.cam.eng.ml.tcs27.compression.*;

import java.io.*;

public class DirichletCompressor {
  private static final Integer EOF = 256;

  public static void main(String[] args) throws IOException {
    if (args.length < 1 || args.length > 3) {
        System.err.println("USAGE: <COMPRESS|DECOMPRESS> [in] [out]\n");
        System.exit(-1);
    }

    Coder arith = new Arith();
    // 256 bytes + EOF symbol
    Dirichlet prior = new Dirichlet(257);
    AdamDDC ddc = new AdamDDC(prior);

    InputStream inStream = System.in;
    if (args.length >= 2) {
      inStream = new FileInputStream(args[1]);
    }

    OutputStream outStream = System.out;
    if (args.length >= 3) {
        outStream = new FileOutputStream(args[2]);
    }

    switch (args[0]) {
      case "COMPRESS": {
        OutputStreamBitWriter out = new OutputStreamBitWriter(outStream);
        arith.start_encode(out);

        Iterable<Byte> in = IOTools.byteSequenceFromInputStream(inStream);

        for (Byte b : in) {
          ddc.encode(b.intValue(), arith);
          ddc.learn(b.intValue());
        }
        ddc.encode(EOF, arith);

        arith.finish_encode();
        out.close();
        break;
      }
      case "DECOMPRESS": {
        InputStreamBitReader in = new InputStreamBitReader(inStream);
        arith.start_decode(in);

        Integer b;
        do {
          b = ddc.decode(arith);
          if (b.equals(EOF)) {
            break;
          }
          ddc.learn(b);
          outStream.write(b);
        } while (true);

        arith.finish_decode();
        outStream.close(); // shouldn't be necessary, but there's no destructor
        break;
      }
      default:
        System.err.println("Illegal command " + args[0] + " expected either COMPRESS or DECOMPRESS.");
        System.exit(-1);
        break;
    }
  }
}