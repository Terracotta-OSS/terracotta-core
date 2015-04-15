/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.net.protocol.transport;

import com.tc.logging.TCLogger;
import com.tc.net.core.TCConnectionManager;

public class TestConnectionHealthCheckerImpl extends ConnectionHealthCheckerImpl {

  public TestConnectionHealthCheckerImpl(HealthCheckerConfig healthCheckerConfig, TCConnectionManager connManager) {
    super(healthCheckerConfig, connManager);
  }

  @Override
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

    @Override
    protected ConnectionHealthCheckerContext getHealthCheckerContext(MessageTransportBase transport,
                                                                     HealthCheckerConfig conf,
                                                                     TCConnectionManager connManager) {

      return new TestConnectionHealthCheckerContextImpl(transport, conf, connManager);
    }

  }
}
