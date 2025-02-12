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
 * Default Health Checker which is tied to the communications manager. All it does is, attaching a ECHO context to the
 * ESTABLISHED TC Connection
 * 
 * @author Manoj
 */
public class ConnectionHealthCheckerEchoImpl implements ConnectionHealthChecker {
  private MessageTransportBase transportBase;

  @Override
  public void start() {
    // keep mum
  }

  @Override
  public void stop() {
    // keep mum
  }

  @Override
  public void notifyTransportClosed(MessageTransport transport) {
    // who cares
  }

  @Override
  public void notifyTransportConnectAttempt(MessageTransport transport) {
    // who cares
  }

  @Override
  public void notifyTransportConnected(MessageTransport transport) {
    this.transportBase = (MessageTransportBase) transport;
    ConnectionHealthCheckerContext context = new ConnectionHealthCheckerContextEchoImpl(transportBase);
    transportBase.setHealthCheckerContext(context);
  }

  @Override
  public void notifyTransportDisconnected(MessageTransport transport, boolean forcedDisconnect) {
    // who cares
  }

  @Override
  public void notifyTransportReconnectionRejected(MessageTransport transport) {
    // NOP
  }

}
