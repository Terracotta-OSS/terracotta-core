/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
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

  public AcknowledgeTransactionMessageImpl(final SessionID sessionID, final MessageMonitor monitor, final TCByteBufferOutputStream out,
                                           final MessageChannel channel, final TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public AcknowledgeTransactionMessageImpl(final SessionID sessionID, final MessageMonitor monitor, final MessageChannel channel,
                                           final TCMessageHeader header, final TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  @Override
  protected void dehydrateValues() {
    putNVPair(REQUEST_ID, requestID.toLong());
    putNVPair(REQUESTER_ID, requesterID);
  }

  @Override
  protected boolean hydrateValue(final byte name) throws IOException {
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

  public void initialize(final NodeID nid, final TransactionID txID) {
    this.requesterID = nid;
    this.requestID = txID;
  }

  public NodeID getRequesterID() {
    return requesterID;
  }

  public TransactionID getRequestID() {
    return requestID;
  }

}