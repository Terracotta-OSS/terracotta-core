/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.retrieval.actions;

import com.tc.logging.NullTCLogger;
import com.tc.net.NodeID;
import com.tc.net.ServerID;
import com.tc.object.ObjectID;
import com.tc.objectserver.l1.api.ClientStateManager;
import com.tc.objectserver.l1.impl.ClientStateManagerImpl;
import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticType;
import com.tc.util.Assert;

import junit.framework.TestCase;

public class SRAL1ReferenceCountTest extends TestCase {

  public void testRetrieval() {
    ClientStateManager manager = new ClientStateManagerImpl(new NullTCLogger());

    SRAL1ReferenceCount sra = new SRAL1ReferenceCount(manager);
    Assert.assertEquals(StatisticType.SNAPSHOT, sra.getType());

    StatisticData[] data1 = sra.retrieveStatisticData();
    assertNotNull(data1);
    assertEquals(0, data1.length);

    byte[] b1 = new byte[] { 34, 55, 2 , (byte) 255, 0 };
    byte[] b2 = new byte[] { 34, 55, 2 , (byte) 255, 0, 4 };
    byte[] b3 = new byte[] { 43, 5, 127 , (byte) 255, -87, 9 };
    byte[] b4 = new byte[] { 4};

    NodeID n1 = new ServerID("node1", b1);
    NodeID n2 = new ServerID("node2", b2);
    NodeID n3 = new ServerID("node3", b3);
    NodeID n4 = new ServerID("node4", b4);

    manager.startupNode(n1);
    manager.startupNode(n2);
    manager.startupNode(n3);
    manager.startupNode(n4);

    ObjectID o1 = new ObjectID(1L);
    ObjectID o2 = new ObjectID(2L);
    ObjectID o3 = new ObjectID(3L);
    ObjectID o4 = new ObjectID(4L);
    ObjectID o5 = new ObjectID(5L);
    ObjectID o6 = new ObjectID(6L);

    manager.addReference(n1, o1);
    manager.addReference(n1, o2);
    manager.addReference(n2, o3);
    manager.addReference(n2, o4);
    manager.addReference(n2, o5);
    manager.addReference(n3, o6);

    StatisticData[] data2 = sra.retrieveStatisticData();
    assertNotNull(data2);
    assertEquals(4, data2.length);
    boolean[] nodes_with_data = new boolean[] {false, false, false, false};
    for (StatisticData data : data2) {
      assertEquals(SRAL1ReferenceCount.ACTION_NAME, data.getName());
      if (n1.toString().equals(data.getElement())) {
        nodes_with_data[0] = true;
        assertEquals(new Long(2), data.getData());
      } else if (n2.toString().equals(data.getElement())) {
        nodes_with_data[1] = true;
        assertEquals(new Long(3), data.getData());
      } else if (n3.toString().equals(data.getElement())) {
        nodes_with_data[2] = true;
        assertEquals(new Long(1), data.getData());
      } else if (n4.toString().equals(data.getElement())) {
        nodes_with_data[3] = true;
        assertEquals(new Long(0), data.getData());
      }
    }

    for (boolean node : nodes_with_data) {
      assertTrue(node);
    }
  }
}