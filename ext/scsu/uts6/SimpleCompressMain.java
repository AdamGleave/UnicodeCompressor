package uts6;

import java.io.*;
import java.util.*;

public class SimpleCompressMain {
  public static void encodeTest(boolean fDebug)
      throws IOException
  {
    String text = new Scanner(System.in).useDelimiter("\\A").next();
    System.err.println(text.length());

    // Create an instance of the compressor
    Compress compressor = new Compress();

    byte [] bytes = null;

    try
    {
      // perform compression
      bytes = compressor.compress(text);
    }
    catch (Exception e)
    {
      System.out.println(e);
    }

    System.out.write(bytes, 0, bytes.length);
  }

  public static void decodeTest(boolean fDebug) throws IOException
  {
    // HACK: just allocate a buffer that's large enough for any file in our test data
    // The original sample code CompressMain used available() which is even more wrong, CBA to fix it
    byte[] bytes = new byte[10*1024*1024];
    int len = System.in.read(bytes);
    if (len >= bytes.length) {
      throw new AssertionError("BUG: fixed buffer size too small to contain file.");
    }
    bytes = Arrays.copyOfRange(bytes, 0, len);

    Expand expand = new Expand();

    char [] chars = null;
    try
    {
      String text = expand.expand(bytes);
      chars = text.toCharArray();
    }
    catch (Exception e)
    {
      System.out.println(e);
    }
    if (chars == null)
      return;

    BufferedWriter out = new BufferedWriter(new OutputStreamWriter(System.out));
    System.err.println(chars.length);
    out.write(chars);
    out.flush();
  }
  
  public static void usage() {
    System.err.println("usage: <compress|decompress>. Reads from stdin, writes to stdout.");
    System.exit(1);
  }

  public static void main(String args[]) {
    try {
      if (args.length != 1)
        usage();
      switch (args[0]) {
        case "compress":
          encodeTest(false);
          break;
        case "decompress":
          decodeTest(false);
          break;
        default:
          usage();
          break;
      }
    } catch (IOException e) {
      System.err.println(e);
      System.exit(-1);
    }
  }
}
