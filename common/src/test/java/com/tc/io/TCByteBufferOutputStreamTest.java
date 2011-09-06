/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.io;

import com.tc.bytes.TCByteBuffer;
import com.tc.bytes.TCByteBufferFactory;
import com.tc.io.TCByteBufferOutputStream.Mark;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class TCByteBufferOutputStreamTest extends TCTestCase {

  final Random random = new Random();

  public void testMultipleToArray() {
    for (int i = 0; i < 250; i++) {

      TCByteBufferOutputStream bbos = new TCByteBufferOutputStream(random.nextInt(100) + 1, false);
      try {
        int bytesToWrite = random.nextInt(75) + 50;
        for (int n = 0; n < bytesToWrite; n++) {
          bbos.write(42);
        }
        assertEquals(bytesToWrite, bbos.getBytesWritten());
        TCByteBuffer[] data = bbos.toArray();
        assertEquals(bytesToWrite, length(data));
        for (int j = 0; j < 10; j++) {
          bbos.toArray();
        }
      } finally {
        bbos.close();
      }
    }
  }

  public void testMark() {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    TCByteBufferOutputStream output = new TCByteBufferOutputStream(32, false);
    try {
      for (int i = 0; i < 30; i++) {
        output.write(1);
        baos.write(1);
      }
      Mark mark1 = output.mark();
      for (int i = 0; i < 4; i++) {
        output.write(0);
        baos.write(i + 1);
      }
      for (int i = 0; i < 30; i++) {
        output.write(1);
        baos.write(1);
      }
      Mark mark2 = output.mark();
      output.write(0);
      int b = random.nextInt();
      baos.write(b);
      int written = output.getBytesWritten();
      mark1.write(new byte[] { 1, 2, 3, 4 }); // should cross the 1st and 2nd buffers in the stream
      assertEquals(written, output.getBytesWritten());
      mark2.write(b); // should write to the 3rd buffer exclusively, but start on the 2nd
      assertEquals(written, output.getBytesWritten());
      compareData(baos.toByteArray(), output.toArray());
      // output stream should be closed now due to toArray() above
      try {
        mark1.write(1);
        fail();
      } catch (IllegalStateException ise) {
        // expected
      }
      try {
        mark1.write(new byte[2]);
        fail();
      } catch (IllegalStateException ise) {
        // expected
      }
      try {
        output.mark();
        fail();
      } catch (IllegalStateException ise) {
        // expected
      }
    } finally {
      output.close();
    }

  }

  public void testInvalidWriteThroughMark() {
    TCByteBufferOutputStream output = new TCByteBufferOutputStream();
    try {
      output.write(new byte[30]);
      Mark mark1 = output.mark();
      try {
        mark1.write(0);
        fail();
      } catch (IllegalArgumentException iae) {
        // expected
      }
      try {
        mark1.write(new byte[1]);
        fail();
      } catch (IllegalArgumentException iae) {
        // expected
      }
      output.write(1);
      int written = output.getBytesWritten();
      mark1.write(1);
      assertEquals(written, output.getBytesWritten());
      mark1.write(new byte[0]);
      assertEquals(written, output.getBytesWritten());
      mark1.write(new byte[1]);
      assertEquals(written, output.getBytesWritten());
      try {
        mark1.write(new byte[2]);
        fail();
      } catch (IllegalArgumentException iae) {
        // expected
      }
    } finally {
      output.close();
    }

  }

  public void testRandomMark() throws IOException {
    for (int i = 0; i < 500; i++) {
      doRandomMark();
    }
  }

  private void doRandomMark() throws IOException {
    int initial = random.nextInt(10) + 1;
    int max = initial + random.nextInt(1024) + 1;
    TCByteBufferOutputStream output = new TCByteBufferOutputStream(initial, max, false);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    List data = new ArrayList();
    List marks = new ArrayList();

    try {
      for (int i = 0; i < 1000; i++) {
        marks.add(output.mark());

        if (random.nextBoolean()) {
          byte[] b = new byte[random.nextInt(10)];
          random.nextBytes(b);
          baos.write(b);
          output.write(new byte[b.length]);
          data.add(b);
        } else {
          int b = random.nextInt();
          output.write(0);
          baos.write(b);
          data.add(new byte[] { (byte) b });
        }

        if (random.nextInt(10) > 6) {
          byte[] b = new byte[random.nextInt(5) + 1];
          random.nextBytes(b);
          baos.write(b);
          output.write(b);
        }
      }
      for (int i = 0, n = marks.size(); i < n; i++) {
        Mark mark = (Mark) marks.get(i);
        byte[] b = (byte[]) data.get(i);
        if (b.length == 1) {
          mark.write(b[0]);
        } else {
          mark.write(b);
        }
      }
      compareData(baos.toByteArray(), output.toArray());
    } finally {
      output.close();
    }
  }

  public void testArrayWriteZeroLength() {
    TCByteBufferOutputStream output = new TCByteBufferOutputStream();

    try {
      TCByteBuffer[] bufs = new TCByteBuffer[5];
      TCByteBuffer bufZeroLen = TCByteBufferFactory.getInstance(false, 0);
      bufs[0] = bufZeroLen;
      bufs[1] = TCByteBufferFactory.getInstance(false, 10);
      bufs[2] = bufZeroLen;
      bufs[3] = bufZeroLen;
      bufs[4] = bufZeroLen;
      long buflength = length(bufs);
      output.write(bufs);
      assertEquals(buflength, output.getBytesWritten());
      TCByteBuffer[] bufsOut = output.toArray();
      assertTrue(bufsOut.length < bufs.length); // 'coz its consolidated
    } finally {
      output.close();
    }
  }

  public void testBytesWritten() {
    TCByteBufferOutputStream bbos = new TCByteBufferOutputStream();
    try {
      assertEquals(0, bbos.getBytesWritten());
      bbos.write(42);
      assertEquals(1, bbos.getBytesWritten());
      bbos.write(new byte[10]);
      assertEquals(11, bbos.getBytesWritten());
      bbos.write(new byte[10], 1, 2);
      assertEquals(13, bbos.getBytesWritten());
      // an exception shouldn't mess up the bytes written count
      try {
        bbos.write(new byte[10], 10, 1);
        fail();
      } catch (IndexOutOfBoundsException ioobe) {
        // expected
      }
      assertEquals(13, bbos.getBytesWritten());
      bbos.write(new byte[0]);
      assertEquals(13, bbos.getBytesWritten());
    } finally {
      bbos.close();
    }
  }

  public void testBasic() {
    int blockSize = 4096;
    TCByteBufferOutputStream bbos = new TCByteBufferOutputStream(blockSize, false);
    int num = 10;

    byte write = 0;
    try {
      for (int i = 0; i < blockSize * num; i++) {
        bbos.write(write++);
      }
      TCByteBuffer[] data = bbos.toArray();
      assertEquals(data.length, num);
      for (TCByteBuffer element : data) {
        assertNotNull(element);
        assertEquals(0, element.position());
        assertEquals(blockSize, element.limit());
      }
      byte expect = 0;
      for (TCByteBuffer buf : data) {
        while (buf.hasRemaining()) {
          byte read = buf.get();
          assertEquals(expect++, read);
        }
      }
    } finally {
      bbos.close();
    }

  }

  public void testRandom() throws IOException {
    for (int i = 0; i < 100; i++) {
      doRandom();
    }
  }

  public void testBasicConsolidation() {
    TCByteBufferOutputStream os = new TCByteBufferOutputStream(32, 4096, false);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();

    try {
      for (int i = 0; i < 8192; i++) {
        byte b = (byte) random.nextInt();
        baos.write(b);
        os.write(b);
      }
      TCByteBuffer[] bufs = os.toArray();
      /*
       * Consolidation with the new TCByteBuffer buf 0 : 32 + 64 + 128 + 256 + 512 + 1024 + 2048 = 4064 ( < 4096 ) buf 1
       * : 4096 buf 2 : 32
       */
      assertEquals(3, bufs.length);
      assertEquals(4064, bufs[0].limit());
      assertEquals(4096, bufs[1].limit());
      assertEquals(32, bufs[2].limit());
      compareData(baos.toByteArray(), bufs);
    } finally {
      os.close();
    }
  }

  public void doRandom() throws IOException {
    // this guy will hold the control/compare data
    ByteArrayOutputStream baos = new ByteArrayOutputStream();

    final int bufSize = random.nextInt(50) + 1;

    TCByteBufferOutputStream os = new TCByteBufferOutputStream(bufSize, false);

    try {
      for (int i = 0; i < bufSize * 3; i++) {
        int streamLen = os.getBytesWritten();
        int whichWrite = random.nextInt(4);
        switch (whichWrite) {
          case 0: { // write(int)
            byte b = (byte) random.nextInt();
            baos.write(b);
            os.write(b);
            assertEquals(streamLen + 1, os.getBytesWritten());
            break;
          }
          case 1: { // write(byte[])
            byte b[] = new byte[random.nextInt(bufSize * 2)];
            random.nextBytes(b);
            baos.write(b);
            os.write(b);
            break;
          }
          case 2: { // write(byte[], int, int)
            byte b[] = new byte[random.nextInt(bufSize * 2)];
            random.nextBytes(b);
            int off = b.length == 0 ? 0 : random.nextInt(b.length);
            int len = b.length == 0 ? 0 : random.nextInt(b.length - off);
            baos.write(b, off, len);
            os.write(b, off, len);
            assertEquals(streamLen + len, os.getBytesWritten());
            break;
          }
          case 3: { // write(TCByteBuffer[])
            int num = random.nextInt(5);
            TCByteBuffer[] b = new TCByteBuffer[num];
            for (int n = 0; n < b.length; n++) {
              TCByteBuffer buf = TCByteBufferFactory.getInstance(false, random.nextInt(bufSize * 2));
              byte[] bites = new byte[buf.limit()];
              random.nextBytes(bites);
              buf.put(bites);
              buf.position(0);
              b[n] = buf;
              baos.write(bites);
            }
            os.write(b);
            assertEquals(streamLen + length(b), os.getBytesWritten());
            break;
          }
          default: {
            fail("unknown write: " + whichWrite);
          }
        }
      }

      TCByteBuffer[] bufsOut = os.toArray();
      assertNoZeroLength(bufsOut);

      compareData(baos.toByteArray(), os.toArray());
    } finally {
      os.close();
    }
  }

  private void assertNoZeroLength(TCByteBuffer[] bufs) {
    for (int i = 0; i < bufs.length; i++) {
      assertTrue("Buffer " + i + " has zero length", bufs[i].limit() > 0);
    }
  }

  private void compareData(byte[] compare, TCByteBuffer[] test) {
    if (test.length == 0) {
      assertEquals(0, compare.length);
      return;
    }

    int index = 0;
    for (byte b : compare) {
      while (!test[index].hasRemaining()) {
        index++;
      }
      byte b2 = test[index].get();
      assertEquals(b, b2);
    }

    assertFalse(test[index].hasRemaining());
  }

  private int length(TCByteBuffer[] b) {
    int rv = 0;
    for (TCByteBuffer element : b) {
      rv += element.limit();
    }
    return rv;
  }

  public void testWriteWierdString() throws IOException {
    TCByteBufferOutputStream out = new TCByteBufferOutputStream();
    String wierd = "Weird" + (char) 0xd900;
    out.writeString(wierd);
    out.close();

    TCByteBufferInputStream in = new TCByteBufferInputStream(out.toArray());
    try {
      String read = in.readString();
      Assert.assertTrue(Arrays.equals(wierd.toCharArray(), read.toCharArray()));
    } finally {
      in.close();
    }
  }

  public void testWriteString() throws IOException {
    TCByteBufferOutputStream out = new TCByteBufferOutputStream();
    out.writeString(null);
    out.writeString("XXX");
    String longString = makeString(0xFFFF * 2);
    out.writeString(longString);
    out.close();

    TCByteBufferInputStream in = new TCByteBufferInputStream(out.toArray());
    try {
      Assert.assertNull(in.readString());
      Assert.assertTrue(Arrays.equals(in.readString().toCharArray(), "XXX".toCharArray()));
      Assert.assertTrue(Arrays.equals(longString.toCharArray(), in.readString().toCharArray()));
    } finally {
      in.close();
    }
  }

  private String makeString(int len) {
    StringBuilder buf = new StringBuilder(len);
    for (int i = 0; i < len; i++) {
      buf.append('x');
    }
    String rv = buf.toString();
    Assert.assertEquals(rv.length(), len);
    return rv;
  }

  public void testWithArrayWrite() {
    int blockSize = 4096;
    TCByteBufferOutputStream bbos = new TCByteBufferOutputStream(blockSize, false);
    int num = 10;

    byte write = 0;
    try {
      for (int i = 0; i < blockSize * num; i++) {
        bbos.write(write++);
      }
      TCByteBuffer[] b = new TCByteBuffer[1];
      byte[] s = "Hello Steve".getBytes();
      b[0] = TCByteBufferFactory.getInstance(false, s.length);
      b[0].put(s);
      b[0].flip();
      bbos.write(b);
      TCByteBuffer[] data = bbos.toArray();
      assertEquals(num + 1, data.length);
      for (int i = 0; i < data.length - 1; i++) {
        assertNotNull(data[i]);
        assertEquals(0, data[i].position());
        assertEquals(blockSize, data[i].limit());
      }
      int last = data.length - 1;
      assertNotNull(data[last]);
      assertEquals(0, data[last].position());
      byte expect = 0;
      for (int i = 0; i < data.length - 1; i++) {
        TCByteBuffer buf = data[i];
        while (buf.hasRemaining()) {
          byte read = buf.get();
          assertEquals(expect++, read);
        }
      }
      byte[] s2 = new byte[s.length];
      data[last].get(s2);
      assertTrue(Arrays.equals(s, s2));
      assertFalse(data[last].hasRemaining());
    } finally {
      bbos.close();
    }
  }

  public void testExceptions() {
    TCByteBufferOutputStream bbos = null;
    try {
      bbos = new TCByteBufferOutputStream(0, false);
      fail();
    } catch (IllegalArgumentException iae) {
      // expected
    } finally {
      if (bbos != null) {
        bbos.close();
      }
    }

    try {
      bbos = new TCByteBufferOutputStream(-1, false);
      fail();
    } catch (IllegalArgumentException iae) {
      // expected
    } finally {
      if (bbos != null) {
        bbos.close();
      }
    }

    bbos = new TCByteBufferOutputStream();
    try {
      try {
        bbos.write((byte[]) null);
        fail();
      } catch (NullPointerException npe) {
        // expected
      }
      try {
        bbos.write((TCByteBuffer[]) null);
        fail();
      } catch (NullPointerException npe) {
        // expected
      }
      try {
        bbos.write(null, 0, 0);
        fail();
      } catch (NullPointerException npe) {
        // expected
      }
      try {
        bbos.write(new byte[10], -10, 0);
        fail();
      } catch (IndexOutOfBoundsException ioobe) {
        // expected
      }
      try {
        bbos.write(new byte[10], 1, -10);
        fail();
      } catch (IndexOutOfBoundsException ioobe) {
        // expected
      }
      try {
        bbos.write(new byte[10], 1, 10);
        fail();
      } catch (IndexOutOfBoundsException ioobe) {
        // expected
      }
    } finally {
      bbos.close();
    }
  }

  public void testEdgeCase1() {
    testEdgeCase(10, 5, 1004);
  }

  private void testEdgeCase(int bufLength, int byteArrLength, int iterationCount) {

    TCByteBufferOutputStream bbos = new TCByteBufferOutputStream(bufLength, false);
    try {
      byte data[] = new byte[byteArrLength];
      int dataWriten = 0;
      for (int i = 0; i < iterationCount; i++) {
        bbos.write(data);
        dataWriten += data.length;
      }
      TCByteBuffer[] bufs = bbos.toArray();
      for (int i = 0; i < bufs.length - 1; i++) {
        assertEquals(bufs[i].capacity(), bufs[i].limit());
        dataWriten -= bufs[i].limit();
      }
      assertEquals(dataWriten, bufs[bufs.length - 1].limit());
    } finally {
      bbos.close();
    }
  }

  public void testEdgeCase2() {
    testEdgeCase(3, 37, 10);
  }

  public void testEdgeCase3() {
    testEdgeCase(1, 1, 5000);
  }

  public void testEdgeCase4() {
    TCByteBufferOutputStream bbos = new TCByteBufferOutputStream(27, false);
    try {
      byte data[] = new byte[370];
      int written = 0;
      for (int i = 0; i < 50; i++) {
        Arrays.fill(data, (byte) 0);
        Arrays.fill(data, i, i + 50, (byte) 42);
        bbos.write(data, i, 50);
        written += 50;
      }
      TCByteBuffer[] bufs = bbos.toArray();
      for (int i = 0; i < bufs.length - 1; i++) {
        assertEquals(bufs[i].capacity(), bufs[i].limit());
        written -= bufs[i].limit();
      }
      assertEquals(written, bufs[bufs.length - 1].limit());
      for (TCByteBuffer buf : bufs) {
        while (buf.hasRemaining()) {
          assertEquals(42, buf.get());
        }
      }
    } finally {
      bbos.close();
    }
  }

  public void testEmpty() {
    TCByteBufferOutputStream bbos = new TCByteBufferOutputStream();
    bbos.close();
    TCByteBuffer[] data = bbos.toArray();
    assertEquals(0, data.length);
  }

  public void testClose() {
    TCByteBufferOutputStream bbos = new TCByteBufferOutputStream();

    try {
      bbos.write(new byte[1234]);
      for (int i = 0; i < 10; i++) {
        bbos.close();
      }
      try {
        bbos.write(1);
        fail();
      } catch (IllegalStateException ise) {
        // expected
      }
      try {
        bbos.write(new byte[10]);
        fail();
      } catch (IllegalStateException ise) {
        // expected
      }
      try {
        bbos.write(new byte[10], 2, 5);
        fail();
      } catch (IllegalStateException ise) {
        // expected
      }
      try {
        bbos.write(new TCByteBuffer[] {});
        fail();
      } catch (IllegalStateException ise) {
        // expected
      }
    } finally {
      bbos.close();
    }

  }
}
