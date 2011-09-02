/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
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