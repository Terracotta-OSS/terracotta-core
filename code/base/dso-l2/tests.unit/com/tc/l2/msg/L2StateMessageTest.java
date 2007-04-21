/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.msg;

import com.tc.l2.state.Enrollment;
import com.tc.l2.state.EnrollmentFactory;
import com.tc.net.groups.NodeID;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

import junit.framework.TestCase;

public class L2StateMessageTest extends TestCase {

  private Enrollment     enrollment;
  private L2StateMessage l2StateMessage;

  public void setUp() {
    enrollment = EnrollmentFactory.createEnrollment(new NodeID("30001", new byte[] { 54, -125, 34, -4 }), true);
    l2StateMessage = new L2StateMessage();
  }

  public void tearDown() {
    enrollment = null;
    l2StateMessage = null;
  }

  private void validate(L2StateMessage l2sm, L2StateMessage l2sm1) {
    assertEquals(l2sm.getType(), l2sm1.getType());
    assertEquals(l2sm.getMessageID(), l2sm1.getMessageID());
    assertEquals(l2sm.inResponseTo(), l2sm1.inResponseTo());
    assertEquals(l2sm.messageFrom(), l2sm1.messageFrom());

    assertEquals(l2sm.getEnrollment(), l2sm1.getEnrollment());
    assertEquals(l2sm.toString(), l2sm1.toString());
  }

  private L2StateMessage writeAndRead(L2StateMessage l2sm) throws Exception {
    ByteArrayOutputStream bo = new ByteArrayOutputStream();
    ObjectOutput oo = new ObjectOutputStream(bo);
    oo.writeObject(l2sm);
    System.err.println("Written : " + l2sm);
    ByteArrayInputStream bi = new ByteArrayInputStream(bo.toByteArray());
    ObjectInput oi = new ObjectInputStream(bi);
    L2StateMessage l2sm1 = (L2StateMessage) oi.readObject();
    System.err.println("Read : " + l2sm1);
    return l2sm1;
  }

  public void testBasicSerialization() throws Exception {
    L2StateMessage l2sm = (L2StateMessage) L2StateMessageFactory.createElectionStartedMessage(enrollment);
    L2StateMessage l2sm1 = writeAndRead(l2sm);
    validate(l2sm, l2sm1);

    l2sm = (L2StateMessage) L2StateMessageFactory.createElectionResultMessage(enrollment);
    l2sm1 = writeAndRead(l2sm);
    validate(l2sm, l2sm1);

    l2sm = (L2StateMessage) L2StateMessageFactory.createElectionWonMessage(enrollment);
    l2sm1 = writeAndRead(l2sm);
    validate(l2sm, l2sm1);

    l2sm = (L2StateMessage) L2StateMessageFactory.createMoveToPassiveStandbyMessage(enrollment);
    l2sm1 = writeAndRead(l2sm);
    validate(l2sm, l2sm1);

    l2sm = (L2StateMessage) L2StateMessageFactory.createAbortElectionMessage(l2StateMessage, enrollment);
    l2sm1 = writeAndRead(l2sm);
    validate(l2sm, l2sm1);

    l2sm = (L2StateMessage) L2StateMessageFactory.createElectionStartedMessage(l2StateMessage, enrollment);
    l2sm1 = writeAndRead(l2sm);
    validate(l2sm, l2sm1);

    l2sm = (L2StateMessage) L2StateMessageFactory.createResultConflictMessage(l2StateMessage, enrollment);
    l2sm1 = writeAndRead(l2sm);
    validate(l2sm, l2sm1);

    l2sm = (L2StateMessage) L2StateMessageFactory.createResultAgreedMessage(l2StateMessage, enrollment);
    l2sm1 = writeAndRead(l2sm);
    validate(l2sm, l2sm1);
  }

}
