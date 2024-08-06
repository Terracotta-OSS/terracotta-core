/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
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
package com.tc.entity;

import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.ClientInstanceID;
import com.tc.object.msg.DSOMessageBase;
import com.tc.object.session.SessionID;

import java.io.IOException;

/**
 * @author twu
 */
public class ServerEntityMessageImpl extends DSOMessageBase implements ServerEntityMessage {
  private static final byte ENTITY_DESCRIPTOR = 0;
  private static final byte MESSAGE = 1;
  private static final byte RESPONSE_ID = 2;

  private byte[] message;
  private ClientInstanceID clientInstance;
  private Long responseId;

  public ServerEntityMessageImpl(SessionID sessionID, MessageMonitor monitor, TCByteBufferOutputStream out, MessageChannel channel, TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public ServerEntityMessageImpl(SessionID sessionID, MessageMonitor monitor, MessageChannel channel, TCMessageHeader header,TCByteBufferInputStream data) {
    super(sessionID, monitor, channel, header, data);
  }

  @Override
  public void setMessage(ClientInstanceID clientInstance, byte[] message) {
    this.clientInstance = clientInstance;
    this.message = message;
  }

  @Override
  public void setMessage(ClientInstanceID clientInstance, byte[] payload, long responseId) {
    this.clientInstance = clientInstance;
    this.message = payload;
    this.responseId = responseId;
  }

  @Override
  public Long getResponseId() {
    return responseId;
  }

  @Override
  public ClientInstanceID getClientInstanceID() {
    return this.clientInstance;
  }

  @Override
  public byte[] getMessage() {
    return message;
  }

  @Override
  protected void dehydrateValues() {
    putNVPair(ENTITY_DESCRIPTOR, this.clientInstance);
    if (responseId != null) {
      putNVPair(RESPONSE_ID, responseId);
    }
    putNVPair(MESSAGE, message.length);
    getOutputStream().write(message);
  }

  @Override
  protected boolean hydrateValue(byte name) throws IOException {
    boolean didMatch = false;
    switch (name) {
      case ENTITY_DESCRIPTOR:
        this.clientInstance = ClientInstanceID.readFrom(getInputStream());
        didMatch = true;
        break;
      case MESSAGE:
        message = getBytesArray();
        didMatch = true;
        break;
      case RESPONSE_ID:
        responseId = getLongValue();
        didMatch = true;
        break;
      default:
        // This must be malformed data so fail.
        didMatch = false;
    }
    return didMatch;
  }
}
