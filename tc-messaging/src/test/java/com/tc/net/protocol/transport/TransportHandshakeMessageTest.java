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

import com.tc.bytes.TCReference;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TransportHandshakeMessageTest {

  private TransportHandshakeMessage   message;
  private TransportMessageFactoryImpl factory;

  @Before
  public void setUp() throws Exception {
    factory = new TransportMessageFactoryImpl();
  }

  @Test
  public void testSendAndReceive() throws Exception {
    boolean isMaxConnectionsExceeded = true;
    int maxConnections = 13;
    ConnectionID connectionId = new ConnectionID("abc", 1L);
    message = factory.createSynAck(connectionId, null, isMaxConnectionsExceeded, maxConnections);
    TCReference payload = message.getPayload();

    WireProtocolHeader header = new WireProtocolHeader();
    header.setProtocol(WireProtocolHeader.PROTOCOL_TRANSPORT_HANDSHAKE);
    message = new TransportMessageImpl(null, header, payload.duplicate());
    assertEquals(isMaxConnectionsExceeded, message.isMaxConnectionsExceeded());
    assertEquals(maxConnections, message.getMaxConnections());
  }
}
