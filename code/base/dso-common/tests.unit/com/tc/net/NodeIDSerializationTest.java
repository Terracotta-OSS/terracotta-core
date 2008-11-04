/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net;

import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.io.serializer.TCObjectInputStream;
import com.tc.io.serializer.TCObjectOutputStream;
import com.tc.net.groups.NodeIDSerializer;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.util.UUID;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

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

    NodeID n5 = new ClientID(new ChannelID(1000));
    NodeID n6 = dupBySerialization(n5);
    assertTrue(n5.equals(n6));
    assertTrue(n6 instanceof ClientID);
  }

  private NodeID dupBySerialization(NodeID orig) throws Exception {
    ByteArrayOutputStream bout = new ByteArrayOutputStream();
    TCObjectOutputStream out = new TCObjectOutputStream(bout);
    NodeIDSerializer.writeNodeID(orig, out);
    out.flush();

    ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
    TCObjectInputStream in = new TCObjectInputStream(bin);
    NodeID dup = NodeIDSerializer.readNodeID(in);
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

    NodeID n5 = new ClientID(new ChannelID(1000));
    NodeID n6 = dupByTCSerializable(n5);
    assertTrue(n5.equals(n6));
    assertTrue(n6 instanceof ClientID);
  }

  private NodeID dupByTCSerializable(NodeID orig) throws Exception {
    NodeIDSerializer serializer = new NodeIDSerializer(orig);
    TCByteBufferOutputStream out = new TCByteBufferOutputStream();
    serializer.serializeTo(out);
    out.flush();

    TCByteBufferInputStream in = new TCByteBufferInputStream(out.toArray());
    serializer = new NodeIDSerializer();
    NodeID dup = ((NodeIDSerializer) serializer.deserializeFrom(in)).getNodeID();
    return dup;
  }

}
