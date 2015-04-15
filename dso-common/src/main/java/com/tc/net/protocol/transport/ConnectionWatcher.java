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

public class ConnectionWatcher implements MessageTransportListener {

  protected final ClientMessageTransport      cmt;
  protected final ClientConnectionEstablisher cce;
  protected final MessageTransportListener    target;

  /**
   * Listens to events from a MessageTransport, acts on them, and passes events through to target
   */
  public ConnectionWatcher(ClientMessageTransport cmt, MessageTransportListener target, ClientConnectionEstablisher cce) {
    this.cmt = cmt;
    this.target = target;
    this.cce = cce;
  }

  @Override
  public void notifyTransportClosed(MessageTransport transport) {
    cce.quitReconnectAttempts();
    target.notifyTransportClosed(transport);
  }

  @Override
  public void notifyTransportDisconnected(MessageTransport transport, final boolean forcedDisconnect) {
    cce.asyncReconnect(cmt);
    target.notifyTransportDisconnected(transport, forcedDisconnect);
  }

  @Override
  public void notifyTransportConnectAttempt(MessageTransport transport) {
    target.notifyTransportConnectAttempt(transport);
  }

  @Override
  public void notifyTransportConnected(MessageTransport transport) {
    target.notifyTransportConnected(transport);
  }

  @Override
  public void notifyTransportReconnectionRejected(MessageTransport transport) {
    target.notifyTransportReconnectionRejected(transport);
  }
}
