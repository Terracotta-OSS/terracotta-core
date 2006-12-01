/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.core;

import com.tc.exception.ImplementMe;
import com.tc.net.TCSocketAddress;
import com.tc.net.core.event.TCConnectionEventListener;
import com.tc.net.protocol.TCNetworkMessage;
import com.tc.util.concurrent.NoExceptionLinkedQueue;

import java.net.Socket;

public class TestTCConnection implements TCConnection {

  public final NoExceptionLinkedQueue addListenerCalls = new NoExceptionLinkedQueue();

  public long getConnectTime() {
    throw new ImplementMe();
  }

  public long getIdleTime() {
    throw new ImplementMe();
  }

  public void addListener(TCConnectionEventListener listener) {
    addListenerCalls.put(listener);
  }

  public void removeListener(TCConnectionEventListener listener) {
    throw new ImplementMe();
  }

  public void asynchClose() {
    throw new ImplementMe();
  }

  public boolean close(long timeout) {
    throw new ImplementMe();
  }

  public void connect(TCSocketAddress addr, int timeout) {
    throw new ImplementMe();
  }

  public boolean asynchConnect(TCSocketAddress addr) {
    throw new ImplementMe();
  }

  public boolean isConnected() {
    throw new ImplementMe();
  }

  public boolean isClosed() {
    throw new ImplementMe();
  }

  public TCSocketAddress getLocalAddress() {
    throw new ImplementMe();
  }

  public TCSocketAddress getRemoteAddress() {
    throw new ImplementMe();
  }

  public void putMessage(TCNetworkMessage message) {
    throw new ImplementMe();
  }

  public Socket detach() {
    throw new ImplementMe();
  }

}
