/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net;

import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.groups.NodeIDSerializer;
import com.tc.util.UUID;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import junit.framework.TestCase;

public class ServerIDTest extends TestCase {

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

    HashSet set = new HashSet();
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
    System.err.println("Written : " + n1);
    serializer = new NodeIDSerializer(n2);
    serializer.serializeTo(bo);
    System.err.println("Written : " + n2);
    serializer = new NodeIDSerializer(n3);
    serializer.serializeTo(bo);
    System.err.println("Written : " + n3);
    serializer = new NodeIDSerializer(n4);
    serializer.serializeTo(bo);
    System.err.println("Written : " + n4);
    serializer = new NodeIDSerializer(ServerID.NULL_ID);
    serializer.serializeTo(bo);
    System.err.println("Written : " + ServerID.NULL_ID);

    TCByteBufferInputStream bi = new TCByteBufferInputStream(bo.toArray());
    serializer = new NodeIDSerializer();
    serializer.deserializeFrom(bi);
    NodeID r1 = serializer.getNodeID();
    System.err.println("Read : " + r1);
    assertEquals(n1, r1);
    serializer = new NodeIDSerializer();
    serializer.deserializeFrom(bi);
    NodeID r2 = serializer.getNodeID();
    System.err.println("Read : " + r2);
    assertEquals(n2, r2);
    serializer = new NodeIDSerializer();
    serializer.deserializeFrom(bi);
    NodeID r3 = serializer.getNodeID();
    System.err.println("Read : " + r3);
    assertEquals(n3, r3);
    serializer = new NodeIDSerializer();
    serializer.deserializeFrom(bi);
    NodeID r4 = serializer.getNodeID();
    System.err.println("Read : " + r4);
    assertEquals(n4, r4);
    serializer = new NodeIDSerializer();
    serializer.deserializeFrom(bi);
    NodeID r5 = serializer.getNodeID();
    System.err.println("Read : " + r5);
    assertEquals(ServerID.NULL_ID, r5);
  }

  private ServerID makeNodeID(String name) {
    return (new ServerID(name, UUID.getUUID().toString().getBytes()));
  }

  public void testSortOrder() throws Exception {

    NodeID n1 = makeNodeID("node1");
    NodeID n2 = makeNodeID("node2");

    assertTrue(n1.compareTo(n2) != 0);
    assertTrue(n1.compareTo(new ClientID(0)) != 0);

    List all = new ArrayList();
    TreeSet ss = new TreeSet();
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

  private void assertIsSorted(Collection nodeIDs) {
    ServerID last = null;
    for (Iterator i = nodeIDs.iterator(); i.hasNext();) {
      ServerID next = (ServerID) i.next();
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
