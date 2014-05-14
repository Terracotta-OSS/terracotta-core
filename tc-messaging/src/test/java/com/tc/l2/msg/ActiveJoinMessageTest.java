/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.msg;

import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.GroupID;
import com.tc.net.ServerID;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ActiveJoinMessageTest {

  private void validate(ActiveJoinMessage ajm, ActiveJoinMessage ajm1) {
    assertEquals(ajm.getType(), ajm1.getType());
    assertEquals(ajm.getMessageID(), ajm1.getMessageID());
    assertEquals(ajm.inResponseTo(), ajm1.inResponseTo());
    assertEquals(ajm.messageFrom(), ajm1.messageFrom());

    assertEquals(ajm.getGroupID(), ajm1.getGroupID());
    assertEquals(ajm.getServerID(), ajm1.getServerID());
  }

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
        .createActiveJoinMessage(new GroupID(100), new ServerID("30001", new byte[] { 54, -125, 34, -4 }));
    ActiveJoinMessage ajm1 = writeAndRead(ajm);
    validate(ajm, ajm1);

    ajm = (ActiveJoinMessage) ActiveJoinMessage.createActiveLeftMessage(new GroupID(100));
    ajm1 = writeAndRead(ajm);
    validate(ajm, ajm1);

  }
}
