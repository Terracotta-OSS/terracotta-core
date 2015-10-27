/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.net.protocol.transport;

import com.tc.bytes.TCByteBuffer;

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
    message = factory.createSynAck(connectionId, null, isMaxConnectionsExceeded, maxConnections, 43);
    TCByteBuffer payload[] = message.getPayload();

    WireProtocolHeader header = new WireProtocolHeader();
    header.setProtocol(WireProtocolHeader.PROTOCOL_TRANSPORT_HANDSHAKE);
    message = new TransportMessageImpl(null, header, payload);
    assertEquals(isMaxConnectionsExceeded, message.isMaxConnectionsExceeded());
    assertEquals(maxConnections, message.getMaxConnections());
  }
}
