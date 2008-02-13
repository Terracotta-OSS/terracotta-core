/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.groups;

import com.tc.util.UUID;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.HashSet;

import junit.framework.TestCase;

public class NodeIdComparableTest extends TestCase {
  
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
    
    
    ByteArrayOutputStream bo = new ByteArrayOutputStream();
    ObjectOutput oo = new ObjectOutputStream(bo);
    oo.writeObject(n1);
    System.err.println("Written : " + n1);
    oo.writeObject(n2);
    System.err.println("Written : " + n2);
    oo.writeObject(n3);
    System.err.println("Written : " + n3);
    oo.writeObject(n4);
    System.err.println("Written : " + n3);
    oo.writeObject(NodeIDImpl.NULL_ID);
    System.err.println("Written : " + NodeIDImpl.NULL_ID);
    
    ByteArrayInputStream bi = new ByteArrayInputStream(bo.toByteArray());
    ObjectInput oi = new ObjectInputStream(bi);
    NodeID r1 = (NodeID) oi.readObject();
    System.err.println("Read : " + r1);
    assertEquals(n1, r1);
    NodeID r2 = (NodeID) oi.readObject();
    System.err.println("Read : " + r2);
    assertEquals(n2, r2);
    NodeID r3 = (NodeID) oi.readObject();
    System.err.println("Read : " + r3);
    assertEquals(n3, r3);
    NodeID r4 = (NodeID) oi.readObject();
    System.err.println("Read : " + r4);
    assertEquals(n4, r4);
    NodeID r5 = (NodeID) oi.readObject();
    System.err.println("Read : " + r5);
    assertEquals(NodeIDImpl.NULL_ID, r5);

  }
  
  private NodeIdComparable makeNodeID(String name) {
    return (new NodeIdComparable(name, UUID.getUUID().toString().getBytes()));
  }

}
