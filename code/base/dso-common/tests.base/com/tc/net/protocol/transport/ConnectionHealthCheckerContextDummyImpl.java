/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

/**
 * A Dummy HealthChecker Context. Doesn't send any probe signals to peers. This is just a dumb context, keeps mouth shut
 * for all the PING probes it gets.
 * 
 * @author Manoj
 */
public class ConnectionHealthCheckerContextDummyImpl implements ConnectionHealthCheckerContext {

  public boolean receiveProbe(HealthCheckerProbeMessage message) {
    if (message.isPing()) {
      // keep mum
      return true;
    }
    throw new AssertionError("Dummy HealthChecker");
  }

  public boolean probeIfAlive() {
    throw new AssertionError("Dummy HealthChecker");
  }

  public void refresh() {
    throw new AssertionError("Dummy HealthChecker");
  }

}
