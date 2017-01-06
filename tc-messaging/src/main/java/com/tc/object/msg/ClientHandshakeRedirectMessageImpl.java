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
package com.tc.object.msg;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.session.SessionID;

import java.io.IOException;

public class ClientHandshakeRedirectMessageImpl extends DSOMessageBase implements ClientHandshakeRedirectMessage {
  private static final byte REDIRECT_TO = 1;
  private static final byte REDIRECT_PORT = 2;
  private String            server;
  private int               port;

  public ClientHandshakeRedirectMessageImpl(SessionID sessionID, MessageMonitor monitor,
                                           TCByteBufferOutputStream out, MessageChannel channel,
                                           TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public ClientHandshakeRedirectMessageImpl(SessionID sessionID, MessageMonitor monitor,
                                           MessageChannel channel, TCMessageHeader header,
                                           TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  @Override
  protected void dehydrateValues() {
    putNVPair(REDIRECT_TO, this.server);
    putNVPair(REDIRECT_PORT, this.port);
  }

  @Override
  protected boolean hydrateValue(byte name) throws IOException {
    switch (name) {
      case REDIRECT_TO:
        this.server = getStringValue();
        return true;
      case REDIRECT_PORT:
        this.port = getIntValue();
        return true;
      default:
        return false;
    }
  }

  @Override
  public String getActiveHost() {
    return this.server;
  }

  @Override
  public int getActivePort() {
    return this.port;
  }

  @Override
  public void initialize(String message, int port) {
    this.server = message;
    this.port = port;
  }

}
