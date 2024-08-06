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
package com.tc.net.protocol.tcm;

import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.object.session.SessionID;

import static org.mockito.Mockito.mock;

public class TestTCMessage implements TCAction {

  public TCMessageType type = TCMessageType.PING_MESSAGE;

  public int getCorrelationId(boolean initialize) {
    return 0;
  }

  public void setCorrelationId(int id) {
    return;
  }

  @Override
  public TCMessageType getMessageType() {
    return type;
  }

  @Override
  public void hydrate() {
    return;
  }

  @Override
  public NetworkRecall send() {
    return mock(NetworkRecall.class);
  }

  @Override
  public MessageChannel getChannel() {
    return null;
  }

  @Override
  public int getMessageLength() {
    return 100;
  }

  public ClientID getClientID() {
    return null;
  }

  public boolean resend() {
    throw new UnsupportedOperationException();

  }

  @Override
  public SessionID getLocalSessionID() {
    throw new UnsupportedOperationException();
  }

  @Override
  public NodeID getSourceNodeID() {
    throw new UnsupportedOperationException();
  }

  @Override
  public NodeID getDestinationNodeID() {
    throw new UnsupportedOperationException();
  }
}
