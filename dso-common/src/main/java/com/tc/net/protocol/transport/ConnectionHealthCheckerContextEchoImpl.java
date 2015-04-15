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
 * ECHO HealthChecker Context. On receiving a PING probe, it sends back the PING_REPLY.
 * 
 * @author Manoj
 */
public class ConnectionHealthCheckerContextEchoImpl implements ConnectionHealthCheckerContext {
  private final MessageTransportBase             transport;
  private final HealthCheckerProbeMessageFactory messageFactory;
  private final TCLogger                         logger = TCLogging.getLogger(ConnectionHealthCheckerImpl.class);

  public ConnectionHealthCheckerContextEchoImpl(MessageTransportBase mtb) {
    this.transport = mtb;
    this.messageFactory = new TransportMessageFactoryImpl();
  }

  @Override
  public boolean receiveProbe(HealthCheckerProbeMessage message) {
    if (message.isPing()) {
      HealthCheckerProbeMessage pingReplyMessage = this.messageFactory.createPingReply(transport.getConnectionId(),
                                                                                       transport.getConnection());
      this.transport.send(pingReplyMessage);
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

}
