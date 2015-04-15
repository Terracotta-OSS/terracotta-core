/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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
  public void notifyTransportDisconnected(MessageTransport transport, final boolean forcedDisconnect) {
    // who cares
  }

  @Override
  public void notifyTransportReconnectionRejected(MessageTransport transport) {
    // NOP
  }

}
