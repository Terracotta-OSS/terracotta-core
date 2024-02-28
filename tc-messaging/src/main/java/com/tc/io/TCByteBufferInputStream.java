/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.io;

import com.tc.bytes.TCByteBuffer;
import com.tc.bytes.TCByteBufferFactory;
import com.tc.bytes.TCReferenceSupport;
import com.tc.bytes.TCReference;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.BufferUnderflowException;
import java.util.Iterator;
import java.util.stream.StreamSupport;

public class TCByteBufferInputStream extends InputStream implements TCByteBufferInput {
  private static final int            EOF                     = -1;
  private static final TCByteBuffer[] EMPTY_BYTE_BUFFER_ARRAY = new TCByteBuffer[] {};

  private final Runnable   onCloseHook;

  private final TCReference              data;
  private final Iterator<TCByteBuffer>     list;
  private TCByteBuffer current;
  private int                         totalLength;
  private boolean                     closed                  = false;


  public TCByteBufferInputStream(TCByteBuffer data) {
    this(new TCByteBuffer[] { data });
  }
    
  public TCByteBufferInputStream(TCReference data) {
    if (data == null) { throw new NullPointerException(); }
    
    this.data = data.duplicate();
    this.list = this.data.iterator();
    this.current = this.list.hasNext() ? this.list.next() : TCByteBufferFactory.getInstance(0);

    long length = StreamSupport.stream(this.data.spliterator(), false).map(TCByteBuffer::remaining).map(Integer::longValue).reduce(0L, Long::sum);

    if (length > Integer.MAX_VALUE) { throw new IllegalArgumentException("too much data: " + length); }

    this.totalLength = (int) length;

    this.onCloseHook = this.data::close;
  }
  
  public TCByteBufferInputStream(TCByteBuffer[] data) {
    this(TCReferenceSupport.createGCReference(data));
  }
  /**
   * Duplicate this stream. The resulting stream will share data with the source stream (ie. no copying), but the two
   * streams will have independent read positions. The read position of the result stream will initially be the same as
   * the source stream
   */
  @Override
  public TCByteBufferInput duplicate() {
    checkClosed();
    return new TCByteBufferInputStream(this.data);
  }
  
  @Override
  public int getTotalLength() {
    return this.totalLength;
  }

  @Override
  public int available() {
    return StreamSupport.stream(this.data.spliterator(), false).map(TCByteBuffer::remaining).reduce(0, Integer::sum);
  }

  @Override
  public void close() {
    if (!this.closed) {
      this.closed = true;
      this.onCloseHook.run();
    }
  }

  @Override
  public final int read(byte[] b, int off, int len) {
    checkClosed();

    if (b == null) { throw new NullPointerException(); }
    if ((off < 0) || (off > b.length) || (len < 0) || ((off + len) > b.length) || ((off + len) < 0)) { 
      throw new IndexOutOfBoundsException(); 
    }
    if (len == 0) { return 0; }

    if (available() == 0) { return EOF; }

    int bytesRead = 0;
    int numToRead = Math.min(available(), len);

    TCByteBuffer src = nextBuffer();
    while (src.hasRemaining()) {
      int read = Math.min(src.remaining(), numToRead);
      src.get(b, off, read);
      off += read;
      bytesRead += read;
      numToRead -= read;
      if (numToRead == 0) {
        break;
      }
      src = nextBuffer();
    }

    return bytesRead;
  }

  @Override
  public final int read(byte[] b) {
    return read(b, 0, b.length);
  }

  @Override
  public final TCByteBuffer read(int len) {
    checkClosed();

    if (len == 0) { return TCByteBufferFactory.getInstance(0); }

    if (available() == 0) { return TCByteBufferFactory.getInstance(0); }

    TCByteBuffer result = TCByteBufferFactory.getInstance(len);
    TCByteBuffer src = nextBuffer();
    while (result.hasRemaining()) {
      int limit = src.limit();
      src.limit(Math.min(src.position() + result.remaining(), limit));
      result.put(src);
      src.limit(limit);
      src = nextBuffer();
    }
    if (result.hasRemaining()) {
      throw new BufferUnderflowException();
    }
    return result.flip();
  }
  
  @Override
  public final TCReference readReference(final int len) {
    checkClosed();

    if (len == 0) { 
      return TCReferenceSupport.createGCReference(TCByteBufferFactory.getInstance(0)); 
    }
    
    TCReference dup = data.duplicate(len);
    long run = dup.available();
    skip(run);
    
    if (run != len) {
      throw new BufferUnderflowException(); 
    }
    return dup;
  }

  @Override
  public final int read() {
    checkClosed();
    
    TCByteBuffer src = nextBuffer();
    while (src.hasRemaining()) {
      if (src.hasRemaining()) {
        return src.get() & 0xFF;
      }
      src = nextBuffer();
    }
    return EOF;
  }

  private TCByteBuffer nextBuffer() {
    if (!current.hasRemaining() && this.list.hasNext()) {
      current = this.list.next();
    }
    return current;
  }

  @Override
  public long skip(long skip) {
    checkClosed();

    if (skip > Integer.MAX_VALUE) { throw new IllegalArgumentException("skip value too large: " + skip); }

    if ((skip <= 0) || (available() == 0)) { return 0; } // per java.io.InputStream.skip() javadoc

    int numToSkip = Math.min(available(), (int) skip);

    int bytesSkipped = 0;
    TCByteBuffer src = nextBuffer();
    while (src.hasRemaining()) {
      int remaining = src.remaining();
      if (remaining > 0) {
        int numToRead = Math.min(remaining, numToSkip);
        src.position(src.position() + numToRead);
        bytesSkipped += numToRead;
        numToSkip -= numToRead;
        if (numToSkip == 0) {
          break;
        }
      }
      src = nextBuffer();
    }

    return bytesSkipped;
  }

  private void checkClosed() {
    if (this.closed) { throw new IllegalStateException("stream is closed"); }
  }

  @Override
  public final int readInt() throws IOException {
    int byte1 = read();
    int byte2 = read();
    int byte3 = read();
    int byte4 = read();
    if ((byte1 | byte2 | byte3 | byte4) < 0) { throw new EOFException(); }
    return ((byte1 << 24) + (byte2 << 16) + (byte3 << 8) + (byte4 << 0));
  }

  @Override
  public final byte readByte() throws IOException {
    int b = read();
    if (b < 0) { throw new EOFException(); }
    return (byte) (b);
  }

  @Override
  public final boolean readBoolean() throws IOException {
    int b = read();
    if (b < 0) { throw new EOFException(); }
    return (b != 0);
  }

  @Override
  public final char readChar() throws IOException {
    int byte1 = read();
    int byte2 = read();
    if ((byte1 | byte2) < 0) { throw new EOFException(); }
    return (char) ((byte1 << 8) + (byte2 << 0));
  }

  @Override
  public final double readDouble() throws IOException {
    return Double.longBitsToDouble(readLong());
  }

  @Override
  public final long readLong() throws IOException {
    int byte1 = read();
    int byte2 = read();
    int byte3 = read();
    int byte4 = read();
    int byte5 = read();
    int byte6 = read();
    int byte7 = read();
    int byte8 = read();

    if ((byte1 | byte2 | byte3 | byte4 | byte5 | byte6 | byte7 | byte8) < 0) { throw new EOFException(); }

    return (((long) byte1 << 56) + ((long) (byte2 & 255) << 48) + ((long) (byte3 & 255) << 40)
            + ((long) (byte4 & 255) << 32) + ((long) (byte5 & 255) << 24) + ((byte6 & 255) << 16)
            + ((byte7 & 255) << 8) + ((byte8 & 255) << 0));
  }

  @Override
  public final float readFloat() throws IOException {
    return Float.intBitsToFloat(readInt());
  }

  @Override
  public final short readShort() throws IOException {
    int byte1 = read();
    int byte2 = read();
    if ((byte1 | byte2) < 0) { throw new EOFException(); }
    return (short) ((byte1 << 8) + (byte2 << 0));
  }

  @Override
  public final String readString() throws IOException {
    boolean isNull = readBoolean();
    if (isNull) { return null; }

    int utf = read();
    if (utf < 0) { throw new EOFException(); }

    switch (utf) {
      case 0: {
        return readStringFromChars();
      }
      case 1: {
        return DataInputStream.readUTF(this);
      }
      default:
        throw new AssertionError("utf = " + utf);
    }

    // unreachable
  }

  private String readStringFromChars() throws IOException {
    int len = readInt();

    // special case the empty String
    if (len == 0) { return ""; }

    char[] chars = new char[len];
    for (int i = 0, n = chars.length; i < n; i++) {
      chars[i] = readChar();
    }
    return new String(chars);
  }

  @Override
  public final void readFully(byte[] b) throws IOException {
    readFully(b, 0, b.length);
  }

  @Override
  public final void readFully(byte[] b, int off, int len) throws IOException {
    if (len < 0) { throw new IndexOutOfBoundsException(); }
    int n = 0;
    while (n < len) {
      int count = read(b, off + n, len - n);
      if (count < 0) { throw new EOFException(); }
      n += count;
    }
  }

  @Override
  public final int skipBytes(int n) {
    return (int) skip(n);
  }

  @Override
  public final int readUnsignedByte() throws IOException {
    int b = read();
    if (b < 0) { throw new EOFException(); }
    return b;
  }

  @Override
  public final int readUnsignedShort() throws IOException {
    int byte1 = read();
    int byte2 = read();
    if ((byte1 | byte2) < 0) { throw new EOFException(); }
    return (byte1 << 8) + (byte2 << 0);
  }

  @Override
  public final String readLine() {
    // Don't implement this method
    throw new UnsupportedOperationException();
  }

  @Override
  public final String readUTF() throws IOException {
    return readString();
  }
}
