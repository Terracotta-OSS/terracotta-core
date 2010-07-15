/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.net.core.event.TCConnectionEventListener;

/**
 * Helps HealthChecker in doing extra checks to monitor peer node's health. Here, a socket connect is attempted to some
 * of the peer node's listening port.
 * 
 * @author Manoj
 */
public interface HealthCheckerSocketConnect extends TCConnectionEventListener {

  enum SocketConnectStartStatus {
    STARTED, NOT_STARTED, FAILED
  }

  public SocketConnectStartStatus start();

  /* Once in a probe interval, the health checker queries to get the connect status if wanted */
  public boolean probeConnectStatus();

  public void addSocketConnectEventListener(HealthCheckerSocketConnectEventListener listener);

  public void removeSocketConnectEventListener(HealthCheckerSocketConnectEventListener listener);

}
