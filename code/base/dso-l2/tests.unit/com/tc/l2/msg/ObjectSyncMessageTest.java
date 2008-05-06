/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.msg;

import com.tc.async.api.Sink;
import com.tc.async.impl.MockSink;
import com.tc.bytes.TCByteBuffer;
import com.tc.bytes.TCByteBufferFactory;
import com.tc.bytes.TCByteBufferTestUtil;
import com.tc.l2.context.ManagedObjectSyncContext;
import com.tc.net.groups.NodeID;
import com.tc.net.groups.NodeIDImpl;
import com.tc.object.ObjectID;
import com.tc.object.dna.impl.ObjectStringSerializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import junit.framework.TestCase;

public class ObjectSyncMessageTest extends TestCase {
  private ManagedObjectSyncContext managedObjectSyncContext;
  private TCByteBuffer[]           tcByteBufferArray;
  private ObjectStringSerializer   objectStringSerializer;
  private final int                dnaCount = 56;

  public void setUp() {
    NodeID nodeID = new NodeIDImpl("foo", "foobar".getBytes());
    HashMap rootsMap = new HashMap();
    rootsMap.put("root1", new ObjectID(1));
    rootsMap.put("root2", new ObjectID(2));
    rootsMap.put("root3", new ObjectID(3));
    Sink sink = new MockSink();
    objectStringSerializer = new ObjectStringSerializer();
    TCByteBuffer tcbb = TCByteBufferFactory.getInstance(false, 3452);
    tcByteBufferArray = new TCByteBuffer[] { tcbb };
    managedObjectSyncContext = new ManagedObjectSyncContext(nodeID, rootsMap, true, sink, 100, 10);
    managedObjectSyncContext.setDehydratedBytes(new TCByteBuffer[] { tcbb }, dnaCount, objectStringSerializer);
    managedObjectSyncContext.setSequenceID(11);
  }

  public void tearDown() {
    managedObjectSyncContext = null;
  }

  private void validate(ObjectSyncMessage osm, ObjectSyncMessage osm1) {
    assertEquals(osm.getType(), osm1.getType());
    assertEquals(osm.getMessageID(), osm1.getMessageID());
    assertEquals(osm.inResponseTo(), osm1.inResponseTo());
    assertEquals(osm.messageFrom(), osm1.messageFrom());

    assertEquals(osm.getRootsMap(), osm1.getRootsMap());
    assertEquals(osm.getDnaCount(), osm1.getDnaCount());

    Set oids = osm.getOids();
    Set oids1 = osm1.getOids();
    assertEquals(oids.size(), oids1.size());
    for (Iterator iter = osm.getOids().iterator(); iter.hasNext();) {
      ObjectID oid = (ObjectID) iter.next();
      assertTrue(oids1.contains(oid));
      oids1.remove(oid);
    }
    assertTrue(oids1.isEmpty());

    TCByteBuffer[] dnas1 = osm1.getUnprocessedDNAs();

    TCByteBufferTestUtil.checkEquals(tcByteBufferArray, dnas1);

    assertEquals(osm.getSequenceID(), osm1.getSequenceID());
  }

  private ObjectSyncMessage writeAndRead(ObjectSyncMessage osm) throws Exception {
    ByteArrayOutputStream bo = new ByteArrayOutputStream();
    ObjectOutput oo = new ObjectOutputStream(bo);
    oo.writeObject(osm);
    System.err.println("Written : " + osm);
    ByteArrayInputStream bi = new ByteArrayInputStream(bo.toByteArray());
    ObjectInput oi = new ObjectInputStream(bi);
    ObjectSyncMessage osm1 = (ObjectSyncMessage) oi.readObject();
    System.err.println("Read : " + osm1);
    return osm1;
  }

  public void testBasicSerialization() throws Exception {
    ObjectSyncMessage osm = ObjectSyncMessageFactory.createObjectSyncMessageFrom(managedObjectSyncContext);
    ObjectSyncMessage osm1 = writeAndRead(osm);
    validate(osm, osm1);
  }
}
