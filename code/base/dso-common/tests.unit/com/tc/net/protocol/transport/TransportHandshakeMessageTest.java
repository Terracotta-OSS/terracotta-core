/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.bytes.TCByteBuffer;

import junit.framework.TestCase;

public class TransportHandshakeMessageTest extends TestCase {

  private TransportHandshakeMessage   message;
  private TransportMessageFactoryImpl factory;

  @Override
  public void setUp() throws Exception {
    factory = new TransportMessageFactoryImpl();
  }

  public void testSendAndReceive() throws Exception {
    boolean isMaxConnectionsExceeded = true;
    int maxConnections = 13;
    DefaultConnectionIdFactory connectionIDProvider = new DefaultConnectionIdFactory();
    ConnectionID connectionId = connectionIDProvider.nextConnectionId(JvmIDUtil.getJvmID());
    message = factory.createSynAck(connectionId, null, isMaxConnectionsExceeded, maxConnections, 43);
    TCByteBuffer payload[] = message.getPayload();

    WireProtocolHeader header = new WireProtocolHeader();
    header.setProtocol(WireProtocolHeader.PROTOCOL_TRANSPORT_HANDSHAKE);
    message = new TransportMessageImpl(null, header, payload);
    assertEquals(isMaxConnectionsExceeded, message.isMaxConnectionsExceeded());
    assertEquals(maxConnections, message.getMaxConnections());
  }
}
