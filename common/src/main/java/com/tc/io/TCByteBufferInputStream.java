/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.io;

import com.tc.bytes.TCByteBuffer;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class TCByteBufferInputStream extends InputStream implements TCDataInput, TCByteBufferInput {
  private static final int            EOF                     = -1;
  private static final TCByteBuffer[] EMPTY_BYTE_BUFFER_ARRAY = new TCByteBuffer[] {};

  private TCByteBuffer[]              data;
  private int                         totalLength;
  private int                         numBufs;
  private boolean                     closed                  = false;
  private int                         position                = 0;
  private int                         index                   = 0;
  private boolean                     marked                  = false;

  private TCByteBufferInputStream(TCByteBuffer[] sourceData, int dupeLength, int sourceIndex) {
    this(sourceData, dupeLength, sourceIndex, true);
  }

  public TCByteBufferInputStream(TCByteBuffer data) {
    this(new TCByteBuffer[] { data });
  }

  public TCByteBufferInputStream(TCByteBuffer[] data) {
    if (data == null) { throw new NullPointerException(); }

    long length = 0;

    this.data = new TCByteBuffer[data.length];

    for (int i = 0, n = data.length; i < n; i++) {
      TCByteBuffer buf = data[i];
      if (buf == null) { throw new NullPointerException("null buffer at index " + i); }

      this.data[i] = buf.duplicate().rewind();
      length += buf.limit();
    }

    if (length > Integer.MAX_VALUE) { throw new IllegalArgumentException("too much data: " + length); }

    this.numBufs = this.data.length;
    this.totalLength = (int) length;
  }

  private TCByteBufferInputStream(TCByteBuffer[] sourceData, int dupeLength, int sourceIndex, boolean duplicate) {
    // skipping checks. Invariants should hold since this is a private cstr()

    if (duplicate) {
      this.data = new TCByteBuffer[sourceData.length - sourceIndex];
    } else {
      this.data = sourceData;
    }

    this.numBufs = this.data.length;

    if (duplicate) {
      for (int i = 0, n = this.data.length; i < n; i++) {
        this.data[i] = sourceData[sourceIndex + i].duplicate();
      }
    }

    this.totalLength = dupeLength;
    this.position = 0;
    this.index = 0;
  }

  /**
   * Duplicate this stream. The resulting stream will share data with the source stream (ie. no copying), but the two
   * streams will have independent read positions. The read position of the result stream will initially be the same as
   * the source stream
   */
  public TCByteBufferInput duplicate() {
    checkClosed();
    return new TCByteBufferInputStream(this.data, available(), this.index);
  }

  /**
   * Effectively the same thing as calling duplicate().limit(int), but potentially creating far less garbage (depending
   * on the size difference between the original stream and the slice you want)
   */
  public TCByteBufferInput duplicateAndLimit(final int limit) {
    checkClosed();

    if (limit > available()) { throw new IllegalArgumentException("Not enough data left in stream: " + limit + " > "
                                                                  + available()); }

    if (limit == 0) { return new TCByteBufferInputStream(EMPTY_BYTE_BUFFER_ARRAY); }

    int numBytesNeeded = limit;
    int dataIndex = this.index;
    int lastLimit = -1;
    while (numBytesNeeded > 0) {
      TCByteBuffer buf = this.data[dataIndex];
      int numFromThisBuffer = Math.min(numBytesNeeded, buf.remaining());
      lastLimit = buf.position() + numFromThisBuffer;
      numBytesNeeded -= numFromThisBuffer;
      if (numBytesNeeded > 0) {
        dataIndex++;
      }
    }

    int size = (dataIndex - this.index) + 1;
    TCByteBuffer[] limitedData = new TCByteBuffer[size];
    for (int i = 0, n = limitedData.length; i < n; i++) {
      limitedData[i] = this.data[this.index + i].duplicate();
    }

    limitedData[limitedData.length - 1].limit(lastLimit);

    return new TCByteBufferInputStream(limitedData, limit, 0, false);
  }

  public TCByteBuffer[] toArray() {
    checkClosed();

    if (available() == 0) { return EMPTY_BYTE_BUFFER_ARRAY; }

    TCByteBuffer[] rv = new TCByteBuffer[this.numBufs - this.index];
    rv[0] = this.data[this.index].slice();
    for (int i = 1, n = rv.length; i < n; i++) {
      rv[i] = this.data[this.index + i].duplicate();
    }

    return rv;
  }

  public TCByteBuffer[] toArray(Mark s, Mark e) {
    checkClosed();

    TCMark start = validateMark(s);
    TCMark end = validateMark(e);
    if (start.getStreamPosition() > end.getStreamPosition()) {
      // wrong order, swapping
      TCMark temp = end;
      end = start;
      start = temp;
    } else if (start.getStreamPosition() == end.getStreamPosition()) { return EMPTY_BYTE_BUFFER_ARRAY; }

    TCByteBuffer[] rv = new TCByteBuffer[end.getBufferIndex() - start.getBufferIndex() + 1];
    for (int i = start.getBufferIndex(), idx = 0; i <= end.getBufferIndex(); i++, idx++) {
      rv[idx] = this.data[i].duplicate();
      rv[idx].position(0);
    }
    rv[0].position(start.getBufferPosition());
    rv[rv.length - 1].limit(end.getBufferPosition());
    // Creating a sliced view as many assume they can rewind this buffer and that limit is the size that can be read.
    rv[0] = rv[0].slice();

    return rv;
  }

  /**
   * Artificially limit the length of this input stream starting at the current read position. This operation is
   * destructive to the stream contents (ie. data trimmed off by setting limit can never be read with this stream).
   */
  public TCDataInput limit(int limit) {
    checkClosed();

    if (available() < limit) { throw new IllegalArgumentException("Not enough data left in stream: " + limit + " > "
                                                                  + available()); }

    List newData = new ArrayList();
    int num = limit;
    while (num > 0) {
      TCByteBuffer current = this.data[this.index];
      int avail = current.remaining();
      int take = Math.min(avail, num);
      if (take > 0) {
        newData.add(current.slice().limit(take));
        num -= take;
      }
      nextBuffer();
    }

    this.data = new TCByteBuffer[newData.size()];
    this.data = (TCByteBuffer[]) newData.toArray(this.data);
    this.numBufs = this.data.length;
    this.totalLength = limit;
    this.position = 0;
    this.index = 0;

    return this;
  }

  public int getTotalLength() {
    return this.totalLength;
  }

  @Override
  public int available() {
    return this.totalLength - this.position;
  }

  @Override
  public void close() {
    if (!this.closed) {
      this.closed = true;
      this.data = null;
    }
  }

  @Override
  public void mark(int readlimit) {
    throw new UnsupportedOperationException();
  }

  // XXX: This is a TC special version of mark() to be used in conjunction with tcReset()...We should eventually
  // implement the general purpose mark(int) method as specified by InputStream. NOTE: It has some unusual semantics
  // that make it a little trickier to implement (in our case) than you might think (specifially the readLimit field)
  public Mark mark() {
    checkClosed();
    this.marked = true;
    return new TCMark(this, this.index, this.data[this.index].position(), this.position);
  }

  @Override
  public boolean markSupported() {
    return false;
  }

  @Override
  public final int read(byte[] b, int off, int len) {
    checkClosed();

    if (b == null) { throw new NullPointerException(); }
    if ((off < 0) || (off > b.length) || (len < 0) || ((off + len) > b.length) || ((off + len) < 0)) { throw new IndexOutOfBoundsException(); }
    if (len == 0) { return 0; }

    if (available() == 0) { return EOF; }

    int bytesRead = 0;
    int numToRead = Math.min(available(), len);

    while (this.index < this.numBufs) {
      TCByteBuffer buf = this.data[this.index];
      if (buf.hasRemaining()) {
        int read = Math.min(buf.remaining(), numToRead);
        buf.get(b, off, read);
        off += read;
        this.position += read;
        bytesRead += read;
        numToRead -= read;
        if (numToRead == 0) {
          break;
        }
      }
      nextBuffer();
    }

    return bytesRead;
  }

  @Override
  public final int read(byte[] b) {
    return read(b, 0, b.length);
  }

  @Override
  public final int read() {
    checkClosed();

    while (this.index < this.numBufs) {
      if (this.data[this.index].hasRemaining()) {
        this.position++;
        return this.data[this.index].get() & 0xFF;
      }
      nextBuffer();
    }
    return EOF;
  }

  private void nextBuffer() {
    if (!this.marked) {
      this.data[this.index] = null;
    }
    this.index++;
  }

  @Override
  public void reset() {
    throw new UnsupportedOperationException();
  }

  /**
   * Reset this input stream to the position recorded by the mark that is passed an input parameter.
   * 
   * @throws IllegalArgumentException if m is null or if it was not created against this stream.
   */
  public void tcReset(Mark m) {
    checkClosed();

    TCMark mark = validateMark(m);

    int rewindToIndex = mark.getBufferIndex();
    while (this.index > rewindToIndex) {
      this.data[this.index].position(0);
      this.index--;
    }

    this.index = rewindToIndex;
    this.data[rewindToIndex].position(mark.getBufferPosition());
    this.position = mark.getStreamPosition();
  }

  private TCMark validateMark(Mark m) {
    if (m == null) { throw new IllegalArgumentException("Mark is null"); }
    if (!(m instanceof TCMark)) { throw new IllegalArgumentException("Illegal Mark type Exception"); }
    TCMark mark = (TCMark) m;
    if (mark.getStream() != this) { throw new IllegalArgumentException("Mark is not from this stream"); }
    return mark;
  }

  @Override
  public long skip(long skip) {
    checkClosed();

    if (skip > Integer.MAX_VALUE) { throw new IllegalArgumentException("skip value too large: " + skip); }

    if ((skip <= 0) || (available() == 0)) { return 0; } // per java.io.InputStream.skip() javadoc

    int numToSkip = Math.min(available(), (int) skip);

    int bytesSkipped = 0;
    while (this.index < this.numBufs) {
      TCByteBuffer buf = this.data[this.index];
      int remaining = buf.remaining();
      if (remaining > 0) {
        int numToRead = Math.min(remaining, numToSkip);
        buf.position(buf.position() + numToRead);
        this.position += numToRead;
        bytesSkipped += numToRead;
        numToSkip -= numToRead;
        if (numToSkip == 0) {
          break;
        }
      }
      nextBuffer();
    }

    return bytesSkipped;
  }

  private void checkClosed() {
    if (this.closed) { throw new IllegalStateException("stream is closed"); }
  }

  private static class TCMark implements Mark {
    private final int                     bufferPosition;
    private final int                     bufferIndex;
    private final int                     streamPosition;
    private final TCByteBufferInputStream stream;

    TCMark(TCByteBufferInputStream stream, int bufferIndex, int bufferPosition, int streamPosition) {
      this.stream = stream;
      this.bufferIndex = bufferIndex;
      this.bufferPosition = bufferPosition;
      this.streamPosition = streamPosition;
    }

    public TCByteBufferInputStream getStream() {
      return this.stream;
    }

    int getBufferIndex() {
      return this.bufferIndex;
    }

    int getBufferPosition() {
      return this.bufferPosition;
    }

    int getStreamPosition() {
      return this.streamPosition;
    }

    @Override
    public String toString() {
      return "Mark[TCByteBufferInputStream@" + System.identityHashCode(this.stream) + "] { Stream position : "
             + this.streamPosition + ", Buffer Index : " + this.bufferIndex + ", Buffer position : "
             + this.bufferPosition + " }";
    }
  }

  public final int readInt() throws IOException {
    int byte1 = read();
    int byte2 = read();
    int byte3 = read();
    int byte4 = read();
    if ((byte1 | byte2 | byte3 | byte4) < 0) { throw new EOFException(); }
    return ((byte1 << 24) + (byte2 << 16) + (byte3 << 8) + (byte4 << 0));
  }

  public final byte readByte() throws IOException {
    int b = read();
    if (b < 0) { throw new EOFException(); }
    return (byte) (b);
  }

  public final boolean readBoolean() throws IOException {
    int b = read();
    if (b < 0) { throw new EOFException(); }
    return (b != 0);
  }

  public final char readChar() throws IOException {
    int byte1 = read();
    int byte2 = read();
    if ((byte1 | byte2) < 0) { throw new EOFException(); }
    return (char) ((byte1 << 8) + (byte2 << 0));
  }

  public final double readDouble() throws IOException {
    return Double.longBitsToDouble(readLong());
  }

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

  public final float readFloat() throws IOException {
    return Float.intBitsToFloat(readInt());
  }

  public final short readShort() throws IOException {
    int byte1 = read();
    int byte2 = read();
    if ((byte1 | byte2) < 0) { throw new EOFException(); }
    return (short) ((byte1 << 8) + (byte2 << 0));
  }

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

  public final void readFully(byte[] b) throws IOException {
    readFully(b, 0, b.length);
  }

  public final void readFully(byte[] b, int off, int len) throws IOException {
    if (len < 0) { throw new IndexOutOfBoundsException(); }
    int n = 0;
    while (n < len) {
      int count = read(b, off + n, len - n);
      if (count < 0) { throw new EOFException(); }
      n += count;
    }
  }

  public final int skipBytes(int n) {
    return (int) skip(n);
  }

  public final int readUnsignedByte() throws IOException {
    int b = read();
    if (b < 0) { throw new EOFException(); }
    return b;
  }

  public final int readUnsignedShort() throws IOException {
    int byte1 = read();
    int byte2 = read();
    if ((byte1 | byte2) < 0) { throw new EOFException(); }
    return (byte1 << 8) + (byte2 << 0);
  }

  public final String readLine() {
    // Don't implement this method
    throw new UnsupportedOperationException();
  }

  public final String readUTF() {
    // Don't implement this method --> use readString() instead
    throw new UnsupportedOperationException();
  }
}
