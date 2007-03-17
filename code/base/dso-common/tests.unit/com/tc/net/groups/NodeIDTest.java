/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.groups;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.HashSet;

import junit.framework.TestCase;

public class NodeIDTest extends TestCase {
  
  public void test() throws Exception {
    byte[] b1 = new byte[] { 34, 55, 2 , (byte) 255, 0 };
    byte[] b2 = new byte[] { 34, 55, 2 , (byte) 255, 0, 4 };
    byte[] b3 = new byte[] { 43, 5, 127 , (byte) 255, -87, 9 };
    byte[] b4 = new byte[] { 4};
    
    NodeID n1 = new NodeID("node1", b1);
    NodeID n2 = new NodeID("node2", b2);
    NodeID n3 = new NodeID("node3", b3);
    NodeID n4 = new NodeID("node4", b4);
    
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
    oo.writeObject(NodeID.NULL_ID);
    System.err.println("Written : " + NodeID.NULL_ID);
    
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
    assertEquals(NodeID.NULL_ID, r5);


  }
}
