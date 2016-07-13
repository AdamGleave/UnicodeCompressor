/* $Id$ */
package uk.ac.cam.eng.ml.tcs27.compression;
import java.util.Random;
import java.io.*;
import java.util.Vector;
import java.util.Arrays;
import java.util.ArrayList;

/** A probabilistic file generator for various
  * simple probability distributions. */
public class FileGen {

  /** Two-valued distribution with entropy of exactly 0.5 bits. */
  public final double[] v2bh = { 0.889972135561640, 0.110027864438360 };
  /** Two-valued distribution with entropy of exactly 0.25 bits. */
  public final double[] v2bq = { 0.958307309726343, 0.0416926902736567 };
  /** Two-valued distribution with entropy of exactly 0.75 bits. */
  public final double[] v2b3q = { 0.785498255140171, 0.214501744859829 };
  /** Three-valued distribution with entropy of exactly 1 bit. */
  public final double[] v3b1 = { 0.7184599249125976, 0.25,
                                 0.03154007508740242 };
  /** Optimal distribution over two values A and B, when B costs
    * twice as much as A. */
  public final double[] a1b2 = { 0.618033988749895, 0.381966011250105 };


  public static Discrete<Character> getCharDist(String descr) {
    int i=0;
    int mode = 0;
    char sym = '@';
    String prob = "";
    ArrayList<Character> symbols = new ArrayList<Character>();
    ArrayList<Double> probs = new ArrayList<Double>();
    while (i < descr.length()) {
      char c = descr.charAt(i);
      switch (mode) {
        case 0: sym = c; mode = 1; break;
        case 1: if (c == ':') {
                  mode=2; break;
                } else {
                  throw new IllegalArgumentException("expected colon (:) at position "+i);
                }
        case 2: if (c != ',') {
                  prob+=c;
                } else {
                  mode=0;
                  symbols.add(sym);
                  probs.add(Double.parseDouble(prob));
                  prob="";
                }
                break;
       default: throw new IllegalStateException();
      }
      i++;
    }
    if (mode == 2) {
      symbols.add(sym);
      probs.add(Double.parseDouble(prob));
    }
    return new Discrete<Character>(symbols,probs);
  }
  
  /** Constructs a uniform distribution over a selected
    * subset of printable ASCII letters and numbers,
    * so that each symbol takes exactly 5 bits to encode. */
  public static DiscreteUniform<Character> uniform5Bit() {
    Character[] syms = {
      '@',
      'A','B','C','D','E','F','G','H','I','J','K','L','M',
      'N','O','P','Q','R','S','T','U','V','W','X','Y','Z',
      '[','\\',']','^','_'};
    return new DiscreteUniform<Character>(Arrays.asList(syms));
  }

  /** Constructs a uniform distribution over a selected
    * subset of printable ASCII letters and numbers,
    * so that each symbol takes exactly 6 bits to encode. */
  public static DiscreteUniform<Character> uniform6Bit() {
    Character[] syms = {
      ' ','0','1','2','3','4','5','6','7','8','9',
      'A','B','C','D','E','F','G','H','I','J','K','L','M',
      'N','O','P','Q','R','S','T','U','V','W','X','Y','Z',
      'a','b','c','d','e','f','g','h','i','j','k','l','m',
      'n','o','p','q','r','s','t','u','v','w','x','y','z',
      '!' };
    return new DiscreteUniform<Character>(Arrays.asList(syms));
  }
  
  /** Constructs a uniform distribution over the 256
    * Unicode Braille characters. */
  public static DiscreteUniform<Character> uniformBraille() {
    Character[] syms = {
    '⠀','⠁','⠂','⠃','⠄','⠅','⠆','⠇','⠈','⠉','⠊','⠋','⠌','⠍','⠎','⠏',
    '⠐','⠑','⠒','⠓','⠔','⠕','⠖','⠗','⠘','⠙','⠚','⠛','⠜','⠝','⠞','⠟',
    '⠠','⠡','⠢','⠣','⠤','⠥','⠦','⠧','⠨','⠩','⠪','⠫','⠬','⠭','⠮','⠯',
    '⠰','⠱','⠲','⠳','⠴','⠵','⠶','⠷','⠸','⠹','⠺','⠻','⠼','⠽','⠾','⠿',
    '⡀','⡁','⡂','⡃','⡄','⡅','⡆','⡇','⡈','⡉','⡊','⡋','⡌','⡍','⡎','⡏',
    '⡐','⡑','⡒','⡓','⡔','⡕','⡖','⡗','⡘','⡙','⡚','⡛','⡜','⡝','⡞','⡟',
    '⡠','⡡','⡢','⡣','⡤','⡥','⡦','⡧','⡨','⡩','⡪','⡫','⡬','⡭','⡮','⡯',
    '⡰','⡱','⡲','⡳','⡴','⡵','⡶','⡷','⡸','⡹','⡺','⡻','⡼','⡽','⡾','⡿',
    '⢀','⢁','⢂','⢃','⢄','⢅','⢆','⢇','⢈','⢉','⢊','⢋','⢌','⢍','⢎','⢏',
    '⢐','⢑','⢒','⢓','⢔','⢕','⢖','⢗','⢘','⢙','⢚','⢛','⢜','⢝','⢞','⢟',
    '⢠','⢡','⢢','⢣','⢤','⢥','⢦','⢧','⢨','⢩','⢪','⢫','⢬','⢭','⢮','⢯',
    '⢰','⢱','⢲','⢳','⢴','⢵','⢶','⢷','⢸','⢹','⢺','⢻','⢼','⢽','⢾','⢿',
    '⣀','⣁','⣂','⣃','⣄','⣅','⣆','⣇','⣈','⣉','⣊','⣋','⣌','⣍','⣎','⣏',
    '⣐','⣑','⣒','⣓','⣔','⣕','⣖','⣗','⣘','⣙','⣚','⣛','⣜','⣝','⣞','⣟',
    '⣠','⣡','⣢','⣣','⣤','⣥','⣦','⣧','⣨','⣩','⣪','⣫','⣬','⣭','⣮','⣯',
    '⣰','⣱','⣲','⣳','⣴','⣵','⣶','⣷','⣸','⣹','⣺','⣻','⣼','⣽','⣾','⣿' };
    return new DiscreteUniform<Character>(Arrays.asList(syms));
  }
  
  /** Constructs a uniform distribution over 7-bit ASCII
    * characters.
    * @see UniformChar#ascii() */
  public static UniformChar uniform7Bit() {
    return UniformChar.ascii();
  }
  
  /** Constructs a uniform distribution over 256 byte values.
    * @see UniformByte */
  public static UniformByte uniform8Bit() {
    return new UniformByte();
  }
  
  /** Constructs a uniform distribution over the set of
    * ASCII digits (0123456789). */
  public static UniformChar uniformDigits() {
    return UniformChar.asciiDigits();
  }

  public static void main(String[] args) throws Exception {
    final String self = "FileGen";
    Random rnd = new Random();
    String rfnm = null; // deterministic random bit sequence (input)
    String ifnm = null; // input sequence
    String tfnm = null; // training sequence
    String ofnm = null; // output sequence
    Distribution<Character> cdist = null;
    Iterable<Character> cset = null;
    Distribution<Byte> bdist = null;
    Iterable<Byte> bset = null;
    final int MODE_CHAR = 1;
    final int MODE_BYTE = 2;
    int mode = 0; // alphabet type (char / byte)
    int N = 0; // number of sampled symbols to generate
    int W = 0; // number of adversarial symbols to generate
    int Y = 0; // number of friendly symbols to generate
    boolean deterministic = false;
    for (int k=0; k<args.length; k++) {
      if (args[k].equals("-h") || args[k].equals("--help")) {
        System.out.println("Usage: java "+self+" [options] [FILE]");
        System.out.println("Generates a file with random content.");
        System.out.println("Options:");
        System.out.println("\t-h, --help   A helpful message");
        System.out.println("---------------------- INPUT MODES ----------------------");
        System.out.println("\t-u CHARS     uniform distribution over a set of CHARS");
        System.out.println("\t-d DIST      discrete distribution, e.g. \"A:0.4,B:0.4,X:0.2\"");
        System.out.println("\t-5bit        uniform distribution over 32 symbols");
        System.out.println("\t-6bit        uniform distribution over 64 symbols");
        System.out.println("\t-7bit        uniform distribution over 128 symbols");
        System.out.println("\t-8bit        uniform distribution over 256 symbols");
        System.out.println("\t-braille     uniform distribution over Unicode Braille symbols");
        System.out.println("\t-flm         fake language model: FakeLM");
        System.out.println("\t-pt A        generate from a Polya tree prior with");
        System.out.println("\t             concentration A (eg. 1)");
        System.out.println("\t-rnd SEED    set pseudo-random number generator seed");
        System.out.println("\t-x FILE      use random bit sequence from given FILE");
        System.out.println("\t-ib FILE     use byte input sequence from FILE");
        System.out.println("\t-ic FILE     use char input sequence from FILE");
        System.out.println("---------------------- TRANSFORMS ----------------------");
        System.out.println("\t-py A B      filter through a Pitman-Yor process with");
        System.out.println("\t             concentration A (eg. 1) and discount B (eg. 0)");
        System.out.println("\t-mtf         filter through a move-to-front transform");
        System.out.println("\t-ppm[:pars]  filter through PPM with optional parameters");
        System.out.println("\t-bbm[:pars]  filter through PY-PPM with optional parameters");
        System.out.println("\t-sm[:pars]   filter through a Sequence Memoizer with optional");
        System.out.println("\t             parameters after a colon, e.g. \"-sm:b=0.5,0.7,0.95\"");
        System.out.println("\t-lzw         filter through LZW");
        System.out.println("\t-t FILE      pre-train on FILE");
        System.out.println("---------------------- OUTPUT OPTIONS ----------------------");
        System.out.println("\t-g N         Generate N symbols (sampled randomly)");
        System.out.println("\t-y N         generate N friendly (most probable) symbols");
        System.out.println("\t-w N         generate N adversarial (least probable) symbols");
        //System.out.println("\t-o FNM       Set output filename");
        return;
      } else
      if (args[k].equals("-u")) {
        String symbols = "";
        if (k+1 < args.length) {
          k++;
          symbols=args[k];
        } else {
          System.err.println(self+": option '-u' requires an argument, e.g. ABC.");
          return;
        }
        Discrete<Character> d = Discrete.chars(symbols);
        cdist = d;
        cset = d;
        mode = MODE_CHAR;
      } else
      if (args[k].equals("-flm")) {
        cdist = new FakeLM();
        mode = MODE_CHAR;
      } else
      if (args[k].equals("-5bit")) {
        DiscreteUniform<Character> d = uniform5Bit();
        cdist = d; cset = d;
        mode = MODE_CHAR;
      } else
      if (args[k].equals("-6bit")) {
        DiscreteUniform<Character> d = uniform6Bit();
        cdist = d; cset = d;
        mode = MODE_CHAR;
      } else
      if (args[k].equals("-7bit")) {
        UniformChar d = uniform7Bit();
        cdist = d; cset = d;
        mode = MODE_CHAR;
      } else
      if (args[k].equals("-8bit")) {
        bdist = uniform8Bit();
        bset = uniform8Bit();
        mode = MODE_BYTE;
      } else
      if (args[k].equals("-braille")) {
        DiscreteUniform<Character> d = uniformBraille();
        cdist = d; cset = d;
        mode = MODE_CHAR;
      } else
      if (args[k].equals("-d")) {
        String descr = "";
        if (k+1 < args.length) {
          k++;
          descr=args[k];
        } else {
          System.err.println(self+": option '-d' requires an argument, e.g. 'A:0.6,B:0.2,C:0.2'.");
          return;
        }
        Discrete<Character> d = getCharDist(descr);
        cdist = d; cset = d;
        mode = MODE_CHAR;
      } else
      if (args[k].equals("-ib")) {
        k++;
        ifnm = args[k];
        bdist = FixedSeq.byteSeqFromFile(ifnm);
        bset = uniform8Bit();
        mode = MODE_BYTE;
      } else
      if (args[k].equals("-ic")) {
        k++;
        ifnm = args[k];
        cdist = FixedSeq.charSeqFromFile(ifnm);
        mode = MODE_CHAR;
        System.err.println(self+": warning: unknown character set.");
      } else
      if (args[k].equals("-py")) {
        double alpha;
        double beta;
        if (k+2 < args.length) {
          k++;
          alpha = Double.parseDouble(args[k]);
          k++;
          beta  = Double.parseDouble(args[k]);
        } else {
          System.err.println(self+": option '-py' requires two numeric arguments.");
          return;
        }
        if (mode == MODE_CHAR) {
          cdist = new CRPU<Character>(alpha, beta, cdist);
        } else
        if (mode == MODE_BYTE) {
          bdist = new CRPU<Byte>(alpha, beta, bdist);
        } else {
          System.err.println(self+": to use '-py', specify a base distribution first (e.g. -6bit)");
          return;
        }
      } else
      if (args[k].equals("-pt")) {
        double alpha;
        if (k+1 < args.length) {
          k++;
          alpha = Double.parseDouble(args[k]);
        } else {
          System.err.println(self+": option '-pt' requires"
                                 +" a numeric argument.");
          return;
        }
        if (mode == MODE_CHAR) {
          final ArrayList<Character> values = new ArrayList<Character>();
          for (Character c : cset) { values.add(c); }
          Distribution<Integer> idist = new SBST(0, values.size()-1, alpha);
          cdist = new MapDist<Integer,Character>(idist) {
            public Character raise(Integer i) { return values.get(i); }
            public Integer lower(Character c) { return values.indexOf(c); }
          };
        } else
        if (mode == MODE_BYTE) {
          final ArrayList<Byte> values = new ArrayList<Byte>();
          for (Byte b : bset) { values.add(b); }
          Distribution<Integer> idist = new SBST(0, values.size()-1, alpha);
          bdist = new MapDist<Integer,Byte>(idist) {
            public Byte raise(Integer i) { return values.get(i); }
            public Integer lower(Byte b) { return values.indexOf(b); }
          };
        } else {
          System.err.println(self+": to use '-pt', specify a base distribution first (e.g. -6bit)");
          return;
        }
      } else
      if (args[k].startsWith("-mtf")) { // Move-to-front transform
        // FIXME: check if MTF indices start from 0 or from 1
        if (mode == MODE_CHAR) {
          final ArrayList<Character> values = new ArrayList<Character>();
          for (Character c : cset) { values.add(c); }
          Distribution<Integer> idist = new MapDist<Character,Integer>(cdist) {
            public Integer raise(Character c) { return values.indexOf(c); }
            public Character lower(Integer i) { return values.get(i); }
          };
          cdist = new MTF<Character>(idist,cset);
        } else
        if (mode == MODE_BYTE) {
          final ArrayList<Byte> values = new ArrayList<Byte>();
          for (Byte b : bset) { values.add(b); }
          Distribution<Integer> idist = new MapDist<Byte,Integer>(bdist) {
            public Integer raise(Byte b) { return values.indexOf(b); }
            public Byte lower(Integer i) { return values.get(i); }
          };
          bdist = new MTF<Byte>(idist,bset);
        } else {
          System.err.println(self+": to use '-mtf', specify a base distribution first (e.g. -6bit -py 0 0.5)");
          return;
        }
      } else
      if (args[k].startsWith("-sm")) { // Sequence Memoizer
        String pars;
        if (args[k].length() > 4) {
          pars = args[k].substring(4);
        } else {
          pars = "b=0.5";
        }
        if (mode == MODE_CHAR) {
          cdist = RSTree.createNew(cdist,cset,pars);
        } else
        if (mode == MODE_BYTE) {
          bdist = RSTree.createNew(bdist,bset,pars);
        } else {
          System.err.println(self+": to use '-sm', specify a base distribution first (e.g. -6bit)");
          return;
        }
      } else
      if (args[k].startsWith("-ppm")) { // PPM
        String pars = "d=5:a=0:b=0.5"; // default (PPMD)
        if (args[k].length() > 4) {
          pars = args[k].substring(4);
        }
        if (mode == MODE_CHAR) {
          cdist = new PPM<Character>(pars,cdist);
        } else
        if (mode == MODE_BYTE) {
          bdist = new PPM<Byte>(pars,bdist);
        } else {
          System.err.println(self+": to use '-ppm', specify a base distribution first (e.g. -6bit)");
          return;
        }
      } else
      if (args[k].startsWith("-bbm")) { // PY-PPM
        String pars = "t=b:d=5:a=0.0:b=0.5";  // default
        if (args[k].length() > 4) {
          pars = args[k].substring(4);
        }
        if (mode == MODE_CHAR) {
          cdist = NPPM.createNew(pars,cdist,cset);
        } else
        if (mode == MODE_BYTE) {
          bdist = NPPM.createNew(pars,bdist,bset);
        } else {
          System.err.println(self+": to use '-bbm', specify a base distribution first (e.g. -6bit)");
          return;
        }
      } else
      if (args[k].startsWith("-lzw")) { // LZW
        if (mode == MODE_CHAR) {
          cdist = new LZW<Character>(cset);
        } else
        if (mode == MODE_BYTE) {
          bdist = new LZW<Byte>(bset);
        } else {
          System.err.println(self+": to use '-lzw', specify a symbol set first (e.g. -6bit)");
          return;
        }
      } else
      if (args[k].equals("-x")) {
        if (k+1 < args.length) {
          k++;
          rfnm = args[k];
          if (rfnm.equals("-")) { rfnm = ""; }
        } else {
          System.err.println(self+": option '-x' requires a filename or '-' for stdin.");
          return;
        }
      } else
      if (args[k].equals("-t")) {
        if (k+1 < args.length) {
          k++;
          tfnm = args[k];
          if (tfnm.equals("-")) { tfnm = ""; }
        } else {
          System.err.println(self+": option '-t' requires a filename or '-' for stdin.");
          return;
        }
      } else
      if (args[k].equals("-g")) {
        if (k+1 < args.length) {
          k++;
          N = Integer.decode(args[k]);
        } else {
          System.err.println(self+": option '-g' requires an integer argument, e.g 2000.");
          return;
        }
      } else
      if (args[k].equals("-w")) {
        if (k+1 < args.length) {
          k++;
          W = Integer.decode(args[k]);
          deterministic = true;
        } else {
          System.err.println(self+": option '-w' requires an integer argument, e.g 2000.");
          return;
        }
      } else
      if (args[k].equals("-W")) {
        if (k+1 < args.length) {
          k++;
          W = Integer.decode(args[k]);
          deterministic = false;
        } else {
          System.err.println(self+": option '-W' requires an integer argument, e.g 2000.");
          return;
        }
      } else
      if (args[k].equals("-y")) {
        if (k+1 < args.length) {
          k++;
          Y = Integer.decode(args[k]);
          deterministic = true;
        } else {
          System.err.println(self+": option '-y' requires an integer argument, e.g 2000.");
          return;
        }
      } else
      if (args[k].equals("-Y")) {
        if (k+1 < args.length) {
          k++;
          Y = Integer.decode(args[k]);
          deterministic = false;
        } else {
          System.err.println(self+": option '-Y' requires an integer argument, e.g 2000.");
          return;
        }
      } else
      if (args[k].equals("-rnd")) {
        if (k+1 < args.length) {
          k++;
          rnd = new Random(Integer.decode(args[k]));
        } else {
          System.err.println(self+": option '-rnd' requires an integer seed, e.g 42.");
          return;
        }
      } else
      if (args[k].startsWith("-")) {
        System.err.println(self+": unsupported option '"+args[k]+"'.");
        return;
      } else {
        if (ofnm == null) {
          ofnm = args[k];
        } else {
          System.err.println(self+": cannot generate more than one file at a time.");
          return;
        }
      }
    }
    /* Pretrain the chosen model, if requested. */
    if (tfnm != null) {
      if (mode == MODE_BYTE) {
        Iterable<Byte> bsrc = IOTools.byteSequenceFromFile(tfnm);
        for (Byte b : bsrc) {
          bdist.learn(b);
        }
      } else
      if (mode == MODE_CHAR) {
        Iterable<Character> csrc = IOTools.charSequenceFromFile(tfnm);
        for (Character c : csrc) {
          cdist.learn(c);
        }
      } else {
        System.err.println(self+": unsupported training mode.");
        return;
      }
    }
    /* Generate the output. */
    if (ofnm == null) {
      // let's use standard output
      ofnm="";
    }
    if (W > 0) {
      /** Generate adversarial sequences. */
      //RSTree<Character> rst = new RSTree<Character>(dist,alphabet);
      if (mode == MODE_CHAR) {
        Character x = null;
        Writer w = IOTools.getWriter(ofnm);
        if (deterministic) {
          System.err.println("Generating "+W+" deterministic adverse chars from: "+cdist);
          for (int n=0; n<W; n++) {
            x = Samplers.leastMass(cdist,cset);
            try { cdist.learn(x); } catch (UnsupportedOperationException e) { }
            w.write(x);
            w.flush();
          }
        } else {
          System.err.println("Generating "+W+" random adverse chars from: "+cdist);
          for (int n=0; n<W; n++) {
            x = Samplers.leastMass(cdist,cset,rnd);
            try { cdist.learn(x); } catch (UnsupportedOperationException e) { }
            w.write(x);
          }
        }
        w.close();
      } else
      if (mode == MODE_BYTE) {
        Byte x = null;
        OutputStream os = IOTools.getOutputStream(ofnm);
        if (deterministic) {
          System.err.println("Generating "+W+" deterministic adverse bytes from: "+bdist);
          for (int n=0; n<W; n++) {
            x = Samplers.leastMass(bdist,bset);
            try { bdist.learn(x); } catch (UnsupportedOperationException e) { }
            os.write(x);
          }
        } else {
          System.err.println("Generating "+W+" random adverse bytes from: "+bdist);
          for (int n=0; n<W; n++) {
            x = Samplers.leastMass(bdist,bset,rnd);
            try { bdist.learn(x); } catch (UnsupportedOperationException e) { }
            os.write(x);
          }
        }
        os.close();
      } else {
        System.err.println(self+": please specify a data model. Try '--help' for info.");
      }
    } else
    if (Y > 0) {
      /** Generate friendly sequences. */
      if (mode == MODE_CHAR) {
        Character x = null;
        Writer w = IOTools.getWriter(ofnm);
        if (deterministic) {
          System.err.println("Generating "+Y+" deterministic friendly chars from: "+cdist);
          for (int n=0; n<Y; n++) {
            x = Samplers.mostMass(cdist,cset);
            try { cdist.learn(x); } catch (UnsupportedOperationException e) { }
            w.write(x);
            w.flush();
          }
        } else {
          System.err.println("Generating "+Y+" random friendly chars from: "+cdist);
          for (int n=0; n<Y; n++) {
            x = Samplers.mostMass(cdist,cset,rnd);
            try { cdist.learn(x); } catch (UnsupportedOperationException e) { }
            w.write(x);
          }
        }
        w.close();
      } else
      if (mode == MODE_BYTE) {
        Byte x = null;
        OutputStream os = IOTools.getOutputStream(ofnm);
        if (deterministic) {
          System.err.println("Generating "+Y+" deterministic friendly bytes from: "+bdist);
          for (int n=0; n<Y; n++) {
            x = Samplers.mostMass(bdist,bset);
            try { bdist.learn(x); } catch (UnsupportedOperationException e) { }
            os.write(x);
          }
        } else {
          System.err.println("Generating "+Y+" random friendly bytes from: "+bdist);
          for (int n=0; n<Y; n++) {
            x = Samplers.mostMass(bdist,bset,rnd);
            try { bdist.learn(x); } catch (UnsupportedOperationException e) { }
            os.write(x);
          }
        }
        os.close();
      } else {
        System.err.println(self+": please specify a data model. Try '--help' for info.");
      }
    } else
    if (N > 0) {
      if (mode == MODE_CHAR) {
        System.err.println("Sampling "+N+" characters from: "+cdist);
        Writer w = IOTools.getWriter(ofnm);
        Character x = null;
        for (int n=0; n<N; n++) {
          x = cdist.sample(rnd);
          try { cdist.learn(x); } catch (UnsupportedOperationException e) { }
          w.write(x);
        }
        w.close();
      } else
      if (mode == MODE_BYTE) {
        System.err.println("Sampling "+N+" bytes from: "+bdist);
        OutputStream os = IOTools.getOutputStream(ofnm);
        Byte x = null;
        for (int n=0; n<N; n++) {
          x = bdist.sample(rnd);
          try { bdist.learn(x); } catch (UnsupportedOperationException e) { }
          os.write(x);
        }
        os.close();
      } else {
        System.err.println(self+": please specify a data model. Try '--help' for info.");
      }
    } else
    if (rfnm != null) {
      // decompress from binary file
      BitReader br = IOTools.getBitReader(rfnm);
      Arith ad = new Arith();
      ad.start_decode(br,true); // suppress first bit
      if (mode == MODE_CHAR) {
        Writer w = IOTools.getWriter(ofnm);
        while (br.informative()) {
          char x = cdist.decode(ad);
          try { cdist.learn(x); } catch (UnsupportedOperationException e) { }
          w.write(x);
          w.flush();
        }
        w.close();
      } else
      if (mode == MODE_BYTE) {
        OutputStream os = IOTools.getOutputStream(ofnm);
        while (br.informative()) {
          byte x = bdist.decode(ad);
          try { bdist.learn(x); } catch (UnsupportedOperationException e) { }
          os.write(x);
          os.flush();
        }
        os.close();
      } else {
        System.err.println(self+": please specify a data model. Try '--help' for info.");
      }
    } else {
      if (mode == 0) {
        System.err.println(self+": please specify data model and operation. Try '--help' for info.");
      } else {
        System.err.println(self+": please specify an operation (e.g. -g or -w). See '--help'.");
      }
    }
  }
}


