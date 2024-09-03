/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.io;

import com.tc.bytes.TCByteBuffer;
import com.tc.bytes.TCByteBufferAllocator;
import com.tc.bytes.TCByteBufferFactory;
import com.tc.bytes.TCReference;
import com.tc.util.Assert;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UTFDataFormatException;
import java.io.UncheckedIOException;
import java.util.function.Supplier;

/**
 * Use me to write data to a set of TCByteBuffer instances. <br>
 * <br>
 * NOTE: This class never throws java.io.IOException (unlike the generic OutputStream) class
 */
public class TCByteBufferOutputStream extends OutputStream implements TCByteBufferOutput {

  private static final int       DEFAULT_MAX_BLOCK_SIZE     = 512 * 1024;
  private static final int       DEFAULT_INITIAL_BLOCK_SIZE = 1024;

  private final DataOutputStream dos;

  // The "buffers" list is accessed by index in the Mark class, thus it should not be a linked list
  private final TCByteBufferAllocator     buffers;

  private TCByteBuffer           current = TCByteBufferFactory.getInstance(0);
  private boolean                closed;
  private int                    written;

  // TODO: Provide a method to write buffers to another output stream
  // TODO: Provide a method to turn the buffers into an input stream with minimal cost (ie. no consolidation, no
  // duplicate(), etc)

  public TCByteBufferOutputStream() {
    this(DEFAULT_INITIAL_BLOCK_SIZE, DEFAULT_MAX_BLOCK_SIZE);
  }

  public TCByteBufferOutputStream(int blockSize) {
    this(blockSize, blockSize);
  }
  
  public TCByteBufferOutputStream(int init, int max) {
    this(new TCByteBufferAllocator(new Supplier<TCByteBuffer>() {
      private int blockSize = init;
      @Override
      public TCByteBuffer get() {
        try {
          return TCByteBufferFactory.getInstance(blockSize);
        } finally {
          blockSize <<= 1;
          if (blockSize > max) {
            blockSize = max;
          }
        }
      }
    }));
    if (init < 1) { throw new IllegalArgumentException("Max block size must be greater than or equal to 1"); }
    if (max < 1) { throw new IllegalArgumentException(
                                                                   "Initial block size must be greater than or equal to 1"); }
  }
    
  public TCByteBufferOutputStream(TCByteBufferAllocator bufferSrc) {
    this.buffers = bufferSrc;
    this.dos = new DataOutputStream(this);
  }

  @Override
  public void write(int b) {
    checkClosed();

    written++;

    checkBuffer();

    current.put((byte) b);
  }

  @Override
  public void write(byte b[]) {
    write(b, 0, b.length);
  }

  @Override
  public void write(TCByteBuffer data) {
    if (data == null) { throw new NullPointerException(); }
    write(new TCByteBuffer[] { data });
  }

  private void checkBuffer() {
    while (current == null || !current.hasRemaining()) {
      current = addBuffer();
    }
  }
  /**
   * Add arbitrary buffers into the stream. All of the data (from position 0 to limit()) in each buffer passed will be
   * used in the stream. If that is not what you want, setup your buffers differently before calling this write()
   */
  @Override
  public void write(TCByteBuffer[] data) {
    checkClosed();
    if (data == null) { throw new NullPointerException(); }
    if (data.length == 0) { return; }
    
    for (TCByteBuffer element : data) {
      int len = element.remaining();
      while (element.hasRemaining()) {
        checkBuffer();
        int saveLimit = element.limit();
        if (element.remaining() > current.remaining()) {
          element.limit(element.position() + current.remaining());
        }
        current.put(element);
        Assert.assertFalse(element.hasRemaining());
        element.limit(saveLimit);
      }
      written += len;
    }
  }

  public int getBytesWritten() {
    return written;
  }

  @Override
  public void write(byte b[], int offset, int length) {
    checkClosed();

    if (b == null) { throw new NullPointerException(); }

    if ((offset < 0) || (offset > b.length) || (length < 0) || ((offset + length) > b.length)) { throw new IndexOutOfBoundsException(); }

    if (length == 0) { return; }

    // do this after the checks (ie. don't corrupt the counter if bogus args passed)
    written += length;

    int index = offset;
    int numToWrite = length;
    while (numToWrite > 0) {
      checkBuffer();
      final int numToPut = Math.min(current.remaining(), numToWrite);
      current.put(b, index, numToPut);
      numToWrite -= numToPut;
      index += numToPut;
    }
  }

  @Override
  public void close() {
    if (!closed) {
      finalizeBuffer();
      closed = true;
    }
  }

  /**
   * Obtain the contents of this stream as an array of TCByteBuffer
   */
  @Override
  public TCReference accessBuffers() {
    close();
    return buffers.complete();
  }

  @Override
  public String toString() {
    return (buffers == null) ? "null" : buffers.toString();
  }
  
  private TCByteBuffer addBuffer() {
    finalizeBuffer();
    return this.buffers.add();
  }

  private void finalizeBuffer() {
    if (current != null) {
      current.flip();
      current = null;
    }
  }

  @Override
  public void writeBoolean(boolean value) {
    try {
      dos.writeBoolean(value);
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

  @Override
  public void writeByte(int value) {
    try {
      dos.writeByte(value);
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

  @Override
  public void writeChar(int value) {
    try {
      dos.writeChar(value);
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

  @Override
  public void writeDouble(double value) {
    try {
      dos.writeDouble(value);
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

  @Override
  public void writeFloat(float value) {
    try {
      dos.writeFloat(value);
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

  @Override
  public void writeInt(int value) {
    try {
      dos.writeInt(value);
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

  @Override
  public void writeLong(long value) {
    try {
      dos.writeLong(value);
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

  @Override
  public void writeShort(int value) {
    try {
      dos.writeShort(value);
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

  @Override
  public void writeString(String string) {
    writeString(string, false);
  }
  
  private void writeString(String string, boolean force) {
    // Is null? (true/false)
    if (string == null) {
      writeBoolean(true);
      return;
    } else {
      writeBoolean(false);
    }
    
    int mark = this.getBytesWritten();

    if (!force) {
      try {
        dos.write(1);
        dos.writeUTF(string);
        return;
      } catch (IOException ioe) {
        if (!(ioe instanceof UTFDataFormatException)) throw new UncheckedIOException(ioe);
        this.rewind(getBytesWritten() - mark);
      }
    }
    try {
      dos.write(0);
      writeStringAsRawChars(string);
    } catch (IOException ioe2) {
      throw new UncheckedIOException(ioe2);
    }
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

  @Override
  public void writeBytes(String s) {
    throw new UnsupportedOperationException("use writeString() instead");
  }

  @Override
  public void writeChars(String s) {
    writeString(s, true);
  }

  @Override
  public void writeUTF(String str) {
    writeString(str);
  }
  
  private void rewind(int bytes) {
    this.buffers.rewind(bytes);
  }

}
