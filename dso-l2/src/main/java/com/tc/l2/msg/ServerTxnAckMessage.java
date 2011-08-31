/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.l2.msg;

import com.tc.net.NodeID;
import com.tc.net.groups.AbstractGroupMessage;
import com.tc.net.groups.MessageID;

import java.util.Set;

public abstract class ServerTxnAckMessage extends AbstractGroupMessage {

  protected ServerTxnAckMessage(int type) {
    super(type);
  }

  public ServerTxnAckMessage(int type, MessageID requestID) {
    super(type, requestID);
  }

  public abstract Set getAckedServerTxnIDs();

  public abstract NodeID getDestinationID();

}
