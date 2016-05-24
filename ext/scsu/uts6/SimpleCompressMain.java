package uts6;

import java.io.*;
import java.util.Arrays;

/**
 * Created by adam on 24/05/16.
 */
public class SimpleCompressMain {
  private static int iMSB = 1;
  
  public static String readUnicodeFile() throws IOException
  {
    byte b[] = new byte[2];
    StringBuffer sb = new StringBuffer();
    char ch = 0;

    iMSB = 1;
    int i = 0;
    int cur = System.in.read();
    for(i = 0; cur != -1; cur = System.in.read(), i++)
    {
      b[i%2] = (byte)cur;

      if ((i & 1) == 1)
      {
        ch = Expand.charFromTwoBytes(b[(i + iMSB)%2], b[(i + iMSB + 1) % 2]);
      }
      else
      {
        continue;
      }
      if (i == 1 && ch == '\uFEFF')
        continue; // throw away byte order mark

      if (i == 1 && ch == '\uFFFE')
      {
        iMSB ++;  // flip byte order
        continue; // throw away byte order mark
      }
      sb.append(ch);
    }

    return sb.toString();
  }

  public static void writeUnicodeFile(char [] chars) throws IOException
  {
    DataOutputStream dos = new DataOutputStream(System.out);
    if ((iMSB & 1) == 1)
    {
      dos.writeByte(0xFF);
      dos.writeByte(0xFE);
    }
    else
    {
      dos.writeByte(0xFE);
      dos.writeByte(0xFF);
    }
    byte b[] = new byte[2];
    for (int ich = 0; ich < chars.length; ich++)
    {
      b[(iMSB + 0)%2] = (byte) (chars[ich] >>> 8);
      b[(iMSB + 1)%2] = (byte) (chars[ich] & 0xFF);
      System.out.write(b, 0, 2);
    }
  }


  public static void encodeTest(boolean fDebug)
      throws IOException
  {
    String text = readUnicodeFile();
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

    System.err.println(bytes.length);
    System.out.write(bytes, 0, bytes.length);
  }

  public static void decodeTest(boolean fDebug)
      throws IOException
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

    writeUnicodeFile(chars);
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
