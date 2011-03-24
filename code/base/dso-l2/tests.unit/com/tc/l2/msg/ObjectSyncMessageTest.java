/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.msg;

import com.tc.bytes.TCByteBuffer;
import com.tc.bytes.TCByteBufferFactory;
import com.tc.bytes.TCByteBufferTestUtil;
import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.l2.context.ManagedObjectSyncContext;
import com.tc.net.NodeID;
import com.tc.net.ServerID;
import com.tc.object.ObjectID;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.dna.impl.ObjectStringSerializerImpl;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;
import com.tc.util.ObjectIDSet;
import com.tc.util.TCCollections;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import junit.framework.TestCase;

public class ObjectSyncMessageTest extends TestCase {
  private ManagedObjectSyncContext managedObjectSyncContext;
  private TCByteBuffer[]           tcByteBufferArray = null;
  private ObjectStringSerializer   objectStringSerializer;
  private static final int         dnaCount          = 56;

  @Override
  public void setUp() {
    NodeID nodeID = new ServerID("foo", "foobar".getBytes());
    HashMap rootsMap = new HashMap();
    rootsMap.put("root1", new ObjectID(1));
    rootsMap.put("root2", new ObjectID(2));
    rootsMap.put("root3", new ObjectID(3));
    this.objectStringSerializer = new ObjectStringSerializerImpl();
    TCByteBuffer tcbb = TCByteBufferFactory.getInstance(false, 3452);
    this.tcByteBufferArray = new TCByteBuffer[] { tcbb };
    ObjectIDSet oids = new ObjectIDSet(rootsMap.values());
    this.managedObjectSyncContext = new ManagedObjectSyncContext(nodeID, rootsMap, oids, true, 100, 10);
    this.managedObjectSyncContext
        .setDehydratedBytes(oids, TCCollections.EMPTY_OBJECT_ID_SET, new TCByteBuffer[] { tcbb }, dnaCount,
                            this.objectStringSerializer);
    this.managedObjectSyncContext.setSequenceID(11);
  }

  @Override
  public void tearDown() {
    this.managedObjectSyncContext = null;
  }

  private void validate(ObjectSyncMessage osm, ObjectSyncMessage osm1) {
    assertEquals(osm.getType(), osm1.getType());
    assertEquals(osm.getMessageID(), osm1.getMessageID());
    assertEquals(osm.inResponseTo(), osm1.inResponseTo());
    assertEquals(osm.messageFrom(), osm1.messageFrom());

    assertEquals(osm.getRootsMap(), osm1.getRootsMap());
    assertEquals(osm.getDnaCount(), osm1.getDnaCount());
    assertEquals(osm.getServerTransactionID(), osm1.getServerTransactionID());

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

    TCByteBufferTestUtil.checkEquals(this.tcByteBufferArray, dnas1);

    assertEquals(osm.getSequenceID(), osm1.getSequenceID());
  }

  private ObjectSyncMessage writeAndRead(ObjectSyncMessage osm) throws Exception {
    TCByteBufferOutputStream bo = new TCByteBufferOutputStream();
    osm.serializeTo(bo);
    System.err.println("Written : " + osm);
    TCByteBufferInputStream bi = new TCByteBufferInputStream(bo.toArray());
    ObjectSyncMessage osm1 = new ObjectSyncMessage();
    osm1.deserializeFrom(bi);
    System.err.println("Read : " + osm1);
    return osm1;
  }

  public void testBasicSerialization() throws Exception {
    ObjectSyncMessage osm = ObjectSyncMessageFactory
        .createObjectSyncMessageFrom(this.managedObjectSyncContext,
                                     new ServerTransactionID(new ServerID("xyz", new byte[] { 3, 4, 5 }),
                                                             new TransactionID(99)));
    ObjectSyncMessage osm1 = writeAndRead(osm);
    validate(osm, osm1);
  }
}
