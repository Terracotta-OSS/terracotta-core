/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
