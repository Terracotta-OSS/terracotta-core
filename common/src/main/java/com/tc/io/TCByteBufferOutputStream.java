/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
import java.util.Iterator;
import java.util.List;

/**
 * Use me to write data to a set of TCByteBuffer instances. <br>
 * <br>
 * NOTE: This class never throws java.io.IOException (unlike the generic OutputStream) class
 */
public final class TCByteBufferOutputStream extends OutputStream implements TCByteBufferOutput {

  private static final int       DEFAULT_MAX_BLOCK_SIZE     = 4096;
  private static final int       DEFAULT_INITIAL_BLOCK_SIZE = 32;

  private final boolean          direct;
  private final int              maxBlockSize;
  private final DataOutputStream dos;

  // The "buffers" list is accessed by index in the Mark class, thus it should not be a linked list
  private List                   buffers                    = new ArrayList();

  private final List             localBuffers               = new ArrayList();

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
   * writing some arbitrary data to the stream. A mark can also be used to read earlier portions of the stream
   */
  public Mark mark() {
    checkClosed();
    return new Mark(buffers.size(), current.position(), getBytesWritten());
  }

  @Override
  public void write(int b) {
    checkClosed();

    written++;

    if (!current.hasRemaining()) {
      addBuffer();
    }

    current.put((byte) b);
  }

  @Override
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

    for (TCByteBuffer element : data) {
      int len = element.limit();
      if (len == 0) {
        continue;
      }

      written += len;
      buffers.add(element.duplicate().position(0));
    }

    if (!reuseCurrent) {
      current = (TCByteBuffer) buffers.remove(buffers.size() - 1);
      current.position(current.limit());
    }
  }

  public int getBytesWritten() {
    return written;
  }

  @Override
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

  @Override
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

  @Override
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

    current = newBuffer(blockSize);
    blockSize = current.capacity();
  }

  private TCByteBuffer newBuffer(int size) {
    TCByteBuffer rv = TCByteBufferFactory.getInstance(direct, size);
    localBuffers.add(rv);
    return rv;
  }

  private void finalizeBuffers() {
    if (current.position() > 0) {
      current.flip();
      buffers.add(current);
    }

    current = null;

    List finalBufs = new ArrayList();

    final int num = buffers.size();
    int index = 0;

    // fixup "small" buffers consolidating them into buffers as close to maxBlockSize as possible
    while (index < num) {
      final int startIndex = index;
      int size = ((TCByteBuffer) buffers.get(startIndex)).limit();

      if (size < maxBlockSize) {
        while (index < (num - 1)) {
          int nextSize = ((TCByteBuffer) buffers.get(index + 1)).limit();
          if ((size + nextSize) <= maxBlockSize) {
            size += nextSize;
            index++;
          } else {
            break;
          }
        }
      }

      if (index > startIndex) {
        TCByteBuffer consolidated = newBuffer(size);

        final int end = index;
        for (int i = startIndex; i <= end; i++) {
          consolidated.put((TCByteBuffer) buffers.get(i));
        }
        Assert.assertEquals(size, consolidated.position());
        consolidated.flip();
        finalBufs.add(consolidated);
      } else {
        finalBufs.add(buffers.get(index));
      }

      index++;
    }

    buffers = finalBufs;
  }

  public void writeBoolean(boolean value) {
    try {
      dos.writeBoolean(value);
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

  public void writeByte(int value) {
    try {
      dos.writeByte(value);
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

  public void writeChar(int value) {
    try {
      dos.writeChar(value);
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

  public void writeDouble(double value) {
    try {
      dos.writeDouble(value);
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

  public void writeFloat(float value) {
    try {
      dos.writeFloat(value);
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

  public void writeInt(int value) {
    try {
      dos.writeInt(value);
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

  public void writeLong(long value) {
    try {
      dos.writeLong(value);
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

  public void writeShort(int value) {
    try {
      dos.writeShort(value);
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

  public void writeString(String string) {
    writeString(string, false);
  }

  private void writeString(String string, boolean forceRaw) {
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
      int buffersSize = buffers.size();
      if (index < buffersSize) {
        return (TCByteBuffer) buffers.get(index);
      } else if (index == buffersSize) {
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

    /**
     * Copy (by invoking write() on the destination stream) the given length of bytes starting at this mark
     * 
     * @throws IOException
     */
    public void copyTo(TCByteBufferOutput dest, int length) {
      copyTo(dest, 0, length);
    }

    /**
     * Copy (by invoking write() on the destination stream) the given length of bytes starting from an offset to this
     * mark
     * 
     * @throws IOException
     */
    public void copyTo(TCByteBufferOutput dest, int offset, int length) {
      if (length < 0) { throw new IllegalArgumentException("length: " + length); }

      if (this.absolutePosition + offset + length > getBytesWritten()) {
        //
        throw new IllegalArgumentException("not enough data for copy of " + length + " bytes starting at position "
                                           + (this.absolutePosition + offset) + " of stream of size "
                                           + getBytesWritten());
      }

      int index = this.bufferIndex;
      int pos = this.bufferPosition;

      while (offset > 0) {
        byte[] array = getBuffer(index).array();
        int num = Math.min(array.length - pos, offset);
        offset -= num;
        if (offset == 0) {
          if (index > this.bufferIndex) {
            pos = num;
          } else {
            pos += num;
          }
          break;
        }

        pos = 0;
        index++;
      }

      while (length > 0) {
        byte[] array = getBuffer(index++).array();
        int num = Math.min(array.length - pos, length);
        dest.write(array, pos, num);
        length -= num;
        pos = 0;
      }
    }
  }

  public void recycle() {
    if (localBuffers.size() > 0) {
      for (Iterator i = localBuffers.iterator(); i.hasNext();) {
        TCByteBuffer buffer = (TCByteBuffer) i.next();
        buffer.recycle();
      }
    }
  }

  public void writeBytes(String s) {
    throw new UnsupportedOperationException("use writeString() instead");
  }

  public void writeChars(String s) {
    writeString(s, true);
  }

  public void writeUTF(String str) {
    throw new UnsupportedOperationException("use writeString() instead");
  }

}
