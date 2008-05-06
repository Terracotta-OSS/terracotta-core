/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.io.serializer;

import com.tc.io.TCDataOutput;
import com.tc.object.dna.api.DNAEncoding;
import com.tc.object.dna.impl.SerializerDNAEncodingImpl;

import java.io.IOException;
import java.io.ObjectOutput;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

/**
 * <p>
 * ObjectOutputStream is totally inefficient when writing just a small amount of data. It creates a lot of garbage (2 or
 * 3 KB to just a create it) plus it also writes all the header/block header into the stream.
 * </p>
 * <p>
 * In the server when we serialize the ManagedObjects to sleepycat, we really dont need all the fancy things and we cant
 * afford to create such overhead. This class is an attempt to solve these problems without getting too complicated.
 * </p>
 * <p>
 * The writeObject() method in this class can only handle writing Literal Objects and will throw an AssertionError for
 * any other kind of object.
 * </p>
 * <p>
 * TCObjectInputStream compliments this class. Since TCDataOutput doesnt throw IOException and DataOutputStream has all
 * final methods, I have to reimplement DataOutputStream to not throw IOException.
 * </p>
 */
public class TCObjectOutputStream implements ObjectOutput, TCDataOutput {

  private static final DNAEncoding SERIALIZER_ENCODING = new SerializerDNAEncodingImpl();

  protected final OutputStream     out;

  public TCObjectOutputStream(OutputStream out) {
    this.out = out;
  }

  /**
   * This method is the reason I am writing this class. This implementation only can handle Literal Objects and is
   * designed to be used where writeObject() is called only for literal objects. Example : Sleepycat Serialization.
   * 
   * @see LiteralValues, DNAEncoding
   */
  public void writeObject(Object obj) {
    if (obj != null && obj.getClass().getName().charAt(0) == '[') {
      SERIALIZER_ENCODING.encodeArray(obj, this);
    } else {
      SERIALIZER_ENCODING.encode(obj, this);
    }
  }

  /**
   * This writes a 4 byte length and then the UTF String itself. If the length is negative, then it is a null string.
   * 
   * @throws IOException
   * @see writeUTF();
   */
  public void writeString(String string) {
    if (string == null) {
      writeInt(-1);
      return;
    }
    try {
      byte strbytes[] = string.getBytes("UTF-8");
      writeInt(strbytes.length);
      write(strbytes);
    } catch (UnsupportedEncodingException e) {
      throw new AssertionError(e);
    }
  }

  public void flush() {
    try {
      out.flush();
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

  public void writeBytes(String s) {
    int len = s.length();
    try {
      for (int i = 0; i < len; i++) {
        out.write((byte) s.charAt(i));
      }
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

  public void writeChars(String s) {
    int len = s.length();
    try {
      for (int i = 0; i < len; i++) {
        int v = s.charAt(i);
        out.write((v >>> 8) & 0xFF);
        out.write((v >>> 0) & 0xFF);
      }
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

  /**
   * This implemetation of writeUTF differes from the DataOutputStream's implementation in the following ways. 1) It
   * handles null strings. 2) It handles long strings (no 65K limit that UTF Encoding poses) 3) Cant be read by
   * DataInputStream. Use TCObjectInputStream.
   */
  public void writeUTF(String str) {
    writeString(str);
  }

  public void close() {
    flush();
    try {
      out.close();
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

  public void write(int b) {
    try {
      out.write(b);
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

  public void write(byte[] value) {
    write(value, 0, value.length);
  }

  public void write(byte[] value, int offset, int length) {
    try {
      out.write(value, offset, length);
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

  public void writeBoolean(boolean value) {
    try {
      out.write(value ? 1 : 0);
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

  public void writeByte(int value) {
    try {
      out.write(value);
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

  public void writeChar(int v) {
    try {
      out.write((v >>> 8) & 0xFF);
      out.write((v >>> 0) & 0xFF);
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

  public void writeDouble(double value) {
    writeLong(Double.doubleToLongBits(value));
  }

  public void writeFloat(float value) {
    writeInt(Float.floatToIntBits(value));
  }

  public void writeInt(int v) {
    try {
      out.write((v >>> 24) & 0xFF);
      out.write((v >>> 16) & 0xFF);
      out.write((v >>> 8) & 0xFF);
      out.write((v >>> 0) & 0xFF);
    } catch (IOException e) {
      throw new AssertionError(e);
    }

  }

  public void writeLong(long v) {
    final byte writeBuffer[] = new byte[8];
    writeBuffer[0] = (byte) (v >>> 56);
    writeBuffer[1] = (byte) (v >>> 48);
    writeBuffer[2] = (byte) (v >>> 40);
    writeBuffer[3] = (byte) (v >>> 32);
    writeBuffer[4] = (byte) (v >>> 24);
    writeBuffer[5] = (byte) (v >>> 16);
    writeBuffer[6] = (byte) (v >>> 8);
    writeBuffer[7] = (byte) (v >>> 0);
    try {
      out.write(writeBuffer, 0, 8);
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

  public void writeShort(int v) {
    try {
      out.write((v >>> 8) & 0xFF);
      out.write((v >>> 0) & 0xFF);
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

}
