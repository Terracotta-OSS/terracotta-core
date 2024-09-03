/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
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
    TCByteBufferInputStream bi = new TCByteBufferInputStream(bo.accessBuffers());
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
