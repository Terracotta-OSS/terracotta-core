/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.tcm;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.object.session.SessionID;

public interface GeneratedMessageFactory {

  TCMessage createMessage(SessionID sid, MessageMonitor monitor, TCByteBufferOutputStream output,
                          MessageChannel channel, TCMessageType type);

  TCMessage createMessage(SessionID sid, MessageMonitor monitor, MessageChannel channel, TCMessageHeader msgHeader,
                          TCByteBuffer[] data);

}
