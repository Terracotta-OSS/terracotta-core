/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.net.protocol.transport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A Dummy HealthChecker Context. Doesn't send any probe signals to peers. This is just a dumb context, keeps mouth shut
 * for all the PING probes it gets.
 * 
 * @author Manoj
 */
public class ConnectionHealthCheckerContextDummyImpl implements ConnectionHealthCheckerContext {
  private static final Logger logger = LoggerFactory.getLogger(ConnectionHealthCheckerContextDummyImpl.class);

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

  @Override
  public void close() {

  }

}
