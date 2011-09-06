/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.msg;

import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.object.ObjectID;
import com.tc.objectserver.dgc.api.GarbageCollectionInfo;
import com.tc.util.ObjectIDSet;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;

public class GCResultMessageTest extends TestCase {

  private void validate(GCResultMessage gcmsg, GCResultMessage gcmsg1) {
    assertEquals(gcmsg.getType(), gcmsg1.getType());
    assertEquals(gcmsg.getMessageID(), gcmsg1.getMessageID());
    assertEquals(gcmsg.inResponseTo(), gcmsg1.inResponseTo());
    assertEquals(gcmsg.messageFrom(), gcmsg1.messageFrom());

    assertEquals(gcmsg.getGCIterationCount(), gcmsg1.getGCIterationCount());
    ObjectIDSet oidset = (ObjectIDSet) gcmsg.getGCedObjectIDs();
    ObjectIDSet oidset1 = (ObjectIDSet) gcmsg1.getGCedObjectIDs();
    assertEquals(oidset.size(), oidset1.size());

    Iterator it = oidset.iterator();
    Iterator it1 = oidset1.iterator();
    while (it.hasNext()) {
      assertEquals(it.next(), it1.next());
    }
    assertFalse(it1.hasNext());
  }

  private GCResultMessage writeAndRead(GCResultMessage gcmsg) throws Exception {
    TCByteBufferOutputStream bo = new TCByteBufferOutputStream();
    gcmsg.serializeTo(bo);
    System.err.println("Written : " + gcmsg);
    TCByteBufferInputStream bi = new TCByteBufferInputStream(bo.toArray());
    GCResultMessage gcmsg1 = new GCResultMessage();
    gcmsg1.deserializeFrom(bi);
    System.err.println("Read : " + gcmsg1);
    return gcmsg1;
  }

  private ObjectIDSet makeObjectIDSet() {
    List longList = new ArrayList();
    longList.add(new ObjectID(25));
    longList.add(new ObjectID(26));
    longList.add(new ObjectID(27));
    longList.add(new ObjectID(28));
    longList.add(new ObjectID(9));
    longList.add(new ObjectID(13));
    longList.add(new ObjectID(12));
    longList.add(new ObjectID(14));
    longList.add(new ObjectID(18));
    longList.add(new ObjectID(2));
    longList.add(new ObjectID(23));
    longList.add(new ObjectID(47));
    longList.add(new ObjectID(35));
    longList.add(new ObjectID(10));
    longList.add(new ObjectID(1));
    longList.add(new ObjectID(4));
    longList.add(new ObjectID(15));
    longList.add(new ObjectID(8));
    longList.add(new ObjectID(56));
    longList.add(new ObjectID(11));
    longList.add(new ObjectID(10));
    longList.add(new ObjectID(33));
    longList.add(new ObjectID(17));
    longList.add(new ObjectID(29));
    longList.add(new ObjectID(19));

    return new ObjectIDSet(longList);
  }

  public void testBasicSerialization() throws Exception {
    ObjectIDSet deleted = makeObjectIDSet();
    GCResultMessage gcmsg = GCResultMessageFactory.createGCResultMessage(new GarbageCollectionInfo(), deleted);
    
    GCResultMessage gcmsg1 = writeAndRead(gcmsg);
    validate(gcmsg, gcmsg1);
  }
  
  public void testUnmodifiableObjectIDSet() throws Exception {
    ObjectIDSet deleted = ObjectIDSet.unmodifiableObjectIDSet(makeObjectIDSet());
    GCResultMessage gcmsg = GCResultMessageFactory.createGCResultMessage(new GarbageCollectionInfo(), deleted);
    
    GCResultMessage gcmsg1 = writeAndRead(gcmsg);
    validate(gcmsg, gcmsg1);
    
  }

}
