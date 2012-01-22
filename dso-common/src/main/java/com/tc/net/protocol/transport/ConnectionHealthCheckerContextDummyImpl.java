/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;

/**
 * A Dummy HealthChecker Context. Doesn't send any probe signals to peers. This is just a dumb context, keeps mouth shut
 * for all the PING probes it gets.
 * 
 * @author Manoj
 */
public class ConnectionHealthCheckerContextDummyImpl implements ConnectionHealthCheckerContext {
  private static final TCLogger logger = TCLogging.getLogger(ConnectionHealthCheckerContextDummyImpl.class);

  public boolean receiveProbe(HealthCheckerProbeMessage message) {
    if (!message.isPing()) {
      logger.error("Unexpected probe message received by Dummy HealthCheckerContext: " + message);
    }
    return true;
  }

  public boolean probeIfAlive() {
    throw new AssertionError("Dummy HealthCheckerContext.");
  }

  public void refresh() {
    throw new AssertionError("Dummy HealthCheckerContext.");
  }

}
