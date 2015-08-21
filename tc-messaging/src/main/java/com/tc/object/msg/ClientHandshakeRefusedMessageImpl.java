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

public class ClientHandshakeRefusedMessageImpl extends DSOMessageBase implements ClientHandshakeRefusedMessage {
  private static final byte REFUSAL_CAUSE = 1;
  private String            refusalCause;

  public ClientHandshakeRefusedMessageImpl(SessionID sessionID, MessageMonitor monitor,
                                           TCByteBufferOutputStream out, MessageChannel channel,
                                           TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public ClientHandshakeRefusedMessageImpl(SessionID sessionID, MessageMonitor monitor,
                                           MessageChannel channel, TCMessageHeader header,
                                           TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  @Override
  protected void dehydrateValues() {
    putNVPair(REFUSAL_CAUSE, this.refusalCause);
  }

  @Override
  protected boolean hydrateValue(byte name) throws IOException {
    switch (name) {
      case REFUSAL_CAUSE:
        this.refusalCause = getStringValue();
        return true;

      default:
        return false;
    }
  }

  @Override
  public String getRefualsCause() {
    return this.refusalCause;
  }

  @Override
  public void initialize(String message) {
    this.refusalCause = message;
  }

}
