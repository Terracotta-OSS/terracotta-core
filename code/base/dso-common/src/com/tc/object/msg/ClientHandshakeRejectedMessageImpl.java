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

public class ClientHandshakeRejectedMessageImpl extends DSOMessageBase implements ClientHandshakeRefusedMessage {
  private static final byte REJECTION_CAUSE = 1;
  private String            rejcetionCause;

  public ClientHandshakeRejectedMessageImpl(final SessionID sessionID, final MessageMonitor monitor,
                                            final TCByteBufferOutputStream out, final MessageChannel channel,
                                            final TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public ClientHandshakeRejectedMessageImpl(final SessionID sessionID, final MessageMonitor monitor,
                                            final MessageChannel channel, final TCMessageHeader header,
                                            final TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  @Override
  protected void dehydrateValues() {
    putNVPair(REJECTION_CAUSE, this.rejcetionCause);
  }

  @Override
  protected boolean hydrateValue(byte name) throws IOException {
    switch (name) {
      case REJECTION_CAUSE:
        this.rejcetionCause = getStringValue();
        return true;

      default:
        return false;
    }
  }

  public String getRefualsCause() {
    return this.rejcetionCause;
  }

  public void initialize(String message) {
    this.rejcetionCause = message;
  }

}
