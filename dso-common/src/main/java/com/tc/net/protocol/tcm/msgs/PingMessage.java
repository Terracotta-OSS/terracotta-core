/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.protocol.tcm.msgs;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.msg.DSOMessageBase;
import com.tc.object.session.SessionID;
import com.tc.util.SequenceGenerator;

import java.io.IOException;

/**
 * A nice simple ping message. Mostly used for testing.
 *
 * @author teck
 */
public class PingMessage extends DSOMessageBase {
  private static final byte SEQUENCE = 1;

  private long              sequence = -1;

  public PingMessage(SessionID sessionID,MessageMonitor monitor, TCByteBufferOutputStream out, MessageChannel channel, TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public PingMessage(SessionID sessionID, MessageMonitor monitor, MessageChannel channel, TCMessageHeader header, TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  public PingMessage(MessageMonitor monitor) {
    this( new SessionID(0), monitor, new TCByteBufferOutputStream(), null, TCMessageType.PING_MESSAGE);
  }

  public void initialize(SequenceGenerator sg) {
    this.sequence = sg.getNextSequence();
  }

  public PingMessage createResponse() {
    PingMessage rv = (PingMessage) getChannel().createMessage(TCMessageType.PING_MESSAGE);
    rv.sequence = getSequence();
    return rv;
  }

  protected void dehydrateValues() {
    putNVPair(SEQUENCE, sequence);
  }

  protected boolean hydrateValue(byte name) throws IOException {
    switch (name) {
      case SEQUENCE:
        sequence = getLongValue();
        return true;
      default:
        return false;
    }
  }

  public long getSequence() {
    return this.sequence;
  }
}