/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.msg;

import com.tc.async.api.EventContext;
import com.tc.net.groups.AbstractGroupMessage;
import com.tc.net.groups.MessageID;
import com.tc.net.groups.NodeID;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;
import com.tc.util.Assert;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class ServerTxnAckMessage extends AbstractGroupMessage implements EventContext {

  public static final int   SERVER_TXN_ACK_MSG_TYPE = 0;

  private Set               serverTxnIDs;

  private transient NodeID nodeID;

  // To make serialization happy
  public ServerTxnAckMessage() {
    super(-1);
  }

  public ServerTxnAckMessage(NodeID nodeID, MessageID messageID, Set serverTxnIDs) {
    super(SERVER_TXN_ACK_MSG_TYPE, messageID);
    this.nodeID = nodeID;
    this.serverTxnIDs = serverTxnIDs;
  }

  public Set getAckedServerTxnIDs() {
    return serverTxnIDs;
  }
  
  public NodeID getDestinationID() {
    Assert.assertNotNull(nodeID);
    return nodeID;
  }
  
  protected void basicReadExternal(int msgType, ObjectInput in) throws IOException {
    Assert.assertEquals(SERVER_TXN_ACK_MSG_TYPE, msgType);
    int size = in.readInt();
    serverTxnIDs = new HashSet(size);
    for (int i = 0; i < size; i++) {
      long cid = in.readLong();
      long clientTxID = in.readLong();
      serverTxnIDs.add(new ServerTransactionID(new ChannelID(cid), new TransactionID(clientTxID)));
    }
  }

  protected void basicWriteExternal(int msgType, ObjectOutput out) throws IOException {
    Assert.assertEquals(SERVER_TXN_ACK_MSG_TYPE, msgType);
    out.writeInt(serverTxnIDs.size());
    for (Iterator i = serverTxnIDs.iterator(); i.hasNext();) {
      ServerTransactionID sTxID = (ServerTransactionID) i.next();
      out.writeLong(sTxID.getChannelID().toLong());
      out.writeLong(sTxID.getClientTransactionID().toLong());
    }
  }

}
