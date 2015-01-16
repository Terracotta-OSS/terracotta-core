/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.msg;

import java.util.Iterator;

import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.object.ObjectID;
import com.tc.objectserver.dgc.api.GarbageCollectionInfo;
import com.tc.util.BasicObjectIDSet;
import com.tc.util.ObjectIDSet;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class GCResultMessageTest {

  private void validate(GCResultMessage gcmsg, GCResultMessage gcmsg1) {
    assertEquals(gcmsg.getType(), gcmsg1.getType());
    assertEquals(gcmsg.getMessageID(), gcmsg1.getMessageID());
    assertEquals(gcmsg.inResponseTo(), gcmsg1.inResponseTo());
    assertEquals(gcmsg.messageFrom(), gcmsg1.messageFrom());

    assertEquals(gcmsg.getGCIterationCount(), gcmsg1.getGCIterationCount());
    ObjectIDSet oidset = (ObjectIDSet) gcmsg.getGCedObjectIDs();
    ObjectIDSet oidset1 = (ObjectIDSet) gcmsg1.getGCedObjectIDs();
    assertEquals(oidset.size(), oidset1.size());

    Iterator<ObjectID> it = oidset.iterator();
    Iterator<ObjectID> it1 = oidset1.iterator();
    while (it.hasNext()) {
      assertEquals(it.next(), it1.next());
    }
    assertFalse(it1.hasNext());
  }

  @SuppressWarnings("resource")
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
    long[] ids = new long[]{1, 2, 4, 8, 9, 10, 11, 12, 13, 14, 15, 17, 18, 19, 23, 25, 26, 27, 28, 29, 33, 35, 47, 56};
    return new BasicObjectIDSet("ImDoingTesting", ids);
  }

  @Test
  public void testBasicSerialization() throws Exception {
    ObjectIDSet deleted = makeObjectIDSet();
    GCResultMessage gcmsg = new GCResultMessage(new GarbageCollectionInfo(), deleted);
    
    GCResultMessage gcmsg1 = writeAndRead(gcmsg);
    validate(gcmsg, gcmsg1);
  }
  
  @Test
  public void testUnmodifiableObjectIDSet() throws Exception {
    ObjectIDSet deleted = ObjectIDSet.unmodifiableObjectIDSet(makeObjectIDSet());
    GCResultMessage gcmsg = new GCResultMessage(new GarbageCollectionInfo(), deleted);
    
    GCResultMessage gcmsg1 = writeAndRead(gcmsg);
    validate(gcmsg, gcmsg1);
    
  }

}
