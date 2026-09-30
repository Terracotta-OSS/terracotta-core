/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2026
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

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Random;

import com.tc.bytes.TCByteBuffer;
import com.tc.bytes.TCByteBufferFactory;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TCByteBufferInputStreamTest {

  private final Random random = new SecureRandom();

  @Test
  public void testExceptions() {
    TCByteBufferInputStream inputStream = null;
    try {
      inputStream = new TCByteBufferInputStream((TCByteBuffer) null);
      fail();
    } catch (NullPointerException npe) {
      // expected
    } finally {
      if (inputStream != null) {
        inputStream.close();
      }
    }

    try {
      inputStream = new TCByteBufferInputStream((TCByteBuffer[]) null);
      fail();
    } catch (NullPointerException npe) {
      // expected
    } finally {
      if (inputStream != null) {
        inputStream.close();
      }
    }

    try {
      inputStream = new TCByteBufferInputStream(new TCByteBuffer[] { TCByteBufferFactory.getInstance(5), null });
      fail();
    } catch (NullPointerException npe) {
      // expected
    } finally {
      if (inputStream != null) {
        inputStream.close();
      }
    }
    {
      TCByteBufferInputStream bbis = null;
      try {
        bbis = new TCByteBufferInputStream(TCByteBufferFactory.getInstance(10));
        bbis.read(null);
        fail();
      } catch (NullPointerException npe) {
        // expected
      } finally {
        if (bbis != null) {
          bbis.close();
        }
      }
    }
    {
      TCByteBufferInputStream bbis = null;
      try {
        bbis = new TCByteBufferInputStream(TCByteBufferFactory.getInstance(10));
        bbis.read(null, 1, 10);
        fail();
      } catch (NullPointerException npe) {
        // expected
      } finally {
        bbis.close();
      }
    }
    {
      TCByteBufferInputStream bbis = null;
      try {
        bbis = new TCByteBufferInputStream(TCByteBufferFactory.getInstance(10));
        bbis.read(new byte[10], 10, 1);
        fail();
      } catch (IndexOutOfBoundsException ioobe) {
        // expected
      } finally {
        bbis.close();
      }
    }

    try {
      TCByteBufferInputStream bbis = new TCByteBufferInputStream(TCByteBufferFactory.getInstance(10));
      try {
        bbis.read(new byte[10], -1, 1);
        fail();
      } finally {
        bbis.close();
      }
    } catch (IndexOutOfBoundsException ioobe) {
      // expected
    }

    {
      TCByteBufferInputStream bbis = null;
      try {
        bbis = new TCByteBufferInputStream(TCByteBufferFactory.getInstance(10));
        bbis.read(new byte[10], 1, -1);
        fail();
      } catch (IndexOutOfBoundsException ioobe) {
        // expected
      } finally {
        if (bbis != null) {
          bbis.close();
        }
      }
    }

    TCByteBufferInputStream bbis = new TCByteBufferInputStream(TCByteBufferFactory.getInstance(10));
    try {
      for (int i = 0; i < 10; i++) {
        bbis.close();
      }
      try {
        bbis.read();
        fail();
      } catch (IllegalStateException ise) {
        // expected
      }
      try {
        bbis.read(new byte[1]);
        fail();
      } catch (IllegalStateException ise) {
        // expected
      }
      try {
        bbis.read(new byte[1], 0, 1);
        fail();
      } catch (IllegalStateException ise) {
        // expected
      }
    } finally {
      bbis.close();
    }
  }

  @Test
  public void testDuplicate() {
    for (int i = 0; i < 250; i++) {
      TCByteBuffer[] data = getRandomData();
      TCByteBufferInputStream bbis = new TCByteBufferInputStream(data);
      TCByteBufferInput dupe;
      try {
        bbis.read();
        dupe = bbis.duplicate();
        assertEquals(bbis.available(), dupe.available());
        int read = bbis.read();
        if (read != -1) {
          // reading from one stream doesn't affect the other
          assertEquals(dupe.available() - 1, bbis.available());
          int dupeRead = dupe.read();
          assertEquals(read, dupeRead);
        }
      } finally {
        bbis.close();
      }
      bbis = new TCByteBufferInputStream(getRandomDataNonZeroLength());
      try {
        int dupeStart = this.random.nextInt(bbis.getTotalLength());
        for (int n = 0; n < dupeStart; n++) {
          int b = bbis.read();
          assertTrue(b >= 0);
        }
        dupe = bbis.duplicate();
        while (bbis.available() > 0) {
          int n1 = bbis.read();
          int n2 = dupe.read();
          assertEquals(n1, n2);
        }
        assertEquals(0, dupe.available());
        assertEquals(0, bbis.available());
      } finally {
        bbis.close();
      }
    }
  }

  @Test
  public void testOffsetReadArray() {
    for (int i = 0; i < 25; i++) {
      testOffsetReadArray(getRandomData());
    }
  }

  private void testOffsetReadArray(TCByteBuffer[] data) {
    final int numBufs = data.length == 0 ? 0 : data.length - 1;

    final long totalLength = length(data);
    TCByteBufferInputStream bbis = new TCByteBufferInputStream(Arrays.asList(data).stream().map(TCByteBuffer::duplicate).toArray(TCByteBuffer[]::new));
    try {
      assertEquals(totalLength, bbis.available());
      assertEquals(totalLength, bbis.getTotalLength());
      int index = 0;
      int bytesRead = 0;
      while (bbis.available() > 0) {
        byte[] buffer = new byte[this.random.nextInt(50) + 1];
        byte fill = (byte) this.random.nextInt();
        Arrays.fill(buffer, fill);

        final int offset = this.random.nextInt(buffer.length);
        final int length = this.random.nextInt(buffer.length - offset);
        final int read = bbis.read(buffer, offset, length);
        if (read == -1) {
          break;
        }
        bytesRead += read;

        for (int i = 0; i < offset; i++) {
          assertEquals(fill, buffer[i]);
        }

        for (int i = offset + length + 1; i < buffer.length; i++) {
          assertEquals(fill, buffer[i]);
        }

        for (int i = 0; i < read; i++) {
          for (; !data[index].hasRemaining() && index < numBufs; index++) {
            //
          }

          assertEquals(data[index].get(), buffer[offset + i]);
        }
      }
      if (index < numBufs) {
        for (; index < numBufs; index++) {
          assertEquals(0, data[index + 1].limit());
        }
      }
      assertEquals(index, numBufs);
      if (numBufs > 0) {
        assertEquals(0, data[numBufs].remaining());
      }
      assertEquals(bytesRead, totalLength);
      assertEquals(0, bbis.available());
      assertEquals(-1, bbis.read());
      assertEquals(-1, bbis.read(new byte[10]));
      assertEquals(-1, bbis.read(new byte[10], 0, 3));
    } finally {
      bbis.close();
    }
  }

  @Test
  public void testReadBasics() {
    for (int i = 0; i < 250; i++) {
      testReadBasics(getRandomData());
    }
  }

  @Test
  public void testBasic() {
    for (int i = 0; i < 250; i++) {
      TCByteBuffer[] data = getRandomDataNonZeroLength();
      final byte b;
      TCByteBufferInputStream bbis = new TCByteBufferInputStream(Arrays.asList(data).stream().map(TCByteBuffer::duplicate).toArray(TCByteBuffer[]::new));

      try {
        assertTrue(bbis.available() > 0);
        b = data[0].get(0);
        int read = bbis.read();
        assertEquals(b, (byte) read);
      } finally {
        bbis.close();
      }
      byte[] readArray = new byte[1];
      bbis = new TCByteBufferInputStream(Arrays.asList(data).stream().map(TCByteBuffer::duplicate).toArray(TCByteBuffer[]::new));
      try {
        bbis.read(readArray);
        assertEquals(b, readArray[0]);
      } finally {
        bbis.close();
      }
      bbis = new TCByteBufferInputStream(Arrays.asList(data).stream().map(TCByteBuffer::duplicate).toArray(TCByteBuffer[]::new));
      try {
        bbis.read(readArray, 0, 1);
        assertEquals(b, readArray[0]);
      } finally {
        bbis.close();
      }
      bbis = new TCByteBufferInputStream(Arrays.asList(data).stream().map(TCByteBuffer::duplicate).toArray(TCByteBuffer[]::new));
      try {
        int avail = bbis.available();
        bbis.read(new byte[0]);
        assertEquals(avail, bbis.available());
        bbis.read(new byte[0], 0, 0);
        assertEquals(avail, bbis.available());
        bbis.read(new byte[10], 0, 0);
        assertEquals(avail, bbis.available());
      } finally {
        bbis.close();
      }
    }
  }

  private byte[] drain(TCByteBuffer[] bb) throws IOException {
    TCByteBufferInputStream bbis = new TCByteBufferInputStream(bb);
    try {
      byte b[] = new byte[bbis.available()];
      bbis.readFully(b);
      return b;
    } finally {
      bbis.close();
    }
  }

  private void testReadBasics(TCByteBuffer[] srcdata) {
    final int numBufs = srcdata.length == 0 ? 0 : srcdata.length - 1;

    final long len = length(srcdata);
    TCByteBufferInputStream bbis = new TCByteBufferInputStream(Arrays.asList(srcdata).stream().map(TCByteBuffer::duplicate).toArray(TCByteBuffer[]::new));
    try {
      assertEquals(len, bbis.available());
      assertEquals(len, bbis.getTotalLength());
      int index = 0;
      for (int i = 0; i < len; i++) {
        for (; !srcdata[index].hasRemaining() && index < numBufs; index++) {
          //
        }

        assertEquals(len - i, bbis.available());
        int fromStream = bbis.read();
        assertFalse(fromStream < 0);

        byte original = srcdata[index].get();

        assertEquals(original, (byte) fromStream);
      }
      if (index < numBufs) {
        for (; index < numBufs; index++) {
          assertEquals(0, srcdata[index + 1].limit());
        }
      }
      assertEquals(index, numBufs);
      if (numBufs > 0) {
        assertEquals(0, srcdata[numBufs].remaining());
      }
      assertEquals(0, bbis.available());
      assertEquals(-1, bbis.read());
      assertEquals(-1, bbis.read(new byte[10]));
      assertEquals(-1, bbis.read(new byte[10], 0, 3));
    } finally {
      bbis.close();
    }
  }

  @Test
  public void testTrailingZeroLength() {
    TCByteBuffer[] data = getRandomData();
    TCByteBuffer zeroLen = TCByteBufferFactory.getInstance(0);

    for (int i = 0; i < Math.min(10, data.length); i++) {
      data[data.length - i - 1] = zeroLen;
      rewindBuffers(data);
      testReadArrayBasics(data);

      rewindBuffers(data);
      testOffsetReadArray(data);
    }
  }

  @Test
  public void testRandomZeroLength() {
    TCByteBuffer[] data = getRandomDataNonZeroLength();

    TCByteBuffer zeroLen = TCByteBufferFactory.getInstance(0);

    int num = Math.min(25, data.length);

    for (int i = 0; i < num; i++) {
      data[this.random.nextInt(data.length)] = zeroLen;
      rewindBuffers(data);
      testReadArrayBasics(data);

      rewindBuffers(data);
      testOffsetReadArray(data);
    }
  }

  private TCByteBuffer[] getRandomDataNonZeroLength() {
    TCByteBuffer[] data;
    do {
      data = getRandomData();
    } while ((data.length == 0) || (data[0].limit() == 0));

    return data;
  }

  @Test
  public void testReadArrayBasics() {
    for (int i = 0; i < 50; i++) {
      testReadArrayBasics(getRandomData());
    }
  }

  private void rewindBuffers(TCByteBuffer[] data) {
    for (TCByteBuffer element : data) {
      element.rewind();
    }
  }

  private void testReadArrayBasics(TCByteBuffer[] data) {
    final int numBufs = data.length == 0 ? 0 : data.length - 1;

    final long len = length(data);
    TCByteBufferInputStream bbis = new TCByteBufferInputStream(Arrays.asList(data).stream().map(TCByteBuffer::duplicate).toArray(TCByteBuffer[]::new));
    try {
      assertEquals(len, bbis.available());
      assertEquals(len, bbis.getTotalLength());
      int counter = 0;
      int index = 0;
      while (bbis.available() > 0) {
        byte[] buffer = new byte[this.random.nextInt(10) + 1];
        int read = bbis.read(buffer);

        if (read <= 0) {
          assertEquals(0, bbis.available());
          break;
        }

        counter += read;
        assertEquals(len - counter, bbis.available());

        for (int i = 0; i < read; i++) {
          for (; !data[index].hasRemaining() && index < numBufs; index++) {
            //
          }
          assertEquals(data[index].get(), buffer[i]);
        }
      }
      if (index < numBufs) {
        for (; index < numBufs; index++) {
          assertEquals(0, data[index + 1].limit());
        }
      }
      assertEquals(numBufs, index);
      if (numBufs > 0) {
        assertEquals(0, data[numBufs].remaining());
      }
      assertEquals(0, bbis.available());
      assertEquals(-1, bbis.read());
      assertEquals(-1, bbis.read(new byte[10]));
      assertEquals(-1, bbis.read(new byte[10], 0, 3));
    } finally {
      bbis.close();
    }
  }

  @Test
  public void testSkip() {
    for (int i = 0; i < 250; i++) {
      TCByteBuffer[] data = getRandomDataNonZeroLength();
      TCByteBufferInputStream is = new TCByteBufferInputStream(data);

      try {
        assertEquals(0, is.skip(0));
        assertEquals(0, is.skip(-1));
        int len = is.getTotalLength();
        assertEquals(len, is.available());
        long skipped = is.skip(len - 1);
        assertEquals(len - 1, skipped);
        assertEquals(1, is.available());
        is.read();
        assertEquals(0, is.available());
        int read = is.read();
        assertEquals(-1, read);
      } finally {
        is.close();
      }
    }
  }

  @Test
  public void testZeroLength() {
    TCByteBuffer[] data = new TCByteBuffer[] {};
    long len = length(data);
    assertEquals(0, len);
    TCByteBufferInputStream bbis = new TCByteBufferInputStream(data);
    try {
      assertEquals(0, bbis.available());
      assertEquals(0, bbis.getTotalLength());
      assertEquals(-1, bbis.read());
      assertEquals(-1, bbis.read(new byte[10]));
      assertEquals(-1, bbis.read(new byte[10], 1, 3));
      assertEquals(0, bbis.read(new byte[10], 1, 0));
      testReadArrayBasics(data);
      testReadBasics(data);
      testOffsetReadArray(data);
    } finally {
      bbis.close();
    }
  }

  @Test
  public void testGetBytesOptimization() {
    TCByteBuffer[] data = new TCByteBuffer[] {TCByteBufferFactory.wrap("the quick brown fox jumped over the cow".getBytes())};

    TCByteBufferInputStream bbis = new TCByteBufferInputStream(data);
    try {
      TCByteBuffer buf = bbis.read(9);
      for (byte b : "the quick".getBytes()) {
        assertEquals(b, buf.get());
      }
    } finally {
      bbis.close();
    }
  }

  private long length(TCByteBuffer[] data) {
    long rv = 0;
    for (TCByteBuffer element : data) {
      rv += element.limit();
    }
    return rv;
  }

  private TCByteBuffer[] createBuffersWithRandomData(int number, int size) {
    TCByteBuffer[] rv = new TCByteBuffer[number];
    for (int i = 0; i < rv.length; i++) {
      rv[i] = TCByteBufferFactory.getInstance(size);
      byte[] bites = new byte[size];
      this.random.nextBytes(bites);
      rv[i].put(bites);
      rv[i].flip();
    }

    return rv;
  }

  private TCByteBuffer[] getRandomData() {
    final int num = this.random.nextInt(20);
    final int maxSize = this.random.nextInt(200);

    TCByteBuffer[] rv = new TCByteBuffer[num];

    for (int i = 0; i < num; i++) {
      rv[i] = TCByteBufferFactory.getInstance(maxSize > 0 ? this.random.nextInt(maxSize) : 0);
      this.random.nextBytes(rv[i].array());
    }

    return rv;
  }

  private static TCByteBuffer[] wrapPerByte(byte[] data) {
    TCByteBuffer[] rv = new TCByteBuffer[data.length];
    for (int i = 0; i < data.length; i++) {
      rv[i] = TCByteBufferFactory.wrap(new byte[] { data[i] });
    }
    return rv;
  }

  private static TCByteBufferInputStream streamOver(TCByteBuffer[] buffers) {
    return new TCByteBufferInputStream(Arrays.stream(buffers).map(TCByteBuffer::duplicate).toArray(TCByteBuffer[]::new));
  }

  private static TCByteBufferInputStream streamOver(byte[] data) {
    return streamOver(new TCByteBuffer[] { TCByteBufferFactory.wrap(data) });
  }

  @Test
  public void testPrimitiveGoldenBytes() throws IOException {
    // big-endian wire order, chosen so every byte is distinct
    byte[] data = new byte[] { 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F, 0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17,
        0x18, 0x19, 0x1A, 0x1B, 0x1C, 0x1D };

    TCByteBufferInputStream in = streamOver(data);
    try {
      assertEquals(0x0A0B0C0D, in.readInt());
      assertEquals(0x0E0F101112131415L, in.readLong());
      assertEquals((char) 0x1617, in.readChar());
      assertEquals((short) 0x1819, in.readShort());
      assertEquals(0x1A1B, in.readUnsignedShort());
      assertEquals(0x1C, in.readUnsignedByte());
      assertEquals((byte) 0x1D, in.readByte());
      assertTrue(streamOver(new byte[] { 1 }).readBoolean());
      assertFalse(streamOver(new byte[] { 0 }).readBoolean());
      assertEquals(0, in.available());
    } finally {
      in.close();
    }
  }

  @Test
  public void testPrimitiveRoundTrip() throws IOException {
    for (int blockSize = 1; blockSize <= 3; blockSize++) {
      long longValue = random.nextLong();
      int intValue = random.nextInt();
      short shortValue = (short) random.nextInt();
      char charValue = (char) random.nextInt();
      double doubleValue = Double.longBitsToDouble(random.nextLong());
      float floatValue = Float.intBitsToFloat(random.nextInt());
      boolean boolValue = random.nextBoolean();

      TCByteBufferOutputStream out = new TCByteBufferOutputStream(blockSize);
      out.writeLong(longValue);
      out.writeInt(intValue);
      out.writeShort(shortValue);
      out.writeChar(charValue);
      out.writeDouble(doubleValue);
      out.writeFloat(floatValue);
      out.writeBoolean(boolValue);
      out.close();

      TCByteBufferInputStream in = new TCByteBufferInputStream(out.accessBuffers());
      try {
        assertEquals(longValue, in.readLong());
        assertEquals(intValue, in.readInt());
        assertEquals(shortValue, in.readShort());
        assertEquals(charValue, in.readChar());
        assertEquals(doubleValue, in.readDouble(), 0.0);
        assertEquals(floatValue, in.readFloat(), 0.0f);
        assertEquals(boolValue, in.readBoolean());
        assertEquals(0, in.available());
      } finally {
        in.close();
      }
    }
  }

  @Test
  public void testPrimitiveStraddlingBuffers() throws IOException {
    // one value per stream; split the source bytes at every possible offset so the
    // primitive crosses buffer boundaries on every code path (bulk -> fallback included)
    for (int split = 0; split <= 8; split++) {
      byte[] longBytes = ByteBuffer.allocate(8).putLong(0x0102030405060708L).array();
      byte[] head = Arrays.copyOf(longBytes, split);
      byte[] tail = Arrays.copyOfRange(longBytes, split, 8);
      TCByteBufferInputStream in = streamOver(new TCByteBuffer[] { TCByteBufferFactory.wrap(head),
                                                                   TCByteBufferFactory.wrap(tail) });
      try {
        assertEquals(0x0102030405060708L, in.readLong());
        assertEquals(0, in.available());
      } finally {
        in.close();
      }
    }
    for (int split = 0; split <= 4; split++) {
      byte[] intBytes = ByteBuffer.allocate(4).putInt(0x11223344).array();
      TCByteBufferInputStream in = streamOver(new TCByteBuffer[] { TCByteBufferFactory.wrap(Arrays.copyOf(intBytes, split)),
                                                                   TCByteBufferFactory.wrap(Arrays.copyOfRange(intBytes, split, 4)) });
      try {
        assertEquals(0x11223344, in.readInt());
        assertEquals(0, in.available());
      } finally {
        in.close();
      }
    }
  }

  @Test
  public void testPrimitiveOneByteBuffers() throws IOException {
    // every read must fall back to the byte-at-a-time path; values repeat per group of 2/4/8
    byte[] data = new byte[32];
    new Random(42).nextBytes(data);

    TCByteBufferInputStream in = streamOver(wrapPerByte(data));
    try {
      assertEquals(ByteBuffer.wrap(data, 0, 8).getLong(), in.readLong());
      assertEquals(ByteBuffer.wrap(data, 8, 4).getInt(), in.readInt());
      assertEquals(ByteBuffer.wrap(data, 12, 2).getShort(), in.readShort());
      assertEquals(ByteBuffer.wrap(data, 14, 2).getChar(), in.readChar());
      assertEquals(ByteBuffer.wrap(data, 16, 2).getShort() & 0xFFFF, in.readUnsignedShort());
      assertEquals(data[18], in.readByte());
      assertEquals(ByteBuffer.wrap(data, 19, 8).getDouble(), in.readDouble(), 0.0);
      assertEquals(ByteBuffer.wrap(data, 27, 4).getFloat(), in.readFloat(), 0.0f);
      assertEquals(1, in.available());
    } finally {
      in.close();
    }
  }

  @Test
  public void testPrimitiveEof() throws IOException {
    for (int len = 0; len < 8; len++) {
      byte[] data = new byte[len];
      new Random(len).nextBytes(data);

      // readLong truncation
      TCByteBufferInputStream in = streamOver(data);
      try {
        in.readLong();
        fail("expected EOFException for readLong with " + len + " bytes");
      } catch (EOFException expected) {
        assertEquals("stream must be fully consumed on truncated primitive", 0, in.available());
      } finally {
        in.close();
      }

      if (len < 4) {
        // readInt truncation
        in = streamOver(data);
        try {
          in.readInt();
          fail("expected EOFException for readInt with " + len + " bytes");
        } catch (EOFException expected) {
          assertEquals(0, in.available());
        } finally {
          in.close();
        }
      }
    }
    for (int len = 0; len < 2; len++) {
      byte[] data = new byte[len];
      TCByteBufferInputStream in = streamOver(data);
      try {
        in.readChar();
        fail("expected EOFException for readChar with " + len + " bytes");
      } catch (EOFException expected) {
        assertEquals(0, in.available());
      } finally {
        in.close();
      }
      in = streamOver(data);
      try {
        in.readShort();
        fail("expected EOFException for readShort with " + len + " bytes");
      } catch (EOFException expected) {
        assertEquals(0, in.available());
      } finally {
        in.close();
      }
      in = streamOver(data);
      try {
        in.readUnsignedShort();
        fail("expected EOFException for readUnsignedShort with " + len + " bytes");
      } catch (EOFException expected) {
        assertEquals(0, in.available());
      } finally {
        in.close();
      }
    }
  }

  @Test
  public void testPrimitiveEofAcrossBuffers() throws IOException {
    // truncated primitive spanning multiple buffers: EOF thrown after all bytes consumed
    byte[] data = ByteBuffer.allocate(8).putLong(Long.MAX_VALUE).array();
    for (int split = 1; split < 7; split++) {
      TCByteBufferInputStream in = streamOver(new TCByteBuffer[] { TCByteBufferFactory.wrap(Arrays.copyOf(data, split)),
                                                                   TCByteBufferFactory.wrap(Arrays.copyOfRange(data, split, 7)) });
      try {
        in.readLong();
        fail("expected EOFException with 7 total bytes across buffers");
      } catch (EOFException expected) {
        assertEquals(0, in.available());
      } finally {
        in.close();
      }
    }
  }

  @Test
  public void testFastPathThenFallbackMix() throws IOException {
    // exact-fit bulk reads landing on buffer boundaries, then a 2-byte read
    // straddling a boundary (fallback) followed by fast-path tail reads
    byte[] first = ByteBuffer.allocate(8).putLong(0xDEADBEEFCAFEBABEL).array();
    byte[] second = ByteBuffer.allocate(4).putInt(0x01234567).array();
    byte[] third = new byte[] { 0x33, 0x44, 0x55 };
    byte[] fourth = new byte[] { 0x66, 0x77, (byte) 0x88 };

    TCByteBufferInputStream in = streamOver(new TCByteBuffer[] { TCByteBufferFactory.wrap(first),
                                                                 TCByteBufferFactory.wrap(second),
                                                                 TCByteBufferFactory.wrap(third),
                                                                 TCByteBufferFactory.wrap(fourth) });
    try {
      assertEquals(0xDEADBEEFCAFEBABEL, in.readLong());          // fast path, exact fit
      assertEquals(0x01234567, in.readInt());                    // fast path, exact fit
      assertEquals(0x3344, in.readShort());                     // fast path (1 byte left after)
      assertEquals(0x5566, in.readUnsignedShort());              // fallback (straddles boundary)
      assertEquals(0x77, in.readUnsignedByte());
      assertEquals((byte) 0x88, in.readByte());
      assertEquals(0, in.available());
    } finally {
      in.close();
    }
  }

  @Test
  public void testDuplicateWithPrimitives() throws IOException {
    byte[] data = new byte[32];
    this.random.nextBytes(data);

    TCByteBufferInputStream bbis = streamOver(data);
    TCByteBufferInput dupe;
    try {
      bbis.readInt();            // consume 4 from a bulk fast path
      bbis.readShort();          // consume 2 more
      dupe = bbis.duplicate();
      assertEquals(bbis.available(), dupe.available());
      assertEquals(bbis.readLong(), dupe.readLong());
      assertEquals(bbis.readInt(), dupe.readInt());
      assertEquals(bbis.readChar(), dupe.readChar());
      assertEquals(bbis.available(), dupe.available());
      while (bbis.available() > 0) {
        assertEquals(bbis.read(), dupe.read());
      }
      assertEquals(0, dupe.available());
    } finally {
      bbis.close();
    }
  }

}
