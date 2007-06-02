/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.state;

import com.tc.l2.ha.WeightGeneratorFactory;
import com.tc.net.groups.NodeID;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

import junit.framework.TestCase;

public class EnrollmentTest extends TestCase {

  public void testSerialization() throws Exception {
    Enrollment e = EnrollmentFactory.createEnrollment(new NodeID("30001", new byte[] { 54, -125, 34, -4 }), true,
                                                      WeightGeneratorFactory.createDefaultFactory());
    ByteArrayOutputStream bo = new ByteArrayOutputStream();
    ObjectOutput oo = new ObjectOutputStream(bo);
    oo.writeObject(e);
    System.err.println("Written : " + e);
    ByteArrayInputStream bi = new ByteArrayInputStream(bo.toByteArray());
    ObjectInput oi = new ObjectInputStream(bi);
    Enrollment e1 = (Enrollment) oi.readObject();
    System.err.println("Read : " + e1);

    assertEquals(e, e1);

  }

}
