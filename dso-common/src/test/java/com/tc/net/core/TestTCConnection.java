/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
  private volatile boolean            connected;

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
    //
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
    return connected;
  }

  public boolean isClosed() {
    throw new ImplementMe();
  }

  public TCSocketAddress getLocalAddress() {
    return new TCSocketAddress(42);
  }

  public TCSocketAddress getRemoteAddress() {
    return new TCSocketAddress(TCSocketAddress.LOOPBACK_ADDR, 0);
  }

  public void putMessage(TCNetworkMessage message) {
    throw new ImplementMe();
  }

  public Socket detach() {
    throw new ImplementMe();
  }

  public long getIdleReceiveTime() {
    throw new ImplementMe();
  }

  public void addWeight(int addWeightBy) {
    connected = true;
  }

  public void setTransportEstablished() {
    //
  }

  public boolean isTransportEstablished() {
    return false;
  }

}
