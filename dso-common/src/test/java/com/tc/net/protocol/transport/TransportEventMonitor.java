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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Monitors transport events.
 */
public class TransportEventMonitor implements MessageTransportListener {

  private final BlockingQueue<Object> connectedEvents      = new LinkedBlockingQueue<Object>();
  private final BlockingQueue<Object> disconnectedEvents   = new LinkedBlockingQueue<Object>();
  private final BlockingQueue<Object> connectAttemptEvents = new LinkedBlockingQueue<Object>();
  private final BlockingQueue<Object> closedEvents         = new LinkedBlockingQueue<Object>();
  private final BlockingQueue<Object> rejectedEvents       = new LinkedBlockingQueue<Object>();

  @Override
  public void notifyTransportConnected(MessageTransport transport) {
    try {
      connectedEvents.put(new Object());
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void notifyTransportDisconnected(MessageTransport transport, final boolean forcedDisconnect) {
    try {
      disconnectedEvents.put(new Object());
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void notifyTransportConnectAttempt(MessageTransport transport) {
    try {
      this.connectAttemptEvents.put(new Object());
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void notifyTransportClosed(MessageTransport transport) {
    try {
      this.closedEvents.put(new Object());
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void notifyTransportReconnectionRejected(MessageTransport transport) {
    try {
      this.rejectedEvents.put(new Object());
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  public boolean waitForConnect(long timeout) throws InterruptedException {
    return this.connectedEvents.poll(timeout, TimeUnit.MILLISECONDS) != null;
  }

  public boolean waitForDisconnect(long timeout) throws InterruptedException {
    return this.disconnectedEvents.poll(timeout, TimeUnit.MILLISECONDS) != null;
  }

  public boolean waitForConnectAttempt(long timeout) throws InterruptedException {
    return this.connectAttemptEvents.poll(timeout, TimeUnit.MILLISECONDS) != null;
  }

  public boolean waitForClose(long timeout) throws InterruptedException {
    return this.closedEvents.poll(timeout, TimeUnit.MILLISECONDS) != null;
  }

  public boolean waitForConnectionRejected(long timeout) throws InterruptedException {
    return this.rejectedEvents.poll(timeout, TimeUnit.MILLISECONDS) != null;
  }

}
