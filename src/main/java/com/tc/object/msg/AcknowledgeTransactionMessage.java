/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.TCMessage;
import com.tc.object.tx.TransactionID;

public interface AcknowledgeTransactionMessage extends TCMessage {

  public void initialize(NodeID channelID, TransactionID txID);

  public NodeID getRequesterID();

  public TransactionID getRequestID();

}
