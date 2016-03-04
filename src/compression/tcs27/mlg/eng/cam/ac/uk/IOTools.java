package compression.tcs27.mlg.eng.cam.ac.uk;/* Automated copy from build process */
/* $Id: IOTools.java,v 1.20 2015/07/30 18:15:48 chris Exp $ */

import java.util.Iterator;
import java.util.ArrayList;
import java.util.LinkedList;
import java.io.Reader;
import java.io.Writer;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.StringReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;


/** A collection of tools for handling various input and output tasks. */
public class IOTools {


  /** Creates a target iterator from a source iterator, using a function
    * which maps elements from source to target.
    * @param it source iterator
    * @param f mapping function
    * @return target iterator
    * @see #map(Iterable,Function) */
  public static <X,Y> Iterator<Y> map(final Iterator<X> it,
                                      final Function<X,Y> f) {
    return new Iterator<Y>() {
      public boolean hasNext() {
        return it.hasNext();
      }
      public Y next() {
        return f.eval(it.next());
      }
      public void remove() {
        it.remove();
      }
    };
  }

  /** Maps an iterable collection of elements from one type
    * to another, using a given mapping function.
    * @see #map(Iterator,Function) */
  public static <X,Y> Iterable<Y> map(final Iterable<X> col,
                                      final Function<X,Y> f) {
    return new Iterable<Y>() {
      public Iterator<Y> iterator() {
        return map(col.iterator(), f);
      }
    };
  }
  
  /** Creates an iterable concatenation of several iterables.
    * @param seqs source iterables
    * @return an iterable over the concatenation of the source iterables */
  public static <X> Iterable<X> concat(final Iterable<Iterable<X>> seqs) {
    return new Iterable<X>() {
      public final Iterator<X> iterator() {
        final Iterator<Iterable<X>> sit = seqs.iterator();
        return new Iterator<X>() {
          X next = null;
          Iterator<X> xit = null;
          boolean finished = false;
          private void fetch() {
            if (xit != null && xit.hasNext()) {
              // get next element
              next = xit.next();
            } else
            if (sit.hasNext()) {
              // get next sequence
              xit = sit.next().iterator();
              fetch();
            } else {
              // no more sequences, no more elements
              xit = null;
              next = null;
              finished = true;
            }
          }
          public boolean hasNext() {
            if (next == null && !finished) {
              fetch();
            }
            return (!finished);
          }
          public X next() {
            if (next == null && !finished) {
              fetch();
            }
            if (next == null && finished) {
              throw new java.util.NoSuchElementException();
            } else {
              X x = next;
              next = null;
              return x;
            }
          }
          public void remove() {
            throw new UnsupportedOperationException();
          }
        };
      }
    };
  }
  
  /** Creates an iterable concatenation of two iterables.
    * @param seq1 first iterable
    * @param seq2 second iterable
    * @return an iterable over both iterables */
  public static <X> Iterable<X> concat(Iterable<X> seq1, Iterable<X> seq2) {
    LinkedList<Iterable<X>> seqs = new LinkedList<Iterable<X>>();
    seqs.add(seq1);
    seqs.add(seq2);
    return concat(seqs);
  }

  @SuppressWarnings("unchecked")
  /** Creates an iterable concatenation of several iterables.
    * @param seqs source iterables
    * @return an iterable over the concatenation of the source iterables */
  public static <X> Iterable<X> concat(Iterable<X>... seqs) {
    return concat(java.util.Arrays.asList(seqs));
  }

  
  /** Returns a bit iterator view of a BitReader.
    * @param br input bit reader */
  public static Iterator<Bit>
                bitIteratorFromBitReader(final BitReader br) {
    return new Iterator<Bit>() {
      byte next = 2;  // 2: fetch more, 3: end-of-stream
      private void fetch() {
        try {
          next = br.readBit();
        }
        catch (IOException e1) {
          next = 3;  // end of stream
          try {
            br.close();
          } catch (IOException e2) {
            // ignore it
          }
        }
      }
      public boolean hasNext() {
        if (next == 2) { fetch(); }
        return (next != 3);
      }
      public Bit next() {
        if (next == 2) { fetch(); }
        if (next == 0) {
          next = 2;
          return Bit.ZERO;
        } else
        if (next == 1) {
          next = 2;
          return Bit.ONE;
        } else {
          throw new java.util.NoSuchElementException();
        }
      }
      public void remove() {
        throw new UnsupportedOperationException();
      }
    };
  }
  
  /** Returns an iterable bit sequence view of a BitReader.
    * @param br input bit reader */
  public static Iterable<Bit>
                    bitSequenceFromBitReader(final BitReader br) {
    return new Iterable<Bit>() {
      public final Iterator<Bit> iterator() {
        return bitIteratorFromBitReader(br);
      }
    };
  }
  
  /** Returns an iterable bit sequence view of a file.
    * @param fnm name of an existing file, or empty string to
    *            read from standard input */
  public static Iterable<Bit> bitSequenceFromFile(final String fnm) {
    return new Iterable<Bit>() {
      public Iterator<Bit> iterator() {
        try {
          BitReader br = getBitReader(fnm);
          return bitIteratorFromBitReader(br);
        }
        catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    };
  }


  /** Returns an byte iterator view of an InputStream.
    * @param is input stream */
  public static Iterator<Byte>
                byteIteratorFromInputStream(final InputStream is) {
    //final BufferedInputStream bis = new BufferedInputStream(is);
    return new Iterator<Byte>() {
      int next = -2;  // start of stream
      private void fetch() {
        try {
          next = is.read();
        }
        catch (IOException e1) {
          next = -1;  // end of stream
          try {
            is.close();
          } catch (IOException e2) {
            // ignore it
          }
        }
      }
      public boolean hasNext() {
        if (next == -2) {
          fetch();
        }
        return (next != -1);
      }
      public Byte next() {
        if (next == -2) {
          fetch();
        }
        if (next != -1) {
          Byte out = (byte) next;
          next = -2;
          return out;
        } else {
          // end of stream
          throw new java.util.NoSuchElementException();
        }
      }
      public void remove() {
        throw new UnsupportedOperationException();
      }
    };
  }
  
  /** Returns an iterable byte sequence view of an InputStream.
    * @param is input stream */
  public static Iterable<Byte>
                    byteSequenceFromInputStream(final InputStream is) {
    //final BufferedInputStream bis = new BufferedInputStream(is);
    return new Iterable<Byte>() {
      public final Iterator<Byte> iterator() {
        return byteIteratorFromInputStream(is);
      }
    };
  }
  
  /** Returns an iterable byte sequence view of a given file.
    * @param fnm name of an existing file, or empty string to
    *            read from standard input */
  public static Iterable<Byte> byteSequenceFromFile(final String fnm) {
    return new Iterable<Byte>() {
      public Iterator<Byte> iterator() {
        try {
          InputStream is;
          if (fnm == null || fnm.equals("")) {
            is = System.in;
          } else {
            is = new FileInputStream(fnm);
          }
          BufferedInputStream bis = new BufferedInputStream(is);
          return byteIteratorFromInputStream(bis);
        }
        catch (FileNotFoundException e) {
          throw new RuntimeException(e);
        }
      }
    };
  }

  /** Returns a char iterator view of a Reader.
    * @param r Reader */
  public static Iterator<Character> charIteratorFromReader(final Reader r) {
    return new Iterator<Character>() {
      int next = -2;
      private void fetch() {
        try {
          next = r.read();
        }
        catch (IOException e1) {
          next = -1;
          try {
            r.close();
          }
          catch (IOException e2) {
            // ignore
          }
        }
      }
      public boolean hasNext() {
        if (next == -2) {
          fetch();
        }
        return (next != -1);
      }
      public Character next() {
        if (next == -2) {
          fetch();
        }
        Character out = (char) next;
        next = -2;
        return out;
      }
      public void remove() {
        throw new UnsupportedOperationException();
      }
    };
  }
  
  /** Returns an iterable character sequence view of a Reader.
    * @param r Reader
    * @deprecated only the first invocation of iterator() will work */
  public static Iterable<Character> charSequenceFromReader(final Reader r) {
    // FIXME
    return new Iterable<Character>() {
      public final Iterator<Character> iterator() {
        return new Iterator<Character>() {
          int next = -2;
          private void fetch() {
            try {
              next = r.read();
            }
            catch (IOException e) {
              next = -1;
            }
          }
          public boolean hasNext() {
            if (next == -2) {
              fetch();
            }
            return (next != -1);
          }
          public Character next() {
            if (next == -2) {
              fetch();
            }
            Character out = (char) next;
            next = -2;
            return out;
          }
          public void remove() {
            throw new UnsupportedOperationException();
          }
        };
      }
    };
  }
  
  /** Returns an iterable integer sequence view of a Reader.
    * @param r Reader
    * @deprecated only the first invocation of iterator() will work */
  public static Iterable<Integer> intSequenceFromReader(final Reader r) {
    // FIXME
    return new Iterable<Integer>() {
      public final Iterator<Integer> iterator() {
        return new Iterator<Integer>() {
          int next = -2;
          private void fetch() {
            try {
              next = r.read();
            }
            catch (IOException e) {
              next = -1;
            }
          }
          public boolean hasNext() {
            if (next == -2) {
              fetch();
            }
            return (next != -1);
          }
          public Integer next() {
            if (next == -2) {
              fetch();
            }
            Integer out = next;
            next = -2;
            return out;
          }
          public void remove() {
            throw new UnsupportedOperationException();
          }
        };
      }
    };
  }

  /** Returns an iterable character sequence view for a text file.
    * @throws RuntimeException if the specified file can't be found
    * @param fnm filename of an existing text file, or the empty string
    *            (for standard input). */
  public static Iterable<Character> charSequenceFromFile(final String fnm) {
    return new Iterable<Character>() {
      public Iterator<Character> iterator() {
        try {
          BufferedReader bfr;
          if (fnm == null || fnm.equals("")) {
            bfr = new BufferedReader(new InputStreamReader(System.in));
          } else {
            bfr = new BufferedReader(new FileReader(fnm));
          }
          return charIteratorFromReader(bfr);
        }
        catch (FileNotFoundException e) {
          throw new RuntimeException(e);
        }
      }
    };
  }
  
  /** Returns an iterable character sequence view for a given string.
    * @param s string whose characters we wish to iterate over */
  public static Iterable<Character> charSequenceFromString(final String s) {
    return new Iterable<Character>() {
      public Iterator<Character> iterator() {
        return charIteratorFromReader(new StringReader(s));
      }
    };
  }
  
  /** Returns an iterable String sequence view for a text file.
    * The input is buffered and then read line by line, so each string
    * corresponds to one line of text.
    * @throws RuntimeException if the specified file can't be found
    * @param r a Reader object
    * @deprecated only the first invocation of iterator() will work */
  public static Iterable<String> stringSequenceFromReader(Reader r) {
    // FIXME
    final BufferedReader bfr = new BufferedReader(r);
    return new Iterable<String>() {
      public final Iterator<String> iterator() {
        return new Iterator<String>() {
          String next = null;
          boolean eof = false;
          private void fetch() {
            try {
              next = bfr.readLine();
            }
            catch (IOException e) {
              next = null;
              eof = true;
            }
            if (next == null) {
              eof = true;
            }
          }
          public boolean hasNext() {
            if (next == null && !eof) {
              fetch();
            }
            return (!eof);
          }
          public String next() {
            if (next == null && !eof) {
              fetch();
            }
            if (next == null && eof) {
              throw new java.util.NoSuchElementException();
            } else {
              String out = next;
              next = null;
              return out;
            }
          }
          public void remove() {
            throw new UnsupportedOperationException();
          }
        };
      }
    };
  }
  
  /** Returns an iterable String sequence view for a text file or
    * standard input.
    * The text file is read line by line, so each string corresponds
    * to one line of text.
    * If the supplied filename is the empty string, standard input
    * is used.
    * @throws RuntimeException if the specified file can't be found
    * @param fnm filename of an existing text file, or the empty string */
  public static Iterable<String> stringSequenceFromFile(String fnm)
                                           throws FileNotFoundException {
    if (fnm.equals("")) {
      return stringSequenceFromReader(new InputStreamReader(System.in));
    } else {
      return stringSequenceFromReader(new FileReader(fnm));
    }
  }
  
  

  /** Returns a BitReader which reads bits from a file or from stdin.
    * @param fnm the filename, or empty string "" for standard input. */
  public static BitReader getBitReader(String fnm) throws IOException {
    if (fnm.equals("")) {
      return new InputStreamBitReader(System.in);
    } else {
      BufferedInputStream bis = new BufferedInputStream(new FileInputStream(fnm));
      return new InputStreamBitReader(bis);
    }
  }
  
  /** Returns a BitWriter which writes bits to a file or to stdout.
    * @param fnm the filename, or empty string "" for standard output. */
  public static BitWriter getBitWriter(String fnm) throws IOException {
    if (fnm.equals("")) {
      return new OutputStreamBitWriter(System.out);
    } else {
      OutputStream os = new BufferedOutputStream(new FileOutputStream(fnm));
      return new OutputStreamBitWriter(os);
    }
  }

  /** Returns an InputStream from a file or from stdin.
    * @param fnm the filename, or empty string "" for standard input. */
  public static InputStream getInputStream(String fnm) throws IOException {
    if (fnm.equals("")) {
      return System.in;
    } else {
      FileInputStream fis = new FileInputStream(fnm);
      return new BufferedInputStream(fis);
    }
  }
  
  /** Returns an OutputStream to a file or to stdout.
    * @param fnm the filename, or empty string "" for standard output. */
  public static OutputStream getOutputStream(String fnm) throws IOException {
    if (fnm.equals("")) {
      return System.out;
    } else {
      FileOutputStream fos = new FileOutputStream(fnm);
      return new BufferedOutputStream(fos);
    }
  }
  
  /** Returns a Reader from a file or from stdin.
    * @param fnm the filename, or empty string for standard input. */
  public static Reader getReader(String fnm) throws IOException {
    if (fnm.equals("")) {
      return new InputStreamReader(System.in);
    } else {
      return new BufferedReader(new FileReader(fnm));
    }
  }
  
  /** Returns a Writer to a file or to stdout.
    * @param fnm the filename, or empty string for standard output. */
  public static Writer getWriter(String fnm) throws IOException {
    if (fnm.equals("")) {
      return new OutputStreamWriter(System.out);
    } else {
      return new BufferedWriter(new FileWriter(fnm));
    }
  }

  /** Copies an iterable source into an ArrayList. */
  public static <X> ArrayList<X> listFromIterable(Iterable<X> seq) {
    ArrayList<X> list = new ArrayList<X>();
    for (X x : seq) { list.add(x); }
    return list;
  }

  /** Splits an iterable sequence into two ArrayLists.
    * @param seq the source sequence
    * @param index the start index of the second sequence
    * @return two sequences: 0 to index-1, and index to end */
  public static <X> Tuple<ArrayList<X>,ArrayList<X>>
                split(Iterable<X> seq, int index) {
    ArrayList<X> a = new ArrayList<X>();
    ArrayList<X> b = new ArrayList<X>();
    int k=0;
    for (X x : seq) {
      if (k < index) {
        a.add(x);
        k++;
      } else {
        b.add(x);
      }
    }
    return Tuple.of(a,b);
  }

  /** Returns a hexadecimal String representation of the given
    * byte array. */
  public static String byteArrayToHexString(byte[] b) {
    StringBuilder sb = new StringBuilder();
    for (int i=0; i < b.length; i++) {
      sb.append(Integer.toString((b[i] & 0xff) + 0x100,16).substring(1));
    }
    return sb.toString();
  }

  
  /** Guesses the default encoding used on the system terminal.
    * If available, the contents of environment variables
    * <tt>LC_ALL</tt> and <tt>LC_CTYPE</tt> are examined (in this order).
    * If these are undefined, we're probing for the
    * <tt>sun.jnu.encoding</tt> system property of the JVM.
    * If that fails, the JVM's default charset (as defined by
    * <code>Charset.defaultCharset()</code>) is returned.<br>
    * <b>Special notes:</b> <ul>
    * <li>The JVM default encoding on MacOSX is set to MacRoman
    * (regardless of environment settings), but the underlying terminal
    * uses UTF-8 by default.  Hopefully, professional Mac users will have
    * set LC_CTYPE or LC_ALL accordingly, otherwise we'll trust their
    * JVM.</li>
    * <li>The Windows console (e.g. as seen from CYGWIN) seems
    * particularly uncooperative.  Luckily, the <tt>sun.jnu.encoding</tt>
    * property seems to help here - but names are not compatible with
    * the IANA Charset Registry names, and must be translated.</li>
    * </ul>
    * @return the canonical (java.nio) charset name */
  public static String getEncoding() {
    // get the JVM's default encoding
    String sysenc = Charset.defaultCharset().toString();
    // get sun.jnu.encoding property
    String jnuenc = System.getProperty("sun.jnu.encoding");
    if (jnuenc != null) {
      // map some known offenders from (java.io) to IANA (java.nio) name space
      if (jnuenc.startsWith("ANSI_X3")) {
        jnuenc = "US-ASCII";
      } else
      if (jnuenc.startsWith("Cp")) {
        jnuenc = "windows-"+jnuenc.substring(3);
      } else
      if (jnuenc.equals("MS932")) {
        jnuenc = "windows-31j";
      }
      // FIXME: this is majorly incomplete and ought to be handled by a
      // designated function
    }
    // now probe environment variables
    String envenc = null;
    String lc_all = System.getenv("LC_ALL");
    if (lc_all != null) {
      envenc = lc_all.substring(lc_all.indexOf("")+1);
    } else {
      String lc_ctype = System.getenv("LC_CTYPE");
      if (lc_ctype != null) {
        envenc = lc_ctype.substring(lc_ctype.indexOf("")+1);
      } /* else {
        String lang = System.getenv("LANG");
        if (lang != null) {
          envenc = lang.substring(lang.indexOf(".")+1);
        }
      } */
    }
    if (envenc != null && envenc.equals("C")) {
      envenc = "US-ASCII";
    }
    return (envenc != null) ? envenc : (jnuenc != null ? jnuenc : sysenc);
  }

  
 /** The CP437 character set, as seen in old DOS machines.
   * This could be used for displaying raw 8-bit data, since
   * every byte has a printable representation.
   * Byte 0xFF maps to a non-breaking space, which might be
   * worth replacing with some more visible glyph. */
 static final char[] CP437 = new char[] {
 '␀','☺','☻','♥','♦','♣','♠','•', '◘','○','◙','♂','♀', '♪','♫','☼',
 '►','◄','↕','‼','¶','§','▬','↨', '↑','↓','→','←','∟', '↔','▲','▼',
 ' ','!','"','#','$','%','&','\'','(',')','*','+',',', '-','.','/',
 '0','1','2','3','4','5','6','7', '8','9',':',';','<', '=','>','?',
 '@','A','B','C','D','E','F','G', 'H','I','J','K','L', 'M','N','O',
 'P','Q','R','S','T','U','V','W', 'X','Y','Z','[','\\',']','^','_',
 '`','a','b','c','d','e','f','g', 'h','i','j','k','l', 'm','n','o',
 'p','q','r','s','t','u','v','w', 'x','y','z','{','|', '}','~','⌂',
 'Ç','ü','é','â','ä','à','å','ç', 'ê','ë','è','ï','î', 'ì','Ä','Å',
 'É','æ','Æ','ô','ö','ò','û','ù', 'ÿ','Ö','Ü','¢','£', '¥','₧','ƒ',
 'á','í','ó','ú','ñ','Ñ','ª','º', '¿','⌐','¬','½','¼', '¡','«','»',
 '░','▒','▓','│','┤','╡','╢','╖', '╕','╣','║','╗','╝', '╜','╛','┐',
 '└','┴','┬','├','─','┼','╞','╟', '╚','╔','╩','╦','╠', '═','╬','╧',
 '╨','╤','╥','╙','╘','╒','╓','╫', '╪','┘','┌','█','▄', '▌','▐','▀',
 'α','ß','Γ','π','Σ','σ','µ','τ', 'Φ','Θ','Ω','δ','∞', 'φ','ε','∩',
 '≡','±','≥','≤','⌠','⌡','÷','≈', '°','·','•','√','ⁿ', '²','■',' '};
 //  ␀   ␁   ␂   ␃   ␄   ␅   ␆   ␇   ␈   ␉   ␊   ␋   ␌   ␍   ␎   ␏ 
 //  ␐   ␑   ␒   ␓   ␔   ␕   ␖   ␗   ␘   ␙   ␚   ␛   ␜   ␝   ␞   ␟


}
