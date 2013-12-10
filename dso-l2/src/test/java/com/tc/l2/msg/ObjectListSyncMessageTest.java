/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.msg;

import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.l2.state.StateManager;
import com.tc.object.ObjectID;
import com.tc.util.ObjectIDSet;

import java.util.Set;

import junit.framework.TestCase;

public class ObjectListSyncMessageTest extends TestCase {
  private ObjectListSyncMessage objectListSyncMessage;
  private Set                   oids;

  @Override
  public void setUp() {
    objectListSyncMessage = new ObjectListSyncMessage();
    oids = new ObjectIDSet();
    oids.add(new ObjectID(1234));
    oids.add(new ObjectID(456));
    oids.add(new ObjectID(9068));
  }

  @Override
  public void tearDown() {
    objectListSyncMessage = null;
    oids = null;
  }

  private void validate(ObjectListSyncMessage olsm, ObjectListSyncMessage olsm1) {
    assertEquals(olsm.getType(), olsm1.getType());
    assertEquals(olsm.getMessageID(), olsm1.getMessageID());
    assertEquals(olsm.inResponseTo(), olsm1.inResponseTo());
    assertEquals(olsm.messageFrom(), olsm1.messageFrom());

    if (olsm.getType() == ObjectListSyncMessage.RESPONSE) {
      assertEquals(olsm.isSyncAllowed(), olsm1.isSyncAllowed());
      assertEquals(olsm.getOffheapSize(), olsm1.getOffheapSize());
      assertEquals(olsm.getDataStorageSize(), olsm1.getDataStorageSize());
    } else {
      assertEquals(olsm.toString(), olsm1.toString());
    }
  }

  private ObjectListSyncMessage writeAndRead(ObjectListSyncMessage olsm) throws Exception {
    TCByteBufferOutputStream bo = new TCByteBufferOutputStream();
    olsm.serializeTo(bo);
    System.err.println("Written : " + olsm);
    TCByteBufferInputStream bi = new TCByteBufferInputStream(bo.toArray());
    ObjectListSyncMessage olsm1 = new ObjectListSyncMessage();
    olsm1.deserializeFrom(bi);
    System.err.println("Read : " + olsm1);
    return olsm1;
  }

  public void testBasicSerialization() throws Exception {
    ObjectListSyncMessage olsm = ObjectListSyncMessageFactory
        .createObjectListSyncRequestMessage();
    ObjectListSyncMessage olsm1 = writeAndRead(olsm);
    validate(olsm, olsm1);

    olsm = ObjectListSyncMessageFactory
        .createObjectListSyncResponseMessage(objectListSyncMessage, StateManager.PASSIVE_UNINITIALIZED, true, 1L, 1L);
    olsm1 = writeAndRead(olsm);
    validate(olsm, olsm1);
  }
}
