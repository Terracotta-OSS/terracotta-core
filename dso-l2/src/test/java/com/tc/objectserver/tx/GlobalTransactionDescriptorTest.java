/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.net.ClientID;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.gtx.GlobalTransactionDescriptor;
import com.tc.test.TCTestCase;

import java.util.Iterator;
import java.util.TreeMap;

public class GlobalTransactionDescriptorTest extends TCTestCase {

  public void tests() throws Exception {
    ClientID cid = new ClientID(1);
    TransactionID tx1 = new TransactionID(1);
    ServerTransactionID stx1 = new ServerTransactionID(cid, tx1);

    GlobalTransactionDescriptor d1 = new GlobalTransactionDescriptor(stx1, new GlobalTransactionID(1));
    GlobalTransactionDescriptor d2 = new GlobalTransactionDescriptor(new ServerTransactionID(cid, tx1),
                                                                     new GlobalTransactionID(1));
    GlobalTransactionDescriptor x1 = new GlobalTransactionDescriptor(new ServerTransactionID(new ClientID(4), tx1),
                                                                     new GlobalTransactionID(1));
    GlobalTransactionDescriptor x2 = new GlobalTransactionDescriptor(new ServerTransactionID(cid, tx1),
                                                                     new GlobalTransactionID(2));

    assertEquals(d1, d2);
    assertEquals(d1.hashCode(), d2.hashCode());
    assertFalse(d1.equals(new Object()));
    assertFalse(d1.equals(null));
    assertFalse(d1.equals(x1));
    assertFalse(d1.equals(x2));
  }

  public void testGIDIsSorted() throws Exception {
    TreeMap tm = new TreeMap();
    tm.put(new GlobalTransactionID(9), new Object());
    tm.put(new GlobalTransactionID(19), new Object());
    tm.put(new GlobalTransactionID(1), new Object());
    tm.put(new GlobalTransactionID(Long.MAX_VALUE), new Object());
    tm.put(new GlobalTransactionID(Long.MIN_VALUE), new Object());
    tm.put(new GlobalTransactionID(-256), new Object());
    tm.put(new GlobalTransactionID(1), new Object());
    tm.put(new GlobalTransactionID(70003), new Object());

    long min = Long.MIN_VALUE;
    for (Iterator i = tm.keySet().iterator(); i.hasNext();) {
      GlobalTransactionID gid = (GlobalTransactionID) i.next();
      assertTrue(min <= gid.toLong());
      min = gid.toLong();
    }
  }
}
