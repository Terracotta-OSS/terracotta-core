/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.msg;

import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.l2.ha.WeightGeneratorFactory;
import com.tc.l2.state.Enrollment;
import com.tc.l2.state.EnrollmentFactory;
import com.tc.net.ServerID;

import junit.framework.TestCase;

public class L2StateMessageTest extends TestCase {

  private Enrollment     enrollment;
  private L2StateMessage l2StateMessage;

  public void setUp() {
    enrollment = EnrollmentFactory.createEnrollment(new ServerID("30001", new byte[] { 54, -125, 34, -4 }), true,
                                                    WeightGeneratorFactory.createDefaultFactory());
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
    TCByteBufferOutputStream bo = new TCByteBufferOutputStream();
    l2sm.serializeTo(bo);
    System.err.println("Written : " + l2sm);
    TCByteBufferInputStream bi = new TCByteBufferInputStream(bo.toArray());
    L2StateMessage l2sm1 = new L2StateMessage();
    l2sm1.deserializeFrom(bi);
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
