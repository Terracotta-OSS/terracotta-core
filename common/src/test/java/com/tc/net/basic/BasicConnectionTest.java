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
package com.tc.net.basic;

import com.tc.bytes.TCReference;
import com.tc.net.core.BufferManager;
import com.tc.net.core.BufferManagerFactory;
import com.tc.net.core.SocketEndpoint;
import com.tc.net.core.TCConnection;
import com.tc.net.core.event.TCConnectionEvent;
import com.tc.net.core.event.TCConnectionEventListener;
import com.tc.net.protocol.TCProtocolAdaptor;
import com.tc.net.protocol.transport.WireProtocolHeader;
import com.tc.net.protocol.transport.WireProtocolMessage;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.SocketChannel;
import java.util.function.Consumer;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.terracotta.utilities.test.net.PortManager;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 *
 */
public class BasicConnectionTest {

  public BasicConnectionTest() {
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

  private ServerSocket openServerSocket(int port) {
    ServerSocket server = null;
    while (server == null) {
      try {
        server = new ServerSocket(port);
      } catch (IOException ioe) {
        // ignored
      }
    }
    return server;
  }

  /**
   * Test of getConnectTime method, of class BasicConnection.
   */
  @Test
  public void testGetConnectTime() throws Exception {
    System.out.println("getConnectTime");

    try (PortManager.PortRef portRef = PortManager.getInstance().reservePort()) {
      try (ServerSocket server = openServerSocket(portRef.port())) {
        TCProtocolAdaptor adapter = mock(TCProtocolAdaptor.class);
        BufferManagerFactory buffer = mock(BufferManagerFactory.class);
        when(buffer.createSocketEndpoint(any(SocketChannel.class), any(boolean.class))).thenReturn(mock(SocketEndpoint.class));
        Consumer<TCConnection> close = s -> {};
        BasicConnection instance = new BasicConnection("", adapter, buffer, close);
        long expResult = 0L;
        assertEquals(expResult, instance.getConnectTime());
        instance.connect(new InetSocketAddress(server.getLocalPort()), 0);
        assertNotEquals(expResult, instance.getConnectTime());
        instance.close();
      }
    }
  }

  /**
   * Test of getIdleTime method, of class BasicConnection.
   */
  @Test
  public void testGetIdleTime() throws Exception {
    System.out.println("getIdleTime");
    try (PortManager.PortRef portRef = PortManager.getInstance().reservePort()) {
      try (ServerSocket server = openServerSocket(portRef.port())) {
        TCProtocolAdaptor adapter = mock(TCProtocolAdaptor.class);
        BufferManagerFactory buffer = mock(BufferManagerFactory.class);
        SocketEndpoint mgr = mock(SocketEndpoint.class);
        when(buffer.createSocketEndpoint(any(SocketChannel.class), any(boolean.class))).thenReturn(mgr);
        Consumer<TCConnection> close = s -> {};
        BasicConnection instance = new BasicConnection("", adapter, buffer, close);
        instance.connect(new InetSocketAddress(server.getLocalPort()), 0);
        long idleTime = instance.getIdleTime();
        Thread.sleep(1000);
        assertNotEquals(idleTime, instance.getIdleTime());
        idleTime = instance.getIdleTime();
        WireProtocolMessage msg = mock(WireProtocolMessage.class);
        when(msg.getHeader()).thenReturn(mock(WireProtocolHeader.class));
        when(msg.getEntireMessageData()).thenReturn(mock(TCReference.class));
        instance.putMessage(msg);
        assertTrue(idleTime > instance.getIdleTime());
        instance.close();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * Test of getIdleReceiveTime method, of class BasicConnection.
   */
  @Test
  public void testGetIdleReceiveTime() throws Exception {
    System.out.println("getIdleReceiveTime");
    try (PortManager.PortRef portRef = PortManager.getInstance().reservePort()) {
      try (ServerSocket server = openServerSocket(portRef.port())) {
        TCProtocolAdaptor adapter = mock(TCProtocolAdaptor.class);
        BufferManagerFactory buffer = mock(BufferManagerFactory.class);
        when(buffer.createSocketEndpoint(any(SocketChannel.class), any(boolean.class))).thenReturn(mock(SocketEndpoint.class));
        Consumer<TCConnection> close = s -> {};
        BasicConnection instance = new BasicConnection("", adapter, buffer, close);
        instance.connect(new InetSocketAddress(server.getLocalPort()), 0);
        long idleTime = instance.getIdleTime();
        Thread.sleep(1000);
        assertNotEquals(idleTime, instance.getIdleTime());
        instance.close();
      }
    }
  }

  /**
   * Test of addListener method, of class BasicConnection.
   */
  @Test
  public void testListener() throws Exception {
    System.out.println("addListener");
    try (PortManager.PortRef portRef = PortManager.getInstance().reservePort()) {
      try (ServerSocket server = openServerSocket(portRef.port())) {
        TCProtocolAdaptor adapter = mock(TCProtocolAdaptor.class);
        BufferManagerFactory buffer = mock(BufferManagerFactory.class);
        when(buffer.createSocketEndpoint(any(SocketChannel.class), any(boolean.class))).thenReturn(mock(SocketEndpoint.class));
        Consumer<TCConnection> close = s -> {};
        BasicConnection instance = new BasicConnection("", adapter, buffer, close);
        TCConnectionEventListener listener = mock(TCConnectionEventListener.class);
        instance.addListener(listener);
        instance.connect(new InetSocketAddress(server.getLocalPort()), 0);
        instance.close();
        verify(listener).closeEvent(any(TCConnectionEvent.class));
        verify(listener).connectEvent(any(TCConnectionEvent.class));
      }
    }
  }
}
