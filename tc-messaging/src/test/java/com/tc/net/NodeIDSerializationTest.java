/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.net;

import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.groups.NodeIDSerializer;
import com.tc.util.UUID;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class NodeIDSerializationTest {

  @Test
  public void testSerialization() throws Exception {
    NodeID n1 = new ServerID("node1", UUID.getUUID().toString().getBytes());
    NodeID n2 = dupBySerialization(n1);
    assertTrue(n1.equals(n2));
    assertTrue(n2 instanceof ServerID);

    NodeID n5 = new ClientID(1000);
    NodeID n6 = dupBySerialization(n5);
    assertTrue(n5.equals(n6));
    assertTrue(n6 instanceof ClientID);
    
    NodeID n7 = new StripeID(UUID.getUUID().toString());
    NodeID n8 = dupBySerialization(n7);
    assertTrue(n7.equals(n8));
    assertTrue(n8 instanceof StripeID);
  }

  @SuppressWarnings("resource")
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

  @Test
  public void testTCSerilaizable() throws Exception {
    NodeID n1 = new ServerID("node1", UUID.getUUID().toString().getBytes());
    NodeID n2 = dupByTCSerializable(n1);
    assertTrue(n1.equals(n2));
    assertTrue(n2 instanceof ServerID);
    
    NodeID n5 = new ClientID(1000);
    NodeID n6 = dupByTCSerializable(n5);
    assertTrue(n5.equals(n6));
    assertTrue(n6 instanceof ClientID);
    
    NodeID n7 = new StripeID(UUID.getUUID().toString());
    NodeID n8 = dupByTCSerializable(n7);
    assertTrue(n7.equals(n8));
    assertTrue(n8 instanceof StripeID);
  }

  @SuppressWarnings("resource")
  private NodeID dupByTCSerializable(NodeID orig) throws Exception {
    NodeIDSerializer serializer = new NodeIDSerializer(orig);
    TCByteBufferOutputStream out = new TCByteBufferOutputStream();
    serializer.serializeTo(out);
    out.close();

    TCByteBufferInputStream in = new TCByteBufferInputStream(out.toArray());
    serializer = new NodeIDSerializer();
    NodeID dup = serializer.deserializeFrom(in).getNodeID();
    return dup;
  }

}
