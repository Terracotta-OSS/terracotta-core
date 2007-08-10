/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.gtx.GlobalTransactionDescriptor;
import com.tc.test.TCTestCase;

public class GlobalTransactionDescriptorTest extends TCTestCase {
  
  public void tests() throws Exception {
    ChannelID channel1 = new ChannelID(1);
    TransactionID tx1 = new TransactionID(1);
    ServerTransactionID stx1 = new ServerTransactionID(channel1, tx1);
    
    GlobalTransactionDescriptor d1 = new GlobalTransactionDescriptor(stx1, new GlobalTransactionID(1));
    GlobalTransactionDescriptor d2 = new GlobalTransactionDescriptor(new ServerTransactionID(channel1, tx1), new GlobalTransactionID(1));
    GlobalTransactionDescriptor x1 = new GlobalTransactionDescriptor(new ServerTransactionID(new ChannelID(4), tx1), new GlobalTransactionID(1));
    GlobalTransactionDescriptor x2 = new GlobalTransactionDescriptor(new ServerTransactionID(channel1, tx1), new GlobalTransactionID(2));
    
    assertEquals(d1, d2);
    assertEquals(d1.hashCode(), d2.hashCode());
    assertFalse(d1.equals(new Object()));
    assertFalse(d1.equals(null));
    assertFalse(d1.equals(x1));
    assertFalse(d1.equals(x2));
  }
}
