/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.msg;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferOutput;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.session.SessionID;

import java.io.IOException;

public class ClientHandshakeAckMessageImpl extends DSOMessageBase implements ClientHandshakeAckMessage {

  private static final byte OBJECT_ID_START_SEQUENCE = 1;
  private static final byte OBJECT_ID_END_SEQUENCE   = 2;
  private static final byte PERSISTENT_SERVER        = 3;

  private long              oidStart;
  private long              oidEnd;
  private boolean           persistentServer;

  public ClientHandshakeAckMessageImpl(MessageMonitor monitor, TCByteBufferOutput out, MessageChannel channel,
                                       TCMessageType type) {
    super(monitor, out, channel, type);
  }

  public ClientHandshakeAckMessageImpl(SessionID sessionID, MessageMonitor monitor, MessageChannel channel,
                                       TCMessageHeader header, TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  protected void dehydrateValues() {
    putNVPair(OBJECT_ID_START_SEQUENCE, oidStart);
    putNVPair(OBJECT_ID_END_SEQUENCE, oidEnd);
    putNVPair(PERSISTENT_SERVER, persistentServer);
  }

  protected boolean hydrateValue(byte name) throws IOException {
    switch (name) {
      case OBJECT_ID_START_SEQUENCE:
        oidStart = getLongValue();
        return true;
      case OBJECT_ID_END_SEQUENCE:
        oidEnd = getLongValue();
        return true;
      case PERSISTENT_SERVER:
        persistentServer = getBooleanValue();
        return true;
      default:
        return false;
    }
  }

  public void initialize(long start, long end, boolean persistent) {
    this.oidStart = start;
    this.oidEnd = end;
    this.persistentServer = persistent;
  }

  public long getObjectIDSequenceStart() {
    return oidStart;
  }

  public long getObjectIDSequenceEnd() {
    return oidEnd;
  }

  public boolean getPersistentServer() {
    return persistentServer;
  }

}
