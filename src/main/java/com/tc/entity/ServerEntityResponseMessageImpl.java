package com.tc.entity;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.msg.DSOMessageBase;
import com.tc.object.session.SessionID;

import java.io.IOException;

/**
 * @author twu
 */
public class ServerEntityResponseMessageImpl extends DSOMessageBase implements ServerEntityResponseMessage {
  private static final byte RESPONSE_ID = 0;

  private long responseId;

  public ServerEntityResponseMessageImpl(SessionID sessionID, MessageMonitor monitor, TCByteBufferOutputStream out, MessageChannel channel, TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public ServerEntityResponseMessageImpl(SessionID sessionID, MessageMonitor monitor, MessageChannel channel, TCMessageHeader header, TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  @Override
  public void setResponseId(long responseId) {
    this.responseId = responseId;
  }

  @Override
  public long getResponseId() {
    return responseId;
  }

  @Override
  protected void dehydrateValues() {
    putNVPair(RESPONSE_ID, responseId);
  }

  @Override
  protected boolean hydrateValue(byte name) throws IOException {
    if (name == RESPONSE_ID) {
      responseId = getLongValue();
      return true;
    } else {
      return false;
    }
  }
}
