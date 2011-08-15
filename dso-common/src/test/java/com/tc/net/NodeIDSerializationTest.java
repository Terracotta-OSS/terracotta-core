/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net;

import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.groups.NodeIDSerializer;
import com.tc.util.UUID;

import junit.framework.TestCase;

public class NodeIDSerializationTest extends TestCase {

  public void testSerialization() throws Exception {
    NodeID n1 = new ServerID("node1", UUID.getUUID().toString().getBytes());
    NodeID n2 = dupBySerialization(n1);
    assertTrue(n1.equals(n2));
    assertTrue(n2 instanceof ServerID);

    NodeID n3 = new GroupID(100);
    NodeID n4 = dupBySerialization(n3);
    assertTrue(n3.equals(n4));
    assertTrue(n4 instanceof GroupID);

    NodeID n5 = new ClientID(1000);
    NodeID n6 = dupBySerialization(n5);
    assertTrue(n5.equals(n6));
    assertTrue(n6 instanceof ClientID);
    
    NodeID n7 = new StripeID(UUID.getUUID().toString());
    NodeID n8 = dupBySerialization(n7);
    assertTrue(n7.equals(n8));
    assertTrue(n8 instanceof StripeID);
  }

  private NodeID dupBySerialization(NodeID orig) throws Exception {
    TCByteBufferOutputStream out = new TCByteBufferOutputStream();
    NodeIDSerializer nodeIDSerializer = new NodeIDSerializer(orig);
    nodeIDSerializer.serializeTo(out);
    out.close();

    TCByteBufferInputStream in = new TCByteBufferInputStream(out.toArray());
    NodeIDSerializer nodeIDSerializer2 = new NodeIDSerializer();
    nodeIDSerializer2.deserializeFrom(in);
    NodeID dup = nodeIDSerializer2.getNodeID();
    return dup;
  }

  public void testTCSerilaizable() throws Exception {
    NodeID n1 = new ServerID("node1", UUID.getUUID().toString().getBytes());
    NodeID n2 = dupByTCSerializable(n1);
    assertTrue(n1.equals(n2));
    assertTrue(n2 instanceof ServerID);

    NodeID n3 = new GroupID(100);
    NodeID n4 = dupByTCSerializable(n3);
    assertTrue(n3.equals(n4));
    assertTrue(n4 instanceof GroupID);

    NodeID n5 = new ClientID(1000);
    NodeID n6 = dupByTCSerializable(n5);
    assertTrue(n5.equals(n6));
    assertTrue(n6 instanceof ClientID);
    
    NodeID n7 = new StripeID(UUID.getUUID().toString());
    NodeID n8 = dupByTCSerializable(n7);
    assertTrue(n7.equals(n8));
    assertTrue(n8 instanceof StripeID);
  }

  private NodeID dupByTCSerializable(NodeID orig) throws Exception {
    NodeIDSerializer serializer = new NodeIDSerializer(orig);
    TCByteBufferOutputStream out = new TCByteBufferOutputStream();
    serializer.serializeTo(out);
    out.close();

    TCByteBufferInputStream in = new TCByteBufferInputStream(out.toArray());
    serializer = new NodeIDSerializer();
    NodeID dup = ((NodeIDSerializer) serializer.deserializeFrom(in)).getNodeID();
    return dup;
  }

}
