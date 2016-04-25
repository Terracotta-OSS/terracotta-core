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
package com.tc.net.protocol.delivery;

import com.tc.net.protocol.tcm.MessageChannelInternal;
import com.tc.net.protocol.transport.MessageTransport;
import com.tc.properties.ReconnectConfig;
import com.tc.util.UUID;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 *
 */
public class OnceAndOnlyOnceProtocolNetworkLayerImplTest {
  OOOProtocolMessageFactory msgFactory;
  OOOProtocolMessageParser msgParser;
  ReconnectConfig reconnect;
  
  public OnceAndOnlyOnceProtocolNetworkLayerImplTest() {
  }
  
  @BeforeClass
  public static void setUpClass() {
  }
  
  @AfterClass
  public static void tearDownClass() {
  }
  
  @Before
  public void setUp() {
    msgFactory = new OOOProtocolMessageFactory();
    msgParser = new OOOProtocolMessageParser(msgFactory);
    reconnect = new AbstractReconnectConfig(true, 5000, 5000, 16, 32, "test");

  }
  
  @Test
  public void testGoodByeMessage() throws Exception {
    OnceAndOnlyOnceProtocolNetworkLayerImpl server = new OnceAndOnlyOnceProtocolNetworkLayerImpl(msgFactory, msgParser, reconnect, false);
    MessageChannelInternal recv = mock(MessageChannelInternal.class);
    server.setReceiveLayer(recv);
    MessageTransport send = mock(MessageTransport.class);
    server.setSendLayer(send);
    
    server.receive(msgFactory.createNewHandshakeMessage(UUID.NULL_ID, 0L).getEntireMessageData());
    OOOProtocolMessage msg = msgFactory.createNewGoodbyeMessage(UUID.NULL_ID);
    server.receive(msg.getEntireMessageData());
    
    verify(recv).close();
    verify(send).close();
  }
  
  @Test
  public void testClientClose() throws Exception {
    OnceAndOnlyOnceProtocolNetworkLayerImpl client = new OnceAndOnlyOnceProtocolNetworkLayerImpl(msgFactory, msgParser, reconnect, true);
    MessageChannelInternal recv = mock(MessageChannelInternal.class);
    client.setReceiveLayer(recv);
    MessageTransport send = mock(MessageTransport.class);
    client.setSendLayer(send);
    
    client.notifyTransportConnected(send);
    client.close();
    client.notifyTransportDisconnected(send, false);
    
    verify(recv).close();
    verify(send).close();
  }
  
  @Test
  public void testServerDisconnect() throws Exception {
    OnceAndOnlyOnceProtocolNetworkLayerImpl client = new OnceAndOnlyOnceProtocolNetworkLayerImpl(msgFactory, msgParser, reconnect, true);
    MessageChannelInternal recv = mock(MessageChannelInternal.class);
    client.setReceiveLayer(recv);
    MessageTransport send = mock(MessageTransport.class);
    client.setSendLayer(send);
    
    client.notifyTransportConnected(send);
    client.close();
    client.notifyTransportDisconnected(send, false);
    
    verify(recv).close();
    verify(send).close();
  }
  
  @After
  public void tearDown() {
  }
}
