/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.io;

import com.tc.bytes.TCByteBuffer;
import com.tc.bytes.TCByteBufferFactory;
import com.tc.util.Assert;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UTFDataFormatException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Use me to write data to a set of TCByteBuffer instances. <br>
 * <br>
 * NOTE: This class never throws java.io.IOException (unlike the generic OutputStream) class
 */
public class TCByteBufferOutputStream extends OutputStream implements TCByteBufferOutput {

  private static final int       DEFAULT_MAX_BLOCK_SIZE     = 4096;
  private static final int       DEFAULT_INITIAL_BLOCK_SIZE = 32;

  private final boolean          direct;
  private final int              maxBlockSize;
  private final DataOutputStream dos;

  // The "buffers" list is accesed by index in the Mark class, thus it should not be a linked list
  private List                   buffers                    = new ArrayList();
  private Map                    localBuffers               = new IdentityHashMap();
  private TCByteBuffer           current;
  private boolean                closed;
  private int                    written;
  private int                    blockSize;

  // TODO: Provide a method to write buffers to another output stream
  // TODO: Provide a method to turn the buffers into an input stream with minimal cost (ie. no consolidation, no
  // duplicate(), etc)

  public TCByteBufferOutputStream() {
    this(DEFAULT_INITIAL_BLOCK_SIZE, DEFAULT_MAX_BLOCK_SIZE, false);
  }

  public TCByteBufferOutputStream(int blockSize, boolean direct) {
    this(blockSize, blockSize, false);
  }

  public TCByteBufferOutputStream(int initialBlockSize, int maxBlockSize, boolean direct) {
    if (maxBlockSize < 1) { throw new IllegalArgumentException("Max block size must be greater than or equal to 1"); }
    if (initialBlockSize < 1) { throw new IllegalArgumentException(
                                                                   "Initial block size must be greater than or equal to 1"); }

    if (maxBlockSize < initialBlockSize) { throw new IllegalArgumentException(
                                                                              "Initial block size less than max block size"); }

    this.maxBlockSize = maxBlockSize;
    this.blockSize = initialBlockSize;
    this.direct = direct;
    this.closed = false;
    this.dos = new DataOutputStream(this);
    addBuffer();
  }

  /**
   * Create a "mark" in this stream. A mark can be used to fixup data in an earlier portion of the stream even after you
   * have written past it. One place this is useful is when you need to backtrack and fill in a length field after
   * writing some arbitrary data to the stream
   */
  public Mark mark() {
    checkClosed();
    return new Mark(buffers.size(), current.position(), getBytesWritten());
  }

  public void write(int b) {
    checkClosed();

    written++;

    if (!current.hasRemaining()) {
      addBuffer();
    }

    current.put((byte) b);
  }

  public void write(byte b[]) {
    write(b, 0, b.length);
  }

  public void write(TCByteBuffer data) {
    if (data == null) { throw new NullPointerException(); }
    write(new TCByteBuffer[] { data });
  }

  /**
   * Add arbitrary buffers into the stream. All of the data (from position 0 to limit()) in each buffer passed will be
   * used in the stream. If that is not what you want, setup your buffers differently before calling this write()
   */
  public void write(TCByteBuffer[] data) {
    checkClosed();
    if (data == null) { throw new NullPointerException(); }
    if (data.length == 0) { return; }

    // deal with the current buffer
    final boolean reuseCurrent = current.position() == 0;

    if (!reuseCurrent) {
      // shrink and make it look like "full"
      buffers.add(current.limit(current.position()).position(0));
    }

    for (int i = 0, n = data.length; i < n; i++) {
      int len = data[i].limit();
      if (len == 0) {
        continue;
      }

      written += len;
      buffers.add(data[i].duplicate().position(0));
    }

    if (!reuseCurrent) {
      current = (TCByteBuffer) buffers.remove(buffers.size() - 1);
      current.position(current.limit());
    }
  }

  public int getBytesWritten() {
    return written;
  }

  public void write(byte b[], final int offset, final int length) {
    checkClosed();

    if (b == null) { throw new NullPointerException(); }

    if ((offset < 0) || (offset > b.length) || (length < 0) || ((offset + length) > b.length)) { throw new IndexOutOfBoundsException(); }

    if (length == 0) { return; }

    // do this after the checks (ie. don't corrupt the counter if bogus args passed)
    written += length;

    int index = offset;
    int numToWrite = length;
    while (numToWrite > 0) {
      if (!current.hasRemaining()) {
        addBuffer();
      }
      final int numToPut = Math.min(current.remaining(), numToWrite);
      current.put(b, index, numToPut);
      numToWrite -= numToPut;
      index += numToPut;
    }
  }

  public void close() {
    if (!closed) {
      finalizeBuffers();
      closed = true;
    }
  }

  /**
   * Obtain the contents of this stream as an array of TCByteBuffer
   */
  public TCByteBuffer[] toArray() {
    close();
    TCByteBuffer[] rv = new TCByteBuffer[buffers.size()];
    return (TCByteBuffer[]) buffers.toArray(rv);
  }

  public String toString() {
    return (buffers == null) ? "null" : buffers.toString();
  }

  private void addBuffer() {
    if (current != null) {
      current.flip();
      buffers.add(current);

      // use a buffer twice as big as the previous, at least until we hit the maximum block size allowed for this stream
      if (blockSize < maxBlockSize) {
        blockSize *= 2;

        if (blockSize > maxBlockSize) {
          blockSize = maxBlockSize;
        }
      }
    }

    current = TCByteBufferFactory.getInstance(direct, blockSize);
    blockSize = current.capacity();
    localBuffers.put(current, current);
  }

  private void finalizeBuffers() {
    if (current.position() > 0) {
      current.flip();
      buffers.add(current);
    }

    current = null;

    List finalBufs = new ArrayList();
    TCByteBuffer[] bufs = new TCByteBuffer[buffers.size()];
    bufs = (TCByteBuffer[]) buffers.toArray(bufs);

    final int num = bufs.length;
    int index = 0;

    // fixup "small" buffers consolidating them into buffers as close to maxBlockSize as possible
    while (index < num) {
      final int startIndex = index;
      int size = bufs[startIndex].limit();

      if (size < maxBlockSize) {
        while (index < (num - 1)) {
          int nextSize = bufs[index + 1].limit();
          if ((size + nextSize) <= maxBlockSize) {
            size += nextSize;
            index++;
          } else {
            break;
          }
        }
      }

      if (index > startIndex) {
        TCByteBuffer consolidated = TCByteBufferFactory.getInstance(direct, size);
        localBuffers.put(consolidated, consolidated);
        final int end = index;
        for (int i = startIndex; i <= end; i++) {
          consolidated.put(bufs[i]);
          if (localBuffers.remove(bufs[i]) != null) {
            bufs[i].recycle();
          }
        }
        Assert.assertEquals(size, consolidated.position());
        consolidated.flip();
        finalBufs.add(consolidated);
      } else {
        finalBufs.add(bufs[index]);
      }

      index++;
    }

    buffers = Collections.unmodifiableList(finalBufs);
  }

  public final void writeBoolean(boolean value) {
    try {
      dos.writeBoolean(value);
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

  public final void writeByte(int value) {
    try {
      dos.writeByte(value);
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

  public final void writeChar(int value) {
    try {
      dos.writeChar(value);
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

  public final void writeDouble(double value) {
    try {
      dos.writeDouble(value);
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

  public final void writeFloat(float value) {
    try {
      dos.writeFloat(value);
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

  public final void writeInt(int value) {
    try {
      dos.writeInt(value);
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

  public final void writeLong(long value) {
    try {
      dos.writeLong(value);
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

  public final void writeShort(int value) {
    try {
      dos.writeShort(value);
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

  public final void writeString(String string) {
    writeString(string, false);
  }

  public final void writeString(String string, boolean forceRaw) {
    // Is null? (true/false)
    if (string == null) {
      writeBoolean(true);
      return;
    } else {
      writeBoolean(false);
    }

    if (!forceRaw) {
      Mark mark = mark();
      // is UTF encoded? 1(true) or 0(false)
      write(1);

      try {
        dos.writeUTF(string);
        // No exception, just return
        return;
      } catch (IOException e) {
        if (!(e instanceof UTFDataFormatException)) { throw new AssertionError(e); }
        // String too long, encode as raw chars
        mark.write(0);
      }
    } else {
      write(0);
    }

    writeStringAsRawChars(string);
  }

  private void writeStringAsRawChars(String string) {
    if (string == null) { throw new AssertionError(); }
    writeInt(string.length());
    try {
      dos.writeChars(string);
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

  private void checkClosed() {
    if (closed) { throw new IllegalStateException("stream is closed"); }
  }

  // This class could be fancier:
  // - Support the TCDataOutput interface
  // - Allow writing through the mark to grow the buffer list
  // - etc, etc, etc
  public class Mark {
    private final int bufferIndex;
    private final int bufferPosition;
    private final int absolutePosition;

    private Mark(int bufferIndex, int bufferPosition, int absolutePosition) {
      this.bufferIndex = bufferIndex;
      this.bufferPosition = bufferPosition;
      this.absolutePosition = absolutePosition;
    }

    public int getPosition() {
      return this.absolutePosition;
    }

    /**
     * Write the given byte array at the position designated by this mark
     */
    public void write(byte[] data) {
      checkClosed();

      if (data == null) { throw new NullPointerException(); }

      if (data.length == 0) { return; }

      if (getBytesWritten() - absolutePosition < data.length) { throw new IllegalArgumentException(
                                                                                                   "Cannot write past the existing tail of stream via the mark"); }

      TCByteBuffer buf = getBuffer(bufferIndex);

      int bufIndex = bufferIndex;
      int bufPos = bufferPosition;
      int dataIndex = 0;
      int numToWrite = data.length;

      while (numToWrite > 0) {
        int howMany = Math.min(numToWrite, buf.limit() - bufPos);

        if (howMany > 0) {
          buf.put(bufPos, data, dataIndex, howMany);
          dataIndex += howMany;
          numToWrite -= howMany;
          if (numToWrite == 0) { return; }
        }

        buf = getBuffer(++bufIndex);
        bufPos = 0;
      }
    }

    private TCByteBuffer getBuffer(int index) {
      if (index <= buffers.size() - 1) {
        return (TCByteBuffer) buffers.get(index);
      } else if (index == buffers.size()) {
        return current;
      } else {
        throw Assert.failure("index=" + index + ", buffers.size()=" + buffers.size());
      }
    }

    /**
     * Write a single byte at the given mark. Calling write(int) multiple times will simply overwrite the same byte over
     * and over
     */
    public void write(int b) {
      write(new byte[] { (byte) b });
    }
  }

  public void recycle() {
    if (localBuffers.size() > 0) {
      for (Iterator i = localBuffers.keySet().iterator(); i.hasNext();) {
        TCByteBuffer buffer = (TCByteBuffer) i.next();
        buffer.recycle();
      }
    }
  }

}