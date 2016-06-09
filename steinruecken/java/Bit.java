/* $Id$ */
package uk.ac.cam.eng.ml.tcs27.compression;

import java.io.Externalizable;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.IOException;
import java.lang.Iterable;
import java.lang.Comparable;
import java.util.Iterator;
import java.util.NoSuchElementException;

/** The Bit class represents immutable bit values. */
public class Bit extends Number implements Externalizable, Comparable<Bit> {
  
  /** A zero-bit, for easy reference. */
  public static final Bit ZERO = new Bit((byte) 0x00);

  /** A one-bit, for easy reference. */
  public static final Bit ONE  = new Bit((byte) 0x01);

  /** The bit value of this Bit instance: either 0x00 or 0x01. */
  protected byte value;
  
  /** Constructs a new Bit of given value.
    * You could also just use the constants Bit.ONE and Bit.ZERO.
    * @see ONE
    * @see ZERO */
  public Bit(boolean bit) {
    value = bit ? (byte) 1 : (byte) 0;
  }

  /** Constructs a new Bit of given value.
    * You could also just use the constants Bit.ONE and Bit.ZERO.
    * @see ONE
    * @see ZERO */
  public Bit(byte bit) {
    if (bit == 0) {
      value = (byte) 0;
    } else
    if (bit == 1) {
      value = (byte) 1;
    } else {
      throw new IllegalArgumentException();
    }
  }
  
  /** Constructs a new Bit of given value.
    * You could also just use the constants Bit.ONE and Bit.ZERO.
    * @see ONE
    * @see ZERO */
  public Bit(int bit) {
    if (bit == 0) {
      value = (byte) 0;
    } else
    if (bit == 1) {
      value = (byte) 1;
    } else {
      throw new IllegalArgumentException();
    }
  }
  

  /** Returns "0" if this bit is zero, and "1" otherwise. */
  public String toString() {
    if (value == 0x00) {
      return "0";
    } else {
      return "1";
    }
  }

  /** Returns true if and only if the given object is a Bit
    * and equal in value to this Bit instance. */
  public boolean equals(Object obj) {
    if (obj instanceof Bit) {
      return this.value == ((Bit)obj).value;
    };
    return false;
  }

  /** Returns a hash code for this Bit. */
  public int hashCode() {
    return (int) value;
  }

  /** Comparator method for bits. */
  public int compareTo(Bit b) {
    return Byte.compare(value,b.value);
  }

  /** Returns true if and only if this Bit is a zero-bit. */
  public boolean isZero() { return (value == 0x00); }

  /** Returns true if and only if this Bit is a one-bit. */
  public boolean isOne() { return (value != 0x00); }

  /** Returns the Bit value of a given boolean.
    * True returns Bit.ONE, false returns Bit.ZERO. */
  public Bit valueOf(boolean bit) {
    return bit ? ONE : ZERO;
  }

  /** Returns the Bit value of a given byte.
    * A byte value of 0 returns Bit.ZERO, all other byte
    * values return Bit.ONE. */
  public Bit valueOf(byte bit) {
    return (bit == (byte) 0x00) ? ZERO : ONE;
  }
  
  /** Returns the negation of the current bit. */
  public Bit negate() {
    return (value == 0x00) ? ONE : ZERO;
  }

  /** Constructs an iterator over possible bit values. */
  public Iterator<Bit> iterator() {
    return new Iterator<Bit>() {
      byte b = 0;
      public boolean hasNext() {
        return (b != 2);
      }
      public Bit next() {
        if (b == 0) {
          b = 1;
          return ZERO;
        } else
        if (b == 1) {
          b = 2;
          return ONE;
        } else {
          throw new NoSuchElementException();
        }
      }
    };
  }

  /** Serializes / stores a single Bit. */
  public void writeExternal(ObjectOutput out) throws IOException {
    if (value == 0x00) {
      out.write(0);
    } else
    if (value == 0x01) {
      out.write(1);
    } else {
      throw new IllegalStateException();
    }
  }
  
  /** Recovers a single Bit. */
  public void readExternal(ObjectInput in) throws IOException {
    value = (byte) in.read();
  }
  
  /** Returns the bit value as a byte. */
  public byte byteValue() {
    return value;
  }
  
  /** Returns the bit value as a double. */
  public double doubleValue() {
    if (value == 0) { return 0.0D; } else { return 1.0D; }
  }
  
  /** Returns the bit value as a float. */
  public float floatValue() {
    if (value == 0) { return 0.0F; } else { return 1.0F; }
  }
  
  /** Returns the bit value as an int. */
  public int intValue() {
    if (value == 0) { return 0; } else { return 1; }
  }
  
  /** Returns the bit value as an long. */
  public long longValue() {
    if (value == 0) { return 0L; } else { return 1L; }
  }
  
  /** Returns the bit value as a short. */
  public short shortValue() {
    if (value == 0) { return (short) 0; } else { return (short) 1; }
  }

}

