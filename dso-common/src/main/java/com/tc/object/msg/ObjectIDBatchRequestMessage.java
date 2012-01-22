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

import java.io.IOException;

public class ObjectIDBatchRequestMessage extends DSOMessageBase implements ObjectIDBatchRequest {
  private final static byte BATCH_SIZE = 1;

  private int               batchSize;

  public ObjectIDBatchRequestMessage(SessionID sessionID, MessageMonitor monitor, TCByteBufferOutputStream out,
                                     MessageChannel channel, TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public ObjectIDBatchRequestMessage(SessionID sessionID, MessageMonitor monitor, MessageChannel channel,
                                     TCMessageHeader header, TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  protected void dehydrateValues() {
    putNVPair(BATCH_SIZE, batchSize);
  }

  protected boolean hydrateValue(byte name) throws IOException {
    switch (name) {
      case BATCH_SIZE:
        batchSize = getIntValue();
        return true;
      default:
        return false;
    }
  }

  public void initialize(int size) {
    this.batchSize = size;
  }

  public int getBatchSize() {
    return batchSize;
  }

  public NodeID getRequestingNodeID() {
    return getSourceNodeID();
  }
}
