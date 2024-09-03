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

import java.io.IOException;

/**
 * ECHO HealthChecker Context. On receiving a PING probe, it sends back the PING_REPLY.
 * 
 * @author Manoj
 */
public class ConnectionHealthCheckerContextEchoImpl implements ConnectionHealthCheckerContext {
  private final MessageTransportBase             transport;
  private final HealthCheckerProbeMessageFactory messageFactory;
  private final Logger logger = LoggerFactory.getLogger(ConnectionHealthCheckerImpl.class);

  public ConnectionHealthCheckerContextEchoImpl(MessageTransportBase mtb) {
    this.transport = mtb;
    this.messageFactory = new TransportMessageFactoryImpl();
  }

  @Override
  public boolean receiveProbe(HealthCheckerProbeMessage message) {
    if (message.isPing()) {
      HealthCheckerProbeMessage pingReplyMessage = this.messageFactory.createPingReply(transport.getConnectionID(),
                                                                                       transport.getConnection());
      try {
        this.transport.send(pingReplyMessage);
      } catch (IOException ioe) {
        logger.warn("trouble ping", ioe);
        return true;
      }
      return true;
    } else if (message.isTimeCheck()) {
      // Just ignore time checks since we're just doing an echo only implementation
      return true;
    }
    logger.info(message.toString());
    throw new AssertionError("Echo HealthChecker");
  }

  @Override
  public void checkTime() {
    throw new AssertionError("Echo HealthChecker");
  }

  @Override
  public boolean probeIfAlive() {
    throw new AssertionError("Echo HealthChecker");
  }

  @Override
  public void refresh() {
    throw new AssertionError("Echo HealthChecker");
  }

  @Override
  public void close() {

  }

}
