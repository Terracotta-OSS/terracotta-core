/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.object.msg;

import com.tc.io.TCByteBufferInputStream;
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
                                           TCByteBufferInputStream data) {
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
  public String getRefusalsCause() {
    return this.refusalCause;
  }

  @Override
  public void initialize(String message) {
    this.refusalCause = message;
  }

}
