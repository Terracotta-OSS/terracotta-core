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

  @Override
  public long getConnectTime() {
    throw new ImplementMe();
  }

  @Override
  public long getIdleTime() {
    throw new ImplementMe();
  }

  @Override
  public void addListener(TCConnectionEventListener listener) {
    addListenerCalls.put(listener);
  }

  @Override
  public void removeListener(TCConnectionEventListener listener) {
    //
  }

  @Override
  public void asynchClose() {
    throw new ImplementMe();
  }

  @Override
  public boolean close(long timeout) {
    throw new ImplementMe();
  }

  @Override
  public void connect(TCSocketAddress addr, int timeout) {
    throw new ImplementMe();
  }

  @Override
  public boolean asynchConnect(TCSocketAddress addr) {
    throw new ImplementMe();
  }

  @Override
  public boolean isConnected() {
    return connected;
  }

  @Override
  public boolean isClosed() {
    throw new ImplementMe();
  }

  @Override
  public TCSocketAddress getLocalAddress() {
    return new TCSocketAddress(42);
  }

  @Override
  public TCSocketAddress getRemoteAddress() {
    return new TCSocketAddress(TCSocketAddress.LOOPBACK_ADDR, 0);
  }

  @Override
  public void putMessage(TCNetworkMessage message) {
    throw new ImplementMe();
  }

  @Override
  public Socket detach() {
    throw new ImplementMe();
  }

  @Override
  public long getIdleReceiveTime() {
    throw new ImplementMe();
  }

  @Override
  public void addWeight(int addWeightBy) {
    connected = true;
  }

  @Override
  public void setTransportEstablished() {
    //
  }

  @Override
  public boolean isTransportEstablished() {
    return false;
  }

  @Override
  public boolean isClosePending() {
    return false;
  }

}
