/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.net.core.event.TCConnectionErrorEvent;
import com.tc.net.core.event.TCConnectionEvent;

public class NullHealthCheckerSocketConnectImpl implements HealthCheckerSocketConnect {

  public boolean probeConnectStatus() {
    return false;
  }

  public SocketConnectStartStatus start() {
    return SocketConnectStartStatus.NOT_STARTED;
  }

  public void closeEvent(TCConnectionEvent event) {
    //
  }

  public void connectEvent(TCConnectionEvent event) {
    //
  }

  public void endOfFileEvent(TCConnectionEvent event) {
    //
  }

  public void errorEvent(TCConnectionErrorEvent errorEvent) {
    //
  }

  public void addSocketConnectEventListener(HealthCheckerSocketConnectEventListener listener) {
    //
  }

  public void removeSocketConnectEventListener(HealthCheckerSocketConnectEventListener listener) {
    //
  }

}
