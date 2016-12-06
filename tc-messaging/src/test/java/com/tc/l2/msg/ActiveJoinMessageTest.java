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
import com.tc.net.ServerID;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ActiveJoinMessageTest {

  private void validate(ActiveJoinMessage ajm, ActiveJoinMessage ajm1) {
    assertEquals(ajm.getType(), ajm1.getType());
    assertEquals(ajm.getMessageID(), ajm1.getMessageID());
    assertEquals(ajm.inResponseTo(), ajm1.inResponseTo());
    assertEquals(ajm.messageFrom(), ajm1.messageFrom());

    assertEquals(ajm.getServerID(), ajm1.getServerID());
  }

  @SuppressWarnings("resource")
  private ActiveJoinMessage writeAndRead(ActiveJoinMessage ajm) throws Exception {
    TCByteBufferOutputStream bo = new TCByteBufferOutputStream();
    ajm.serializeTo(bo);
    System.err.println("Written : " + ajm);
    TCByteBufferInputStream bi = new TCByteBufferInputStream(bo.toArray());
    ActiveJoinMessage ajm1 = new ActiveJoinMessage();
    ajm1.deserializeFrom(bi);
    System.err.println("Read : " + ajm1);
    return ajm1;
  }

  @Test
  public void testBasicSerialization() throws Exception {
    ActiveJoinMessage ajm = (ActiveJoinMessage) ActiveJoinMessage
        .createActiveJoinMessage(new ServerID("30001", new byte[] { 54, -125, 34, -4 }));
    ActiveJoinMessage ajm1 = writeAndRead(ajm);
    validate(ajm, ajm1);

    ajm = (ActiveJoinMessage) ActiveJoinMessage.createActiveLeftMessage();
    ajm1 = writeAndRead(ajm);
    validate(ajm, ajm1);

  }
}
