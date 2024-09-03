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

import com.tc.net.core.TCConnection;
import com.tc.net.core.event.TCConnectionEvent;
import com.tc.net.core.event.TCConnectionEventListener;
import com.tc.net.protocol.IllegalReconnectException;
import com.tc.util.Assert;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentMatchers;
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
    }).when(connection).addListener(ArgumentMatchers.any(TCConnectionEventListener.class));
    when(connection.isConnected()).thenReturn(Boolean.TRUE);
    TransportHandshakeErrorHandler errHdr = mock(TransportHandshakeErrorHandler.class);
    TransportHandshakeMessageFactory factory = mock(TransportHandshakeMessageFactory.class);
    ServerMessageTransport transport = new ServerMessageTransport(connection, errHdr, factory);
    transport.initConnectionID(id);
    transport.addTransportListener(checker);

    Assert.assertTrue(transport.status.isConnected());
    
    TCConnectionEvent event = new TCConnectionEvent(connection);

    for (TCConnectionEventListener trigger : listeners) {
      trigger.closeEvent(event);
    }
        
    TransportHandshakeMessage msg = mock(TransportHandshakeMessage.class);
    when(msg.isAck()).thenReturn(Boolean.TRUE);
    when(msg.getConnectionId()).thenReturn(id);
    when(msg.getSource()).thenReturn(connection);

    transport.wireNewConnection(connection);
    transport.connectEvent(event);
    transport.receiveTransportMessageImpl(msg);
    
    Assert.assertTrue(transport.status.isEstablished());
    
    for (TCConnectionEventListener trigger : listeners) {
      trigger.closeEvent(event);
    }
        
    transport.wireNewConnection(connection);
    transport.disconnect();
    
    Assert.assertTrue(transport.status.isDisconnected());
    
    for (TCConnectionEventListener trigger : listeners) {
      trigger.closeEvent(event);
    }    
    //  this one should not result on notifyTransportDisconnected
    for (TCConnectionEventListener trigger : listeners) {
      trigger.closeEvent(event);
    }    

    verify(checker, times(2)).notifyTransportDisconnected(eq(transport), eq(false));
    verify(checker, times(1)).notifyTransportDisconnected(eq(transport), eq(true));
  }
  
  @Test
  public void testDoubleAttachFails() throws Exception {
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
    }).when(connection).addListener(ArgumentMatchers.any(TCConnectionEventListener.class));
    when(connection.isConnected()).thenReturn(Boolean.TRUE);
    TransportHandshakeErrorHandler errHdr = mock(TransportHandshakeErrorHandler.class);
    TransportHandshakeMessageFactory factory = mock(TransportHandshakeMessageFactory.class);
    ServerMessageTransport transport = new ServerMessageTransport(errHdr, factory);
    transport.initConnectionID(id);
    transport.addTransportListener(checker);

    Assert.assertFalse(transport.status.isConnected());
    
    transport.attachNewConnection(connection);
    Assert.assertTrue(transport.status.isConnected());
        
    try {
      transport.attachNewConnection(connection);
      Assert.fail();
    } catch (IllegalReconnectException illegal) {
      //  expected
    }
    transport.close();
    Assert.assertFalse(transport.status.isConnected());
    try {
      transport.attachNewConnection(connection);
      Assert.fail();
    } catch (IllegalReconnectException illegal) {
      //  expected
    }
  }
}
