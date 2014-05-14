/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.msg;

import java.util.HashMap;
import java.util.Map;

import com.tc.bytes.TCByteBuffer;
import com.tc.bytes.TCByteBufferFactory;
import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.ServerID;
import com.tc.object.ObjectID;
import com.tc.object.dna.impl.ObjectStringSerializerImpl;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;
import com.tc.util.Assert;
import com.tc.util.BasicObjectIDSet;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class ObjectSyncMessageTest {
  private TCByteBuffer[] tcByteBufferArray = null;

  @Before
  public void setUp() {
    TCByteBuffer tcbb = TCByteBufferFactory.getInstance(false, 3452);
    this.tcByteBufferArray = new TCByteBuffer[] { tcbb };
  }

  private void validate(ObjectSyncMessage osm, ObjectSyncMessage osm1) {
    assertEquals(osm.getType(), osm1.getType());
    assertEquals(osm.getMessageID(), osm1.getMessageID());
    assertEquals(osm.inResponseTo(), osm1.inResponseTo());
    assertEquals(osm.messageFrom(), osm1.messageFrom());

    assertEquals(osm.getRootsMap(), osm1.getRootsMap());
    assertEquals(osm.getDnaCount(), osm1.getDnaCount());
    assertEquals(osm.getServerTransactionID(), osm1.getServerTransactionID());

    assertThat(osm.getOids(), equalTo(osm1.getOids()));

    TCByteBuffer[] dnas1 = osm1.getUnprocessedDNAs();

    checkEquals(this.tcByteBufferArray, dnas1);

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

  @Test
  public void testBasicSerialization() throws Exception {
    Map<String, ObjectID> rootsMap = new HashMap<String, ObjectID>();
    rootsMap.put("root1", new ObjectID(1));
    rootsMap.put("root2", new ObjectID(2));
    rootsMap.put("root3", new ObjectID(3));
    
    ObjectSyncMessage osm = new ObjectSyncMessage(new ServerTransactionID(new ServerID("xyz", new byte[] { 3, 4, 5 }), new TransactionID(99)),
            new BasicObjectIDSet("ImDoingTesting", 1, 2, 3), 56, tcByteBufferArray, new ObjectStringSerializerImpl(), rootsMap, 11, new BasicObjectIDSet());
    ObjectSyncMessage osm1 = writeAndRead(osm);
    validate(osm, osm1);
  }

  private static void checkEquals(TCByteBuffer[] expected, TCByteBuffer[] actual) {
    int j = 0;
    for (int i = 0; i < expected.length; i++) {
      while (expected[i].remaining() > 0) {
        byte expectedValue = expected[i].get();
        while (!actual[j].hasRemaining()) {
          j++;
          if (j >= actual.length) { throw new AssertionError("ran out of buffers: " + j); }
        }

        byte actualValue = actual[j].get();
        if (actualValue != expectedValue) {
          //
          throw new AssertionError("Data is not the same, " + actualValue + "!=" + expectedValue + " at expected[" + i
                                   + "] and actual[" + j + "]");
        }
      }
    }

    if (actual.length != 0) {
      Assert.assertEquals(actual.length, j + 1);
      Assert.assertEquals(0, actual[j].remaining());
    }
  }
}
