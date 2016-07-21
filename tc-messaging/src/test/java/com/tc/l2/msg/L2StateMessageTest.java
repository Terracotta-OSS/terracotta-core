/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.l2.msg;

import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.l2.state.Enrollment;
import com.tc.net.ServerID;

import com.tc.util.State;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class L2StateMessageTest {

  private Enrollment     enrollment;
  private L2StateMessage l2StateMessage;

  @Before
  public void setUp() {
    enrollment = new Enrollment(new ServerID("30001", new byte[] { 54, -125, 34, -4 }), true, new long[] {0, 1});
    l2StateMessage = new L2StateMessage();
  }

  @After
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

  @SuppressWarnings("resource")
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

  @Test
  public void testBasicSerialization() throws Exception {
    L2StateMessage l2sm = L2StateMessage.createElectionStartedMessage(enrollment, new State("dummy"));
    L2StateMessage l2sm1 = writeAndRead(l2sm);
    validate(l2sm, l2sm1);

    l2sm = L2StateMessage.createElectionResultMessage(enrollment, new State("dummy"));
    l2sm1 = writeAndRead(l2sm);
    validate(l2sm, l2sm1);

    l2sm = L2StateMessage.createElectionWonMessage(enrollment, new State("dummy"));
    l2sm1 = writeAndRead(l2sm);
    validate(l2sm, l2sm1);

    l2sm = L2StateMessage.createAbortElectionMessage(l2StateMessage, enrollment, new State("dummy"));
    l2sm1 = writeAndRead(l2sm);
    validate(l2sm, l2sm1);

    l2sm = L2StateMessage.createElectionStartedMessage(l2StateMessage, enrollment, new State("dummy"));
    l2sm1 = writeAndRead(l2sm);
    validate(l2sm, l2sm1);

    l2sm = L2StateMessage.createResultConflictMessage(l2StateMessage, enrollment, new State("dummy"));
    l2sm1 = writeAndRead(l2sm);
    validate(l2sm, l2sm1);

    l2sm = L2StateMessage.createResultAgreedMessage(l2StateMessage, enrollment, new State("dummy"));
    l2sm1 = writeAndRead(l2sm);
    validate(l2sm, l2sm1);
  }

}
