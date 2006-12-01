/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.tx;

import com.tc.net.protocol.tcm.ChannelID;
import com.tc.objectserver.gtx.GlobalTransactionDescriptor;
import com.tc.test.TCTestCase;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

public class GlobalTransactionIDTest extends TCTestCase {
  
  public void testSerialization() throws Exception {
    long gtxID = 1;
    ChannelID channel1 = new ChannelID(gtxID);
    TransactionID tx1 = new TransactionID(gtxID);
    ServerTransactionID stxID1 = new ServerTransactionID(channel1, tx1);
    GlobalTransactionDescriptor gtx = new GlobalTransactionDescriptor(stxID1);
    GlobalTransactionDescriptor gtx2 = serializeAndDeserialize(gtx);
    
    assertEquals(channel1, gtx2.getChannelID());
    assertEquals(tx1, gtx2.getClientTransactionID());
    assertEquals(gtx, gtx2);
  }

  private GlobalTransactionDescriptor serializeAndDeserialize(GlobalTransactionDescriptor gtx) throws Exception {
    PipedInputStream sink = new PipedInputStream();
    PipedOutputStream out = new PipedOutputStream(sink);
    ObjectOutputStream oos = new ObjectOutputStream(out);
    oos.writeObject(gtx);
    oos.flush();
    oos.close();
    out.flush();
    out.close();
    ObjectInputStream ois = new ObjectInputStream(sink);
    return (GlobalTransactionDescriptor) ois.readObject();
  }

  
}
