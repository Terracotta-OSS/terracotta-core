/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

import com.tc.bytes.TCByteBuffer;
import com.tc.bytes.TCByteBufferFactory;
import com.tc.bytes.TCReference;
import com.tc.util.Assert;
import java.util.Iterator;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Ignore;

public class TCByteBufferOutputStreamTest {

  final Random random = new Random();
  
  @Before
  public void setup() {

  }

  @Test
  public void testArrayWriteZeroLength() {
    try (TCByteBufferOutputStream output = new TCByteBufferOutputStream()) {
      TCByteBuffer[] bufs = new TCByteBuffer[5];
      TCByteBuffer bufZeroLen = TCByteBufferFactory.getInstance(0);
      bufs[0] = bufZeroLen;
      bufs[1] = TCByteBufferFactory.getInstance(10);
      bufs[2] = bufZeroLen;
      bufs[3] = bufZeroLen;
      bufs[4] = bufZeroLen;
      long buflength = length(bufs);
      output.write(bufs);
      assertEquals(buflength, output.getBytesWritten());
    }
  }

  @Test
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

  @Test
  public void testBasic() {
    int blockSize = 4096;
    TCByteBufferOutputStream bbos = new TCByteBufferOutputStream(blockSize);
    int num = 10;

    byte write = 0;
    try {
      for (int i = 0; i < blockSize * num; i++) {
        bbos.write(write++);
      }
      TCReference data = bbos.accessBuffers();
      int count = 0;
      for (TCByteBuffer element : data) {
        assertNotNull(element);
        assertEquals(0, element.position());
        assertEquals(blockSize, element.limit());
        count++;
      }
      assertEquals(count, num);
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

  @Test
  public void testRandom() throws IOException {
    for (int i = 0; i < 100; i++) {
      doRandom();
    }
  }

  @Test @Ignore
  public void testBasicConsolidation() {
    TCByteBufferOutputStream os = new TCByteBufferOutputStream(32, 4096);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();

    try {
      for (int i = 0; i < 8192; i++) {
        byte b = (byte) random.nextInt();
        baos.write(b);
        os.write(b);
      }
      TCReference bufs = os.accessBuffers();
      /*
       * Consolidation with the new TCByteBuffer buf 0 : 32 + 64 + 128 + 256 + 512 + 1024 + 2048 = 4064 ( < 4096 ) buf 1
       * : 4096 buf 2 : 32
       */
      int count = 0;
      int[] eq = {4064, 4096,32};
      for (TCByteBuffer target : bufs) {
        assertEquals(eq[count], target.limit());
      }
      compareData(baos.toByteArray(), bufs);
    } finally {
      os.close();
    }
  }

  public void doRandom() throws IOException {
    // this guy will hold the control/compare data
    ByteArrayOutputStream baos = new ByteArrayOutputStream();

    final int bufSize = random.nextInt(50) + 1;

    try (TCByteBufferOutputStream os = new TCByteBufferOutputStream(bufSize)) {
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
              TCByteBuffer buf = TCByteBufferFactory.getInstance(random.nextInt(bufSize * 2));
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

      try (TCReference ref = os.accessBuffers()) {
        assertNoZeroLength(ref);

        compareData(baos.toByteArray(), ref);
      }
    }
  }

  private void assertNoZeroLength(TCReference bufs) {
    int count = 0;
    for (TCByteBuffer buffer : bufs) {
      assertTrue("Buffer " + count++ + " has zero length", buffer.limit() > 0);
    }
  }

  private void compareData(byte[] compare, TCReference test) {
    Iterator<TCByteBuffer> curs = test.duplicate().iterator();
    if (!curs.hasNext()) {
      assertEquals(0, compare.length);
      return;
    }

    TCByteBuffer current = curs.next();
    for (byte b : compare) {
      while (!current.hasRemaining()) {
        current = curs.next();
      }
      byte b2 = current.get();
      assertEquals(b, b2);
    }

    assertFalse(current.hasRemaining());
    assertFalse(curs.hasNext());
  }

  private int length(TCByteBuffer[] b) {
    int rv = 0;
    for (TCByteBuffer element : b) {
      rv += element.limit();
    }
    return rv;
  }

  @Test
  public void testWriteWierdString() throws IOException {
    TCByteBufferOutputStream out = new TCByteBufferOutputStream();
    String wierd = "Weird" + (char) 0xd900;
    out.writeString(wierd);
    out.close();

    TCByteBufferInputStream in = new TCByteBufferInputStream(out.accessBuffers());
    try {
      String read = in.readString();
      Assert.assertTrue(Arrays.equals(wierd.toCharArray(), read.toCharArray()));
    } finally {
      in.close();
    }
  }

  @Test
  public void testWriteString() throws IOException {
    TCByteBufferOutputStream out = new TCByteBufferOutputStream();
    out.writeString(null);
    out.writeString("XXX");
    String longString = makeString(0xFFFF * 2);
    out.writeString(longString);
    out.close();

    TCByteBufferInputStream in = new TCByteBufferInputStream(out.accessBuffers());
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

  @Test
  public void testWithArrayWrite() {
    int blockSize = 4096;
    TCByteBufferOutputStream bbos = new TCByteBufferOutputStream(blockSize);
    int num = 10;

    byte write = 0;
    try {
      for (int i = 0; i < blockSize * num; i++) {
        bbos.write(write++);
      }
      TCByteBuffer[] b = new TCByteBuffer[1];
      byte[] s = "Hello Steve".getBytes();
      b[0] = TCByteBufferFactory.getInstance(s.length);
      b[0].put(s);
      b[0].flip();
      bbos.write(b);
      TCReference data = bbos.accessBuffers();

      Iterator<TCByteBuffer> curs = data.iterator();
      while (curs.hasNext()) {
        TCByteBuffer current = curs.next();
        if (curs.hasNext()) {
          assertNotNull(current);
          assertEquals(0, current.position());
          assertEquals(blockSize, current.limit());
        } else {
          assertNotNull(current);
          assertEquals(0, current.position());
        }
      }

      byte expect = 0;
      curs = data.iterator();
      while (curs.hasNext()) {
        TCByteBuffer current = curs.next();
        if (curs.hasNext()) {
          while (current.hasRemaining()) {
            byte read = current.get();
            assertEquals(expect++, read);
          }
        } else {
          byte[] s2 = new byte[s.length];
          current.get(s2);
          assertTrue(Arrays.equals(s, s2));
          assertFalse(current.hasRemaining());
        }
      }

    } finally {
      bbos.close();
    }
  }

  @Test
  public void testExceptions() {
    TCByteBufferOutputStream bbos = null;
    try {
      bbos = new TCByteBufferOutputStream(0);
      fail();
    } catch (IllegalArgumentException iae) {
      // expected
    } finally {
      if (bbos != null) {
        bbos.close();
      }
    }

    try {
      bbos = new TCByteBufferOutputStream(-1);
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

  @Test
  public void testEdgeCase1() {
    testEdgeCase(10, 5, 1004);
  }

  private void testEdgeCase(int bufLength, int byteArrLength, int iterationCount) {

    TCByteBufferOutputStream bbos = new TCByteBufferOutputStream(bufLength);
    try {
      byte data[] = new byte[byteArrLength];
      int dataWriten = 0;
      for (int i = 0; i < iterationCount; i++) {
        bbos.write(data);
        dataWriten += data.length;
      }
      TCReference ref = bbos.accessBuffers();
      Iterator<TCByteBuffer> curs = ref.iterator();
      while (curs.hasNext()) {
        TCByteBuffer current = curs.next();
        if (curs.hasNext()) {
          assertEquals(current.capacity(), current.limit());
          dataWriten -= current.limit();
        } else {
          assertEquals(dataWriten, current.limit());
        }
      }

    } finally {
      bbos.close();
    }
  }

  @Test
  public void testEdgeCase2() {
    testEdgeCase(3, 37, 10);
  }

  @Test
  public void testEdgeCase3() {
    testEdgeCase(1, 1, 5000);
  }

  @Test
  public void testEdgeCase4() {
    TCByteBufferOutputStream bbos = new TCByteBufferOutputStream(27);
    try {
      byte data[] = new byte[370];
      int written = 0;
      for (int i = 0; i < 50; i++) {
        Arrays.fill(data, (byte) 0);
        Arrays.fill(data, i, i + 50, (byte) 42);
        bbos.write(data, i, 50);
        written += 50;
      }
      TCReference ref = bbos.accessBuffers();
      Iterator<TCByteBuffer> curs = ref.iterator();
      while (curs.hasNext()) {
        TCByteBuffer current = curs.next();
        if (curs.hasNext()) {
          assertEquals(current.capacity(), current.limit());
          written -= current.limit();
        } else {
          assertEquals(written, current.limit());
        }
      }
      
      for (TCByteBuffer buf : ref) {
        while (buf.hasRemaining()) {
          assertEquals(42, buf.get());
        }
      }
    } finally {
      bbos.close();
    }
  }

  @Test
  public void testEmpty() {
    TCByteBufferOutputStream bbos = new TCByteBufferOutputStream();
    bbos.close();
    TCReference data = bbos.accessBuffers();
    assertFalse(data.iterator().hasNext());
  }

  @Test
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
