/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.logging.TCLogger;
import com.tc.net.core.TCConnectionManager;

public class TestConnectionHealthCheckerImpl extends ConnectionHealthCheckerImpl {

  public TestConnectionHealthCheckerImpl(HealthCheckerConfig healthCheckerConfig, TCConnectionManager connManager) {
    super(healthCheckerConfig, connManager);
  }

  protected HealthCheckerMonitorThreadEngine getHealthMonitorThreadEngine(HealthCheckerConfig config,
                                                                          TCConnectionManager connectionManager,
                                                                          TCLogger loger) {
    return new TestHealthCheckerMonitorThreadEngine(config, connectionManager, loger);
  }

  class TestHealthCheckerMonitorThreadEngine extends HealthCheckerMonitorThreadEngine {

    public TestHealthCheckerMonitorThreadEngine(HealthCheckerConfig healthCheckerConfig,
                                                TCConnectionManager connectionManager, TCLogger logger) {
      super(healthCheckerConfig, connectionManager, logger);
    }

    protected ConnectionHealthCheckerContext getHealthCheckerContext(MessageTransportBase transport,
                                                                     HealthCheckerConfig conf,
                                                                     TCConnectionManager connManager) {

      return new TestConnectionHealthCheckerContextImpl(transport, conf, connManager);
    }

  }
}
