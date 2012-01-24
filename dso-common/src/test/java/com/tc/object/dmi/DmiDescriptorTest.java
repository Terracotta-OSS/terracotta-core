/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.dmi;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.object.ObjectID;

import java.io.IOException;
import java.util.Arrays;

import junit.framework.TestCase;

public class DmiDescriptorTest extends TestCase {

  final ObjectID       receiverId = new ObjectID(567);
  final ObjectID       dmiCallId  = new ObjectID(789);
  final boolean        faultRec   = true;
  final DmiClassSpec[] classSpecs = new DmiClassSpec[] { new DmiClassSpec("className") };

  public void testSerialization() throws IOException {

    final DmiDescriptor dd1 = new DmiDescriptor(receiverId, dmiCallId, classSpecs, faultRec);
    final DmiDescriptor dd2 = writeAndRead(dd1);
    check(dd1, dd2);
  }

  private void check(DmiDescriptor dd1, DmiDescriptor dd2) {
    check(dd1);
    check(dd2);
  }

  private void check(DmiDescriptor dd2) {
    assertEquals(receiverId, dd2.getReceiverId());
    assertEquals(dmiCallId, dd2.getDmiCallId());
    assertEquals(faultRec, dd2.isFaultReceiver());
    assertTrue(Arrays.equals(classSpecs, dd2.getClassSpecs()));
  }

  private DmiDescriptor writeAndRead(DmiDescriptor dd1) throws IOException {
    final TCByteBufferInputStream in = new TCByteBufferInputStream(write(dd1));
    final DmiDescriptor rv = new DmiDescriptor();
    rv.deserializeFrom(in);
    assertTrue(in.available() == 0);
    return rv;
  }

  private TCByteBuffer[] write(DmiDescriptor dd) {
    final TCByteBufferOutputStream out = new TCByteBufferOutputStream();
    dd.serializeTo(out);
    out.close();
    return out.toArray();
  }
}
