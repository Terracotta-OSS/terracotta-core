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
public class RegisterCacheListenerMessageImpl extends DSOMessageBase implements RegisterCacheListenerMessage {

  public RegisterCacheListenerMessageImpl(final SessionID sessionID, final MessageMonitor monitor,
                                          final TCByteBufferOutputStream out, final MessageChannel channel,
                                          final TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public RegisterCacheListenerMessageImpl(final SessionID sessionID, final MessageMonitor monitor,
                                          final MessageChannel channel, final TCMessageHeader header,
                                          final TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  @Override
  protected void dehydrateValues() {
  }

  @Override
  protected boolean hydrateValue(byte name) throws IOException {
    return true;
  }

}
