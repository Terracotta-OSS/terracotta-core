/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.net;

import com.tc.bytes.TCReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.groups.NodeIDSerializer;
import com.tc.util.UUID;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ServerIDTest {

  @SuppressWarnings("resource")
  @Test
  public void test() throws Exception {

    NodeID n1 = makeNodeID("node1");
    NodeID n2 = makeNodeID("node2");
    NodeID n3 = makeNodeID("node3");
    NodeID n4 = makeNodeID("node4");

    assertFalse(n1.equals(n2));
    assertTrue(n1.equals(n1));
    assertFalse(n1.equals(n3));
    assertTrue(n3.equals(n3));
    assertFalse(n1.equals(n4));
    assertTrue(n4.equals(n4));
    assertFalse(n3.equals(n4));
    assertTrue(n2.equals(n2));

    Set<NodeID> set = new HashSet<NodeID>();
    assertTrue(set.add(n1));
    assertTrue(set.add(n2));
    assertTrue(set.add(n3));
    assertTrue(set.add(n4));
    assertFalse(set.add(n1));
    assertFalse(set.add(n2));
    assertFalse(set.add(n3));
    assertFalse(set.add(n4));

    TCByteBufferOutputStream bo = new TCByteBufferOutputStream();
    NodeIDSerializer serializer = new NodeIDSerializer(n1);
    serializer.serializeTo(bo);
    serializer = new NodeIDSerializer(n2);
    serializer.serializeTo(bo);
    serializer = new NodeIDSerializer(n3);
    serializer.serializeTo(bo);
    serializer = new NodeIDSerializer(n4);
    serializer.serializeTo(bo);
    serializer = new NodeIDSerializer(ServerID.NULL_ID);
    serializer.serializeTo(bo);
    bo.close();
    
    try (TCReference ref = bo.accessBuffers(); TCByteBufferInputStream bi = new TCByteBufferInputStream(ref)) {
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
      assertEquals(ServerID.NULL_ID, r5);
    }
  }

  private ServerID makeNodeID(String name) {
    return (new ServerID(name, UUID.getUUID().toString().getBytes()));
  }

  @Test
  public void testSortOrder() throws Exception {

    NodeID n1 = makeNodeID("node1");
    NodeID n2 = makeNodeID("node2");

    assertTrue(n1.compareTo(n2) != 0);
    assertTrue(n1.compareTo(new ClientID(0)) != 0);

    List<NodeID> all = new ArrayList<NodeID>();
    SortedSet<NodeID> ss = new TreeSet<NodeID>();
    all.add(n1);
    all.add(n2);
    ss.add(n1);
    ss.add(n2);
    for (int i = 0; i < 100; i++) {
      ServerID n = makeNodeID("node-" + i);
      all.add(n);
      ss.add(n);
    }
    assertIsSorted(ss);
    Collections.sort(all);
    assertIsSorted(all);
  }

  private void assertIsSorted(Collection<NodeID> nodeIDs) {
    ServerID last = null;
    for (NodeID nid : nodeIDs) {
      ServerID next = (ServerID) nid;
      if (last == null) {
        last = next;
      } else {
        assertTrue(last.compareTo(next) <= 0);
        assertIsNotGreater(last, next);
      }
    }
  }

  private void assertIsNotGreater(ServerID last, ServerID next) {
    byte[] luid = last.getUID();
    byte[] nuid = next.getUID();
    assertTrue(luid.length <= nuid.length);
    if (luid.length == nuid.length) {
      for (int i = 0; i < luid.length; i++) {
        if (luid[i] != nuid[i]) {
          assertTrue(luid[i] <= nuid[i]);
          break;
        }
      }
    }
  }
}
