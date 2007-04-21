/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.msg;

import com.tc.object.ObjectID;
import com.tc.util.SyncObjectIdSetImpl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.Iterator;
import java.util.Set;

import junit.framework.TestCase;

public class ObjectListSyncMessageTest extends TestCase {
  private ObjectListSyncMessage objectListSyncMessage;
  private Set                   oids;

  public void setUp() {
    objectListSyncMessage = new ObjectListSyncMessage();
    oids = new SyncObjectIdSetImpl();
    oids.add(new ObjectID(1234));
    oids.add(new ObjectID(456));
    oids.add(new ObjectID(9068));
  }

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
      assertEquals(olsm.getObjectIDs().size(), olsm1.getObjectIDs().size());

      Iterator iter1 = olsm1.getObjectIDs().iterator();
      for (Iterator iter = olsm.getObjectIDs().iterator(); iter.hasNext();) {
        assertEquals(iter.next(), iter1.next());
      }
    } else {
      assertEquals(olsm.toString(), olsm1.toString());
    }
  }

  private ObjectListSyncMessage writeAndRead(ObjectListSyncMessage olsm) throws Exception {
    ByteArrayOutputStream bo = new ByteArrayOutputStream();
    ObjectOutput oo = new ObjectOutputStream(bo);
    oo.writeObject(olsm);
    System.err.println("Written : " + olsm);
    ByteArrayInputStream bi = new ByteArrayInputStream(bo.toByteArray());
    ObjectInput oi = new ObjectInputStream(bi);
    ObjectListSyncMessage olsm1 = (ObjectListSyncMessage) oi.readObject();
    System.err.println("Read : " + olsm1);
    return olsm1;
  }

  public void testBasicSerialization() throws Exception {
    ObjectListSyncMessage olsm = (ObjectListSyncMessage) ObjectListSyncMessageFactory
        .createObjectListSyncRequestMessage();
    ObjectListSyncMessage olsm1 = writeAndRead(olsm);
    validate(olsm, olsm1);

    olsm = (ObjectListSyncMessage) ObjectListSyncMessageFactory
        .createObjectListSyncResponseMessage(objectListSyncMessage, oids);
    olsm1 = writeAndRead(olsm);
    validate(olsm, olsm1);
  }
}
