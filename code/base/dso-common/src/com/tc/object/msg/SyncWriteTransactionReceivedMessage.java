/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.session.SessionID;

import java.io.IOException;

public class SyncWriteTransactionReceivedMessage extends DSOMessageBase {
  private final static byte BATCH_ID = 1;

  private long batchID;
  
  public SyncWriteTransactionReceivedMessage(SessionID sessionID, MessageMonitor monitor, TCByteBufferOutputStream out,
                                     MessageChannel channel, TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }
  
  public SyncWriteTransactionReceivedMessage(SessionID sessionID, MessageMonitor monitor, MessageChannel channel,
                                          TCMessageHeader header, TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }
  
  protected void dehydrateValues() {
    putNVPair(BATCH_ID, batchID);
  }

  protected boolean hydrateValue(byte name) throws IOException {
    switch (name) {
      case BATCH_ID:
        batchID = getLongValue();
        return true;
      default:
        return false;
    }
  }

  public void initialize(long batchId) {
    this.batchID = batchId;
  }

  public long getBatchID() {
    return batchID;
  }
}
