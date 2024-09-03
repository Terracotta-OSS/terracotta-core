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
package com.tc.net.protocol.transport;

import com.tc.net.core.TCConnection;
import com.tc.net.protocol.IllegalReconnectException;
import com.tc.net.protocol.NetworkLayer;
import com.tc.net.protocol.TCNetworkMessage;
import com.tc.text.PrettyPrintable;
import java.io.IOException;

import java.util.List;

/**
 * Interface for message transport layer-- the connection-side endcap to the message communications stack.
 */
public interface MessageTransport extends NetworkLayer, PrettyPrintable {

  public static final int CONNWEIGHT_TX_HANDSHAKED = 1;
  
  public void addTransportListener(MessageTransportListener listener);

  public void addTransportListeners(List<MessageTransportListener> transportListeners);

  public void removeTransportListeners();

  public void attachNewConnection(TCConnection connection) throws IllegalReconnectException;

  public void receiveTransportMessage(WireProtocolMessage message);

  public void sendToConnection(TCNetworkMessage message) throws IOException;

  public short getCommunicationStackFlags(NetworkLayer parentLayer);

  public String getCommunicationStackNames(NetworkLayer parentLayer);

  public void initConnectionID(ConnectionID cid);
}
