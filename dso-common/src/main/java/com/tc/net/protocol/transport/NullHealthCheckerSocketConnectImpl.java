/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.net.core.event.TCConnectionErrorEvent;
import com.tc.net.core.event.TCConnectionEvent;

public class NullHealthCheckerSocketConnectImpl implements HealthCheckerSocketConnect {

  @Override
  public boolean probeConnectStatus() {
    return false;
  }

  @Override
  public SocketConnectStartStatus start() {
    return SocketConnectStartStatus.NOT_STARTED;
  }

  @Override
  public void closeEvent(TCConnectionEvent event) {
    //
  }

  @Override
  public void connectEvent(TCConnectionEvent event) {
    //
  }

  @Override
  public void endOfFileEvent(TCConnectionEvent event) {
    //
  }

  @Override
  public void errorEvent(TCConnectionErrorEvent errorEvent) {
    //
  }

  @Override
  public void addSocketConnectEventListener(HealthCheckerSocketConnectEventListener listener) {
    //
  }

  @Override
  public void removeSocketConnectEventListener(HealthCheckerSocketConnectEventListener listener) {
    //
  }

}
