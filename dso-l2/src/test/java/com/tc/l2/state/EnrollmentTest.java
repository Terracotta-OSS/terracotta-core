/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.state;

import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.l2.ha.WeightGeneratorFactory;
import com.tc.net.ServerID;

import junit.framework.TestCase;

public class EnrollmentTest extends TestCase {

  public void testSerialization() throws Exception {
    Enrollment e = EnrollmentFactory.createEnrollment(new ServerID("30001", new byte[] { 54, -125, 34, -4 }), true,
                                                      WeightGeneratorFactory.createDefaultFactory());
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
