/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.io;

import com.tc.bytes.TCByteBuffer;
import com.tc.bytes.TCByteBufferFactory;
import com.tc.io.TCByteBufferInput.Mark;
import com.tc.test.TCTestCase;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class TCByteBufferInputStreamTest extends TCTestCase {

  private final Random random = new SecureRandom();

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
      inputStream = new TCByteBufferInputStream(new TCByteBuffer[] { TCByteBufferFactory.getInstance(false, 5), null });
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
        bbis = new TCByteBufferInputStream(TCByteBufferFactory.getInstance(false, 10));
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
        bbis = new TCByteBufferInputStream(TCByteBufferFactory.getInstance(false, 10));
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
        bbis = new TCByteBufferInputStream(TCByteBufferFactory.getInstance(false, 10));
        bbis.read(new byte[10], 10, 1);
        fail();
      } catch (IndexOutOfBoundsException ioobe) {
        // expected
      } finally {
        bbis.close();
      }
    }

    try {
      TCByteBufferInputStream bbis = new TCByteBufferInputStream(TCByteBufferFactory.getInstance(false, 10));
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
        bbis = new TCByteBufferInputStream(TCByteBufferFactory.getInstance(false, 10));
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

    TCByteBufferInputStream bbis = new TCByteBufferInputStream(TCByteBufferFactory.getInstance(false, 10));
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

  public void testToArray() {
    for (int i = 0; i < 250; i++) {
      TCByteBuffer[] data = getRandomDataNonZeroLength();
      TCByteBufferInputStream bbis = new TCByteBufferInputStream(data);

      try {
        int read = this.random.nextInt(bbis.available());
        for (int r = 0; r < read; r++) {
          bbis.read();
        }
        TCByteBufferInputStream compare = new TCByteBufferInputStream(bbis.toArray());
        try {
          while (compare.available() > 0) {
            int orig = bbis.read();
            int comp = compare.read();
            assertEquals(orig, comp);
          }
          assertEquals(0, compare.available());
          assertEquals(0, bbis.available());
        } finally {
          compare.close();
        }
      } finally {
        bbis.close();
      }
    }
  }

  public void testLimit() {
    for (int i = 0; i < 250; i++) {
      TCByteBuffer[] data = getRandomDataNonZeroLength();
      TCByteBufferInputStream bbis = new TCByteBufferInputStream(data);

      try {
        int read = this.random.nextInt(bbis.available());
        for (int r = 0; r < read; r++) {
          bbis.read();
        }
        int which = this.random.nextInt(3);
        switch (which) {
          case 0: {
            bbis.limit(0);
            assertEquals(0, bbis.available());
            break;
          }
          case 1: {
            int before = bbis.available();
            bbis.limit(bbis.available());
            assertEquals(before, bbis.available());
            break;
          }
          case 2: {
            bbis.limit(this.random.nextInt(bbis.available()));
            break;
          }
          default: {
            throw new RuntimeException("" + which);
          }
        }
        TCByteBufferInputStream compare = new TCByteBufferInputStream(bbis.toArray());
        try {
          while (compare.available() > 0) {
            int orig = bbis.read();
            int comp = compare.read();
            assertEquals(orig, comp);
          }
          assertEquals(0, compare.available());
          assertEquals(0, bbis.available());
        } finally {
          compare.close();
        }
      } finally {
        bbis.close();
      }
    }
  }

  public void testDuplicateAndLimitZeroLen() {
    TCByteBufferInputStream bbis = new TCByteBufferInputStream(new TCByteBuffer[] {});

    try {
      assertEquals(0, bbis.available());
      assertEquals(0, bbis.duplicateAndLimit(0).available());
    } finally {
      bbis.close();
    }
  }

  public void testDuplicateAndLimit() {
    for (int i = 0; i < 50; i++) {
      TCByteBuffer[] data = getRandomDataNonZeroLength();
      TCByteBufferInputStream bbis = new TCByteBufferInputStream(data);

      try {
        int length = bbis.available();
        assertTrue(length > 0);
        int start = this.random.nextInt(length);
        bbis.skip(start);
        int limit = this.random.nextInt(bbis.available());
        TCByteBufferInput dupe = bbis.duplicateAndLimit(limit);
        for (int n = 0; n < limit; n++) {
          int dupeByte = dupe.read();
          int origByte = bbis.read();

          assertTrue(dupeByte != -1);
          assertTrue(origByte != -1);

          assertEquals(origByte, dupeByte);
        }
        assertEquals(0, dupe.available());
      } finally {
        bbis.close();
      }
    }
  }

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

  public void testOffsetReadArray() {
    for (int i = 0; i < 25; i++) {
      testOffsetReadArray(getRandomData());
    }
  }

  private void testOffsetReadArray(TCByteBuffer[] data) {
    final int numBufs = data.length == 0 ? 0 : data.length - 1;

    reportLengths(data);
    final long totalLength = length(data);
    TCByteBufferInputStream bbis = new TCByteBufferInputStream(data);
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

  public void testReadBasics() {
    for (int i = 0; i < 250; i++) {
      testReadBasics(getRandomData());
    }
  }

  public void testBasic() {
    for (int i = 0; i < 250; i++) {
      TCByteBuffer[] data = getRandomDataNonZeroLength();
      final byte b;
      TCByteBufferInputStream bbis = new TCByteBufferInputStream(data);

      try {
        assertTrue(bbis.available() > 0);
        b = data[0].get(0);
        int read = bbis.read();
        assertEquals(b, (byte) read);
      } finally {
        bbis.close();
      }
      byte[] readArray = new byte[1];
      bbis = new TCByteBufferInputStream(data);
      try {
        bbis.read(readArray);
        assertEquals(b, readArray[0]);
      } finally {
        bbis.close();
      }
      bbis = new TCByteBufferInputStream(data);
      try {
        bbis.read(readArray, 0, 1);
        assertEquals(b, readArray[0]);
      } finally {
        bbis.close();
      }
      bbis = new TCByteBufferInputStream(data);
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

  public void testMarkReset() throws IOException {
    TCByteBuffer[] data = createBuffersWithRandomData(4, 10);
    TCByteBufferInputStream bbis = new TCByteBufferInputStream(data);

    try {
      bbis.readInt();
      bbis.readInt();
      bbis.readInt(); // should be 2 bytes into 2nd buffer by now
      try {
        bbis.tcReset(null);
        fail();
      } catch (IllegalArgumentException ise) {
        // expected
      }
      int avail = bbis.available();
      Mark m = bbis.mark();
      int i1 = bbis.readInt();
      int i2 = bbis.readInt();
      int i3 = bbis.readInt(); // should be 4 bytes into 3rd buffer now
      bbis.tcReset(m);
      assertEquals(avail, bbis.available());
      assertEquals(i1, bbis.readInt());
      assertEquals(i2, bbis.readInt());
      assertEquals(i3, bbis.readInt());
      try {
        bbis.tcReset(null);
        fail();
      } catch (IllegalArgumentException ise) {
        // expected
      }
    } finally {
      bbis.close();
    }
  }

  public void testMarkReset2() throws IOException {
    // This one stays on the same buffer between mark and reset
    TCByteBuffer[] data = createBuffersWithRandomData(1, 10);
    TCByteBufferInputStream bbis = new TCByteBufferInputStream(data);

    try {
      bbis.readInt();
      int avail = bbis.available();
      Mark m = bbis.mark();
      int i1 = bbis.readInt();
      bbis.tcReset(m);
      assertEquals(avail, bbis.available());
      assertEquals(i1, bbis.readInt());
    } finally {
      bbis.close();
    }
  }

  public void testMultipleMarks() throws IOException {
    TCByteBuffer[] data = createBuffersWithRandomData(this.random.nextInt(50), this.random.nextInt(512));
    TCByteBufferInputStream bbis = new TCByteBufferInputStream(data);

    try {
      List marks = new ArrayList();
      List bytes = new ArrayList();
      while (bbis.available() > 0) {
        marks.add(bbis.mark());
        byte[] randBytes = new byte[this.random.nextInt(bbis.available() + 1)];
        bbis.readFully(randBytes);
        bytes.add(randBytes);
        marks.add(bbis.mark());
      }
      for (int i = 0, j = 0; i < marks.size(); i++) {
        Mark start = (Mark) marks.get(i++);
        Mark end = (Mark) marks.get(i);
        System.out.println("Start Mark : " + start);
        System.out.println("End Mark   : " + end);
        byte[] expectedBytes = (byte[]) bytes.get(j++);
        TCByteBuffer[] got = bbis.toArray(start, end);
        byte[] gotBytes = drain(got);
        print("Expected : ", expectedBytes);
        print("Got      : ", gotBytes);
        assertEquals(expectedBytes, gotBytes);
      }
    } finally {
      bbis.close();
    }
  }

  private void print(String msg, byte[] b) {
    System.out.print(msg);
    for (byte element : b) {
      System.out.print(element + " ");
    }
    System.out.println();
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

  private void testReadBasics(TCByteBuffer[] data) {
    final int numBufs = data.length == 0 ? 0 : data.length - 1;

    reportLengths(data);
    final long len = length(data);
    TCByteBufferInputStream bbis = new TCByteBufferInputStream(data);
    try {
      assertEquals(len, bbis.available());
      assertEquals(len, bbis.getTotalLength());
      int index = 0;
      for (int i = 0; i < len; i++) {
        for (; !data[index].hasRemaining() && index < numBufs; index++) {
          //
        }

        assertEquals(len - i, bbis.available());
        int fromStream = bbis.read();
        assertFalse(fromStream < 0);

        byte original = data[index].get();

        assertEquals(original, (byte) fromStream);
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
      assertEquals(0, bbis.available());
      assertEquals(-1, bbis.read());
      assertEquals(-1, bbis.read(new byte[10]));
      assertEquals(-1, bbis.read(new byte[10], 0, 3));
    } finally {
      bbis.close();
    }
  }

  private void reportLengths(TCByteBuffer[] data) {
    // comment this if tests are failing
    if (true) { return; }

    System.err.print(data.length + " buffers with lengths: ");
    for (TCByteBuffer element : data) {
      System.err.print(element.limit() + " ");
    }
    System.err.println();
  }

  public void testTrailingZeroLength() {
    TCByteBuffer[] data = getRandomData();
    TCByteBuffer zeroLen = TCByteBufferFactory.getInstance(false, 0);

    for (int i = 0; i < Math.min(10, data.length); i++) {
      data[data.length - i - 1] = zeroLen;
      rewindBuffers(data);
      testReadArrayBasics(data);

      rewindBuffers(data);
      testOffsetReadArray(data);
    }
  }

  public void testRandomZeroLength() {
    TCByteBuffer[] data = getRandomDataNonZeroLength();

    TCByteBuffer zeroLen = TCByteBufferFactory.getInstance(false, 0);

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

    reportLengths(data);
    final long len = length(data);
    TCByteBufferInputStream bbis = new TCByteBufferInputStream(data);
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
        try {
          is.skip(Integer.MAX_VALUE + 1L);
          fail();
        } catch (IllegalArgumentException iae) {
          // expected
        }
      } finally {
        is.close();
      }
    }
  }

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
      TCByteBuffer[] toArray = bbis.toArray();
      assertEquals(0, toArray.length);
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
      rv[i] = TCByteBufferFactory.getInstance(false, size);
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
      rv[i] = TCByteBufferFactory.getInstance(false, maxSize > 0 ? this.random.nextInt(maxSize) : 0);
      this.random.nextBytes(rv[i].array());
    }

    return rv;
  }

}
