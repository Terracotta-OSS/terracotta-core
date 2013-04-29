package com.tc.object.msg;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.session.SessionID;

import java.io.IOException;

/**
 * @author Eugene Shelestovich
 */
public class UnregisterServerEventListenerMessage extends DSOMessageBase {

  private String destination;

  public UnregisterServerEventListenerMessage(final SessionID sessionID, final MessageMonitor monitor,
                                              final TCByteBufferOutputStream out, final MessageChannel channel,
                                              final TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public UnregisterServerEventListenerMessage(final SessionID sessionID, final MessageMonitor monitor,
                                              final MessageChannel channel, final TCMessageHeader header,
                                              final TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  protected void dehydrateValues() {
  }

  protected boolean hydrateValue(byte name) throws IOException {
    return true;
  }

  public String getDestination() {
    return destination;
  }

  public void setDestination(final String destination) {
    this.destination = destination;
  }
}
