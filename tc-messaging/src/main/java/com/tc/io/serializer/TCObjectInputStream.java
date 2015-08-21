/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.io.serializer;

import com.tc.io.TCDataInput;
import com.tc.object.dna.api.DNAEncoding;
import com.tc.object.dna.impl.SerializerDNAEncodingImpl;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;

/**
 * <p>
 * ObjectOutputStream/ObjectInputStream is totally inefficient when writing just a small amount of data. It creates a
 * lot of garbage (2 or 3 KB to just a create it) plus it also writes all the header/block header into the stream.
 * </p>
 * <p>
 * In the server when we serialize the ManagedObjects to sleepycat, we really dont need all the fancy things and we cant
 * afford to create such overhead. This class is an attempt to solve these problems without getting too complicated.
 * </p>
 * <p>
 * The readObject() method in this class can only handle reading Literal Objects and will throw an AssertionError for
 * any other kind of object.
 * </p>
 * <p>
 * TCObjectOutputStream compliments this class. Since methods DataInputStream are final methods and I want a different
 * write/readUTF() implementation, I have to reimplement DataInputStream.
 * </p>
 */
public class TCObjectInputStream implements ObjectInput, TCDataInput {

  private static final DNAEncoding SERIALIZER_ENCODING = new SerializerDNAEncodingImpl();

  private final InputStream        in;

  public TCObjectInputStream(InputStream in) {
    this.in = in;
  }

  /**
   * This method is the reason I am writing this class. This implementation only can handle Literal Objects and is
   * designed to be used where readObject() is called only for literal objects. Example : Sleepycat Serialization.
   * 
   * @see LiteralValues, DNAEncoding
   */
  @Override
  public Object readObject() throws ClassNotFoundException, IOException {
    return SERIALIZER_ENCODING.decode(this);
  }

  @Override
  public int read() throws IOException {
    return in.read();
  }

  @Override
  public int read(byte[] b) throws IOException {
    return in.read(b);
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    return in.read(b, off, len);
  }

  @Override
  public long skip(long n) throws IOException {
    return in.skip(n);
  }

  @Override
  public int available() throws IOException {
    return in.available();
  }

  @Override
  public void close() throws IOException {
    in.close();
  }

  @Override
  public void readFully(byte[] b) throws IOException {
    readFully(b, 0, b.length);
  }

  @Override
  public void readFully(byte[] b, int off, int len) throws IOException {
    if (len < 0) throw new IndexOutOfBoundsException();
    int n = 0;
    while (n < len) {
      int count = in.read(b, off + n, len - n);
      if (count < 0) throw new EOFException();
      n += count;
    }
  }

  @Override
  public int skipBytes(int n) throws IOException {
    int total = 0;
    int cur = 0;

    while ((total < n) && ((cur = (int) in.skip(n - total)) > 0)) {
      total += cur;
    }

    return total;
  }

  @Override
  public boolean readBoolean() throws IOException {
    int ch = in.read();
    if (ch < 0) throw new EOFException();
    return (ch != 0);
  }

  @Override
  public byte readByte() throws IOException {
    int ch = in.read();
    if (ch < 0) throw new EOFException();
    return (byte) (ch);
  }

  @Override
  public int readUnsignedByte() throws IOException {
    int ch = in.read();
    if (ch < 0) throw new EOFException();
    return ch;
  }

  @Override
  public short readShort() throws IOException {
    int ch1 = in.read();
    int ch2 = in.read();
    if ((ch1 | ch2) < 0) throw new EOFException();
    return (short) ((ch1 << 8) + (ch2 << 0));
  }

  @Override
  public int readUnsignedShort() throws IOException {
    int ch1 = in.read();
    int ch2 = in.read();
    if ((ch1 | ch2) < 0) throw new EOFException();
    return (ch1 << 8) + (ch2 << 0);
  }

  @Override
  public char readChar() throws IOException {
    int ch1 = in.read();
    int ch2 = in.read();
    if ((ch1 | ch2) < 0) throw new EOFException();
    return (char) ((ch1 << 8) + (ch2 << 0));
  }

  @Override
  public int readInt() throws IOException {
    int ch1 = in.read();
    int ch2 = in.read();
    int ch3 = in.read();
    int ch4 = in.read();
    if ((ch1 | ch2 | ch3 | ch4) < 0) throw new EOFException();
    return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));
  }

  @Override
  public long readLong() throws IOException {
    final byte readBuffer[] = new byte[8];
    readFully(readBuffer, 0, 8);
    return (((long) readBuffer[0] << 56) + ((long) (readBuffer[1] & 255) << 48) + ((long) (readBuffer[2] & 255) << 40)
            + ((long) (readBuffer[3] & 255) << 32) + ((long) (readBuffer[4] & 255) << 24)
            + ((readBuffer[5] & 255) << 16) + ((readBuffer[6] & 255) << 8) + ((readBuffer[7] & 255) << 0));
  }

  @Override
  public float readFloat() throws IOException {
    return Float.intBitsToFloat(readInt());
  }

  @Override
  public double readDouble() throws IOException {
    return Double.longBitsToDouble(readLong());
  }

  @Override
  public String readLine() {
    throw new UnsupportedOperationException("Use BufferedReader instead.");
  }

  /**
   * This implemetation of writeUTF differes from the DataOutputStream's implementation in the following ways. 1) It
   * handles null strings. 2) It handles long strings (no 65K limit that UTF Encoding poses) 3) Data should have been
   * written by TCObjectOutputStream.
   */
  @Override
  public String readUTF() throws IOException {
    return readString();
  }

  /**
   * This reads a 4 byte length and then the UTF String itself. If the length is negative, then it is a null string.
   * 
   * @throws IOException
   * @see writeUTF();
   */
  @Override
  public String readString() throws IOException {
    int len = readInt();
    if (len < 0) return null;
    else if (len == 0) return "";
    final byte strbytes[] = new byte[len];
    readFully(strbytes);
    return new String(strbytes, "UTF-8");
  }

}
