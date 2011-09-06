/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.core;

import com.tc.exception.ImplementMe;
import com.tc.net.TCSocketAddress;
import com.tc.net.core.event.TCConnectionEventListener;
import com.tc.net.protocol.NetworkMessageSink;
import com.tc.net.protocol.NullProtocolAdaptor;
import com.tc.net.protocol.TCNetworkMessage;
import com.tc.net.protocol.TCProtocolAdaptor;
import com.tc.util.TCTimeoutException;
import com.tc.util.concurrent.NoExceptionLinkedQueue;

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * @deprecated use TestTCConnection instead. This implementation has a bunch of side effects.
 */
public class MockTCConnection implements TCConnection {

  private boolean                     isConnected;
  private boolean                     isClosed;

  private int                         closeCallCount  = 0;
  private List                        sentMessages    = new ArrayList();
  private NetworkMessageSink          messageSink;
  private final TCProtocolAdaptor     protocolAdaptor = new NullProtocolAdaptor();

  public TCSocketAddress              localAddress    = new TCSocketAddress(TCSocketAddress.LOOPBACK_ADDR, 0);
  public TCSocketAddress              remoteAddress   = new TCSocketAddress(TCSocketAddress.LOOPBACK_ADDR, 0);

  public boolean                      fail            = false;
  public final NoExceptionLinkedQueue connectCalls    = new NoExceptionLinkedQueue();

  public long getConnectTime() {
    throw new ImplementMe();
  }

  public long getIdleTime() {
    throw new ImplementMe();
  }

  public void addListener(TCConnectionEventListener listener) {
    //
  }

  public void removeListener(TCConnectionEventListener listener) {
    //
  }

  public boolean close(long timeout) {
    isConnected = false;
    isClosed = true;
    closeCallCount++;
    return true;
  }

  public int getCloseCallCount() {
    return this.closeCallCount;
  }

  public void connect(TCSocketAddress addr, int timeout) throws TCTimeoutException {
    connectCalls.put(new Object[] { addr, new Integer(timeout) });
    if (fail) { throw new TCTimeoutException("Timed out !!!"); }
    this.isConnected = true;
  }

  public boolean asynchConnect(TCSocketAddress addr) {
    return this.isConnected = true;
  }

  public void isConnected(boolean b) {
    this.isConnected = b;
  }

  public boolean isConnected() {
    return isConnected;
  }

  public boolean isClosed() {
    return isClosed;
  }

  public void setMessageSink(NetworkMessageSink sink) {
    this.messageSink = sink;
  }

  public void putMessage(TCNetworkMessage message) {
    this.sentMessages.add(message);
    if (this.messageSink != null) this.messageSink.putMessage(message);
  }

  public List getSentMessages() {
    return this.sentMessages;
  }

  public TCSocketAddress getLocalAddress() {
    return this.localAddress;
  }

  public TCSocketAddress getRemoteAddress() {
    return this.remoteAddress;
  }

  public TCProtocolAdaptor getProtocolAdaptor() {
    return this.protocolAdaptor;
  }

  public void asynchClose() {
    close(-1);
  }

  public Socket detach() {
    throw new ImplementMe();
  }

  public long getIdleReceiveTime() {
    throw new ImplementMe();
  }

  public void addWeight(int addWeightBy) {
    //
  }

  public void setTransportEstablished() {
    //
  }

  public boolean isTransportEstablished() {
    return false;
  }
}