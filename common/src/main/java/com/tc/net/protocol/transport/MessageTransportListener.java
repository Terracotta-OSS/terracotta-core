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
package com.tc.net.protocol.transport;

/**
 * Listener for MessageTransport events
 */
public interface MessageTransportListener {

  /**
   * Called when the transport's connection is established.
   */
  public void notifyTransportConnected(MessageTransport transport);

  /**
   * Called when the transport's connection goes away
   * 
   * @param forcedDisconnect - when a node disconnects its transport on its own (say, when HC detects it as dead)
   */
  public void notifyTransportDisconnected(MessageTransport transport, boolean forcedDisconnect);

  /**
   * Called when the transport tries to connect.
   */
  public void notifyTransportConnectAttempt(MessageTransport transport);

  /**
   * Called when the transport is closed.
   */
  public void notifyTransportClosed(MessageTransport transport);

  /**
   * Called when reconnection rejected by L2 and no more trying to reconnect.
   */
  public void notifyTransportReconnectionRejected(MessageTransport transport);
}
