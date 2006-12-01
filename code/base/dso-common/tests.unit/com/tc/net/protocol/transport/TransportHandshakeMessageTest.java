/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.bytes.TCByteBuffer;

import junit.framework.TestCase;

public class TransportHandshakeMessageTest extends TestCase {

  private TransportHandshakeMessage            message;
  private TransportHandshakeMessageFactoryImpl factory;

  public void setUp() throws Exception {

    factory = new TransportHandshakeMessageFactoryImpl();

  }

  public void testSendAndReceive() throws Exception {
    boolean isMaxConnectionsExceeded = true;
    int maxConnections = 13;
    DefaultConnectionIdFactory connectionIDProvider = new DefaultConnectionIdFactory();
    ConnectionID connectionId = connectionIDProvider.nextConnectionId();
    message = factory.createSynAck(connectionId, null, isMaxConnectionsExceeded, maxConnections);
    TCByteBuffer payload[] = message.getPayload();

    WireProtocolHeader header = new WireProtocolHeader();
    header.setProtocol(WireProtocolHeader.PROTOCOL_TRANSPORT_HANDSHAKE);
    message = new TransportHandshakeMessageImpl(null, header, payload);
    assertEquals(isMaxConnectionsExceeded, message.isMaxConnectionsExceeded());
    assertEquals(maxConnections, message.getMaxConnections());
  }
}
