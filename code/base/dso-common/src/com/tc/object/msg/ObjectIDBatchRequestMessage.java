/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.msg;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferOutput;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.session.SessionID;

import java.io.IOException;

/**
 * @author steve
 */
public class ObjectIDBatchRequestMessage extends DSOMessageBase {
  private final static byte REQUEST_ID = 1;
  private final static byte BATCH_SIZE = 2;

  private long              requestID;
  private int               batchSize;

  public ObjectIDBatchRequestMessage(MessageMonitor monitor, TCByteBufferOutput out, MessageChannel channel, TCMessageType type) {
    super(monitor, out, channel, type);
  }

  public ObjectIDBatchRequestMessage(SessionID sessionID, MessageMonitor monitor, MessageChannel channel, TCMessageHeader header, TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  protected void dehydrateValues() {
    putNVPair(REQUEST_ID, requestID);
    putNVPair(BATCH_SIZE, batchSize);
  }

  protected boolean hydrateValue(byte name) throws IOException {
    switch (name) {
      case REQUEST_ID:
        requestID = getLongValue();
        return true;
      case BATCH_SIZE:
        batchSize = getIntValue();
        return true;
      default:
        return false;
    }
  }

  public void initialize(long reqID, int size) {
    this.requestID = reqID;
    this.batchSize = size;
  }

  public int getBatchSize() {
    return batchSize;
  }

  public long getRequestID() {
    return requestID;
  }
}