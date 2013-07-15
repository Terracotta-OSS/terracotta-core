/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.protocol;

import com.tc.exception.ImplementMe;
import com.tc.net.core.TCConnection;
import com.tc.net.protocol.transport.ConnectionID;
import com.tc.net.protocol.transport.MessageTransport;
import com.tc.net.protocol.transport.NetworkStackProvider;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class MockNetworkStackProvider implements NetworkStackProvider {

  private final BlockingQueue<TCConnection> connectTransportCalls       = new LinkedBlockingQueue<TCConnection>();

  public boolean            throwStackNotFoundException = false;

  public void attachNewConnection(TCConnection connection) throws StackNotFoundException {
    try {
      connectTransportCalls.put(connection);
      if (throwStackNotFoundException) { throw new StackNotFoundException(null, connection.getRemoteAddress()); }
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  public MessageTransport waitForConnectTransportCall(long timeout) throws InterruptedException {
    return (MessageTransport) connectTransportCalls.poll(timeout, TimeUnit.MILLISECONDS);
  }

  public NetworkStackHarness removeNetworkStack(ConnectionID connectionId) {
    throw new ImplementMe();
  }

  @Override
  public MessageTransport attachNewConnection(ConnectionID connectionId, TCConnection connection) {
    throw new ImplementMe();
  }
}
