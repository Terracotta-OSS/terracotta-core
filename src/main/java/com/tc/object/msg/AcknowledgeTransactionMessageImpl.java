/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.session.SessionID;
import com.tc.object.tx.TransactionID;

import java.io.IOException;

/**
 * @author steve
 */
public class AcknowledgeTransactionMessageImpl extends DSOMessageBase implements AcknowledgeTransactionMessage {
  private final static byte REQUEST_ID   = 1;
  private final static byte REQUESTER_ID = 2;

  private TransactionID     requestID;
  private NodeID            requesterID;

  public AcknowledgeTransactionMessageImpl(SessionID sessionID, MessageMonitor monitor, TCByteBufferOutputStream out,
                                           MessageChannel channel, TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public AcknowledgeTransactionMessageImpl(SessionID sessionID, MessageMonitor monitor, MessageChannel channel,
                                           TCMessageHeader header, TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  @Override
  protected void dehydrateValues() {
    putNVPair(REQUEST_ID, requestID.toLong());
    putNVPair(REQUESTER_ID, requesterID);
  }

  @Override
  protected boolean hydrateValue(byte name) throws IOException {
    switch (name) {
      case REQUESTER_ID:
        requesterID = getNodeIDValue();
        return true;
      case REQUEST_ID:
        requestID = new TransactionID(getLongValue());
        return true;
      default:
        return false;
    }
  }

  @Override
  public void initialize(NodeID nid, TransactionID txID) {
    this.requesterID = nid;
    this.requestID = txID;
  }

  @Override
  public NodeID getRequesterID() {
    return requesterID;
  }

  @Override
  public TransactionID getRequestID() {
    return requestID;
  }

}
