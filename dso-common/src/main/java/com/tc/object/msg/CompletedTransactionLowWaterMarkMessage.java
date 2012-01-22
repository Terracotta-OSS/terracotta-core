/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.async.api.EventContext;
import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.session.SessionID;
import com.tc.object.tx.TransactionID;

import java.io.IOException;

public class CompletedTransactionLowWaterMarkMessage extends DSOMessageBase implements EventContext {

  private static final byte LOW_WATER_MARK = 0;

  private TransactionID     lowWaterMark;

  public CompletedTransactionLowWaterMarkMessage(SessionID sessionID, MessageMonitor monitor, TCByteBufferOutputStream out,
                                                 MessageChannel channel, TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public CompletedTransactionLowWaterMarkMessage(SessionID sessionID, MessageMonitor monitor, MessageChannel channel,
                                                 TCMessageHeader header, TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  public void initialize(TransactionID lwm) {
    this.lowWaterMark = lwm;
  }

  protected void dehydrateValues() {
    putNVPair(LOW_WATER_MARK, lowWaterMark.toLong());
  }

  protected boolean hydrateValue(byte name) throws IOException {
    switch (name) {
      case LOW_WATER_MARK:
        this.lowWaterMark = new TransactionID(getLongValue());
        return true;
      default:
        return false;
    }
  }

  public TransactionID getLowWaterMark() {
    return lowWaterMark;
  }

}
