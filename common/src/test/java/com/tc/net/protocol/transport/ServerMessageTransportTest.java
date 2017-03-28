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

import com.tc.net.core.TCConnection;
import com.tc.net.core.event.TCConnectionEvent;
import com.tc.net.core.event.TCConnectionEventListener;
import com.tc.util.Assert;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Matchers;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 *
 */
public class ServerMessageTransportTest {
  
  public ServerMessageTransportTest() {
  }
  
  @BeforeClass
  public static void setUpClass() {
  }
  
  @AfterClass
  public static void tearDownClass() {
  }
  
  @Before
  public void setUp() {
  }
  
  @After
  public void tearDown() {
  }

  @Test
  public void testConnectionClose() throws Exception {
    ConnectionID id = new ConnectionID("JVM", 1);
    TCConnection connection = mock(TCConnection.class);
    
    MessageTransportListener checker = mock(MessageTransportListener.class);
    
    final List<TCConnectionEventListener> listeners = new ArrayList<TCConnectionEventListener>();
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        listeners.add((TCConnectionEventListener)invocation.getArguments()[0]);
        return null;
      }
    }).when(connection).addListener(Matchers.any(TCConnectionEventListener.class));
    TransportHandshakeErrorHandler errHdr = mock(TransportHandshakeErrorHandler.class);
    TransportHandshakeMessageFactory factory = mock(TransportHandshakeMessageFactory.class);
    ServerMessageTransport transport = new ServerMessageTransport(id, connection, errHdr, factory);
    transport.addTransportListener(checker);

    Assert.assertTrue(transport.status.isStart());
    
    TCConnectionEvent event = new TCConnectionEvent(connection);
    transport.connectEvent(event);

    Assert.assertTrue(transport.status.isConnected());

    for (TCConnectionEventListener trigger : listeners) {
      trigger.closeEvent(event);
    }
        
    TransportHandshakeMessage msg = mock(TransportHandshakeMessage.class);
    when(msg.isAck()).thenReturn(Boolean.TRUE);
    when(msg.getConnectionId()).thenReturn(id);
    when(msg.getSource()).thenReturn(connection);

    transport.connectEvent(event);
    transport.receiveTransportMessageImpl(msg);
    
    Assert.assertTrue(transport.status.isEstablished());
    
    for (TCConnectionEventListener trigger : listeners) {
      trigger.closeEvent(event);
    }
    
    transport.disconnect();
    
    Assert.assertTrue(transport.status.isDisconnected());
    
    for (TCConnectionEventListener trigger : listeners) {
      trigger.closeEvent(event);
    }    
    //  this one should not result on notifyTransportDisconnected
    for (TCConnectionEventListener trigger : listeners) {
      trigger.closeEvent(event);
    }    

    verify(checker, times(2)).notifyTransportDisconnected(Matchers.eq(transport), Matchers.eq(false));
    verify(checker, times(1)).notifyTransportDisconnected(Matchers.eq(transport), Matchers.eq(true));
  }
}
