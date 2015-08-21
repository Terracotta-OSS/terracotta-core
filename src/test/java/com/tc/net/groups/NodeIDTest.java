/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.groups;

import java.util.HashSet;

import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.NodeID;
import com.tc.net.ServerID;
import com.tc.net.StripeID;
import com.tc.util.UUID;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NodeIDTest {
  
  @Test
  public void test() throws Exception {
    byte[] b1 = new byte[] { 34, 55, 2 , (byte) 255, 0 };
    byte[] b2 = new byte[] { 34, 55, 2 , (byte) 255, 0, 4 };
    byte[] b3 = new byte[] { 43, 5, 127 , (byte) 255, -87, 9 };
    byte[] b4 = new byte[] { 4};
    
    NodeID n1 = new ServerID("node1", b1);
    NodeID n2 = new ServerID("node2", b2);
    NodeID n3 = new ServerID("node3", b3);
    NodeID n4 = new ServerID("node4", b4);
    NodeID n5 = new StripeID(UUID.getUUID().toString());
    NodeID n6 = new StripeID(UUID.getUUID().toString());
    
    assertFalse(n1.equals(n2));
    assertTrue(n1.equals(n1));
    assertFalse(n1.equals(n3));
    assertTrue(n3.equals(n3));
    assertFalse(n1.equals(n4));
    assertTrue(n4.equals(n4));
    assertFalse(n3.equals(n4));
    assertTrue(n2.equals(n2));
    assertTrue(n5.equals(n5));
    assertFalse(n5.equals(n4));
    assertFalse(n5.equals(n6));
    
    HashSet set = new HashSet();
    assertTrue(set.add(n1));
    assertTrue(set.add(n2));
    assertTrue(set.add(n3));
    assertTrue(set.add(n4));
    assertTrue(set.add(n5));
    assertTrue(set.add(n6));
    assertFalse(set.add(n1));
    assertFalse(set.add(n2));
    assertFalse(set.add(n3));
    assertFalse(set.add(n4));
    assertFalse(set.add(n5));
    assertFalse(set.add(n6));
    
    
    TCByteBufferOutputStream bo = new TCByteBufferOutputStream();
    NodeIDSerializer serializer = new NodeIDSerializer(n1);
    serializer.serializeTo(bo);
    serializer = new NodeIDSerializer(n2);
    serializer.serializeTo(bo);
    serializer = new NodeIDSerializer(n3);
    serializer.serializeTo(bo);
    serializer = new NodeIDSerializer(n4);
    serializer.serializeTo(bo);
    serializer = new NodeIDSerializer(n5);
    serializer.serializeTo(bo);
    serializer = new NodeIDSerializer(n6);
    serializer.serializeTo(bo);
    serializer = new NodeIDSerializer(ServerID.NULL_ID);
    serializer.serializeTo(bo);

    TCByteBufferInputStream bi = new TCByteBufferInputStream(bo.toArray());
    serializer = new NodeIDSerializer();
    serializer.deserializeFrom(bi);
    NodeID r1 = serializer.getNodeID();
    assertEquals(n1, r1);
    serializer = new NodeIDSerializer();
    serializer.deserializeFrom(bi);
    NodeID r2 = serializer.getNodeID();
    assertEquals(n2, r2);
    serializer = new NodeIDSerializer();
    serializer.deserializeFrom(bi);
    NodeID r3 = serializer.getNodeID();
    assertEquals(n3, r3);
    serializer = new NodeIDSerializer();
    serializer.deserializeFrom(bi);
    NodeID r4 = serializer.getNodeID();
    assertEquals(n4, r4);
    serializer = new NodeIDSerializer();
    serializer.deserializeFrom(bi);
    NodeID r5 = serializer.getNodeID();
    assertEquals(n5, r5);
    serializer = new NodeIDSerializer();
    serializer.deserializeFrom(bi);
    NodeID r6 = serializer.getNodeID();
    assertEquals(n6, r6);
    serializer = new NodeIDSerializer();
    serializer.deserializeFrom(bi);
    NodeID r7 = serializer.getNodeID();
    assertEquals(ServerID.NULL_ID, r7);

  }
}
