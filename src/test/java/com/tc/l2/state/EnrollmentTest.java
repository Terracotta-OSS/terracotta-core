/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.state;

import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.ServerID;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class EnrollmentTest {

  @Test
  public void testSerialization() throws Exception {
    Enrollment e = new Enrollment(new ServerID("30001", new byte[] { 54, -125, 34, -4 }), true, new long[] {Long.MIN_VALUE, -1, 0, 1, Long.MAX_VALUE});
    TCByteBufferOutputStream bo = new TCByteBufferOutputStream();
    e.serializeTo(bo);
    System.err.println("Written : " + e);
    TCByteBufferInputStream bi = new TCByteBufferInputStream(bo.toArray());
    Enrollment e1 = new Enrollment();
    e1.deserializeFrom(bi);
    System.err.println("Read : " + e1);

    assertEquals(e, e1);

  }

}
