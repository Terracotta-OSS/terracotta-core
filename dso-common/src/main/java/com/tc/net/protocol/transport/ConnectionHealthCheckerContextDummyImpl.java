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
import com.tc.logging.TCLogging;

/**
 * A Dummy HealthChecker Context. Doesn't send any probe signals to peers. This is just a dumb context, keeps mouth shut
 * for all the PING probes it gets.
 * 
 * @author Manoj
 */
public class ConnectionHealthCheckerContextDummyImpl implements ConnectionHealthCheckerContext {
  private static final TCLogger logger = TCLogging.getLogger(ConnectionHealthCheckerContextDummyImpl.class);

  @Override
  public boolean receiveProbe(HealthCheckerProbeMessage message) {
    if (!message.isPing()) {
      logger.error("Unexpected probe message received by Dummy HealthCheckerContext: " + message);
    }
    return true;
  }

  @Override
  public void checkTime() {
    throw new AssertionError("Dummy HealthCheckerContext.");
  }

  @Override
  public boolean probeIfAlive() {
    throw new AssertionError("Dummy HealthCheckerContext.");
  }

  @Override
  public void refresh() {
    throw new AssertionError("Dummy HealthCheckerContext.");
  }

}
