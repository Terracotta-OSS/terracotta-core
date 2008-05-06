/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.msg;

import com.tc.async.api.EventContext;
import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageImpl;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.session.SessionID;

/**
 * Base class for DSO network messages
 */
public class DSOMessageBase extends TCMessageImpl implements EventContext {

  private final SessionID localSessionID;

  public DSOMessageBase(SessionID sessionID, MessageMonitor monitor, TCByteBufferOutputStream out, MessageChannel channel, TCMessageType type) {
    super(monitor, out, channel, type);
    this.localSessionID = sessionID;
  }

  public DSOMessageBase(SessionID sessionID, MessageMonitor monitor, MessageChannel channel, TCMessageHeader header,
                        TCByteBuffer[] data) {
    super(monitor, channel, header, data);
    this.localSessionID = sessionID;
  }

  public SessionID getLocalSessionID() {
    return localSessionID;
  }

}
