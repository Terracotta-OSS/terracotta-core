/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
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

  @SuppressWarnings("resource")
  public PingMessage(MessageMonitor monitor) {
    this( new SessionID(0), monitor, new TCByteBufferOutputStream(), null, TCMessageType.PING_MESSAGE);
  }

  public void initialize(long seq) {
    this.sequence = seq;
  }

  public PingMessage createResponse() {
    PingMessage rv = (PingMessage) getChannel().createMessage(TCMessageType.PING_MESSAGE);
    rv.sequence = getSequence();
    return rv;
  }

  @Override
  protected void dehydrateValues() {
    putNVPair(SEQUENCE, sequence);
  }

  @Override
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
