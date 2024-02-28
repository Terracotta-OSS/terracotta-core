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
package com.tc.net.core;

import com.tc.net.protocol.NullProtocolAdaptor;
import com.tc.net.protocol.ProtocolAdaptorFactory;
import com.tc.util.concurrent.ThreadUtil;

import java.util.Arrays;

import junit.framework.TestCase;
import com.tc.net.protocol.TCProtocolAdaptor;
import com.tc.properties.TCPropertiesImpl;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collections;
import com.tc.net.core.ClearTextBufferManagerFactory;
/**
 * TODO Jan 13, 2005: comment describing what this class is for.
 */
public class TCConnectionManagerTest extends TestCase {

  private TCConnectionManager clientConnMgr;
  private TCConnectionManager serverConnMgr;
  private TCListener          lsnr;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    TCPropertiesImpl.getProperties().overwriteTcPropertiesFromConfig(Collections.emptyMap());
    this.clientConnMgr = new TCConnectionManagerImpl("Client", 0, new ClearTextBufferManagerFactory());
    this.serverConnMgr = new TCConnectionManagerImpl("Server", 0, new ClearTextBufferManagerFactory());
    this.lsnr = this.serverConnMgr.createListener(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), new ProtocolAdaptorFactory() {
      @Override
      public TCProtocolAdaptor getInstance() {
        return new NullProtocolAdaptor();
      }
    });
  }

  @Override
  protected void tearDown() throws Exception {
    clientConnMgr.shutdown();
    serverConnMgr.shutdown();
  }

  public void testCreateConnection() throws Exception {
    assertEquals(0, clientConnMgr.getAllConnections().length);

    TCConnection conn1 = clientConnMgr.createConnection(new NullProtocolAdaptor());
    TCConnection conn2 = clientConnMgr.createConnection(new NullProtocolAdaptor());
    TCConnection conn3 = clientConnMgr.createConnection(new NullProtocolAdaptor());

    TCConnection conns[] = clientConnMgr.getAllConnections();
    assertEquals(3, conns.length);

    assertTrue(Arrays.asList(conns).containsAll(Arrays.asList(new Object[] { conn1, conn2, conn3 })));

    clientConnMgr.closeAllConnections();

    assertEquals(0, clientConnMgr.getAllConnections().length);

    conn1 = clientConnMgr.createConnection(new NullProtocolAdaptor());
    assertEquals(1, clientConnMgr.getAllConnections().length);

    conn1.close();
    assertEquals(0, clientConnMgr.getAllConnections().length);

    conn1 = clientConnMgr.createConnection(new NullProtocolAdaptor());
    assertEquals(1, clientConnMgr.getAllConnections().length);
    conn1.connect(lsnr.getBindSocketAddress(), 3000);
    assertEquals(1, clientConnMgr.getAllConnections().length);
    conn1.close();
    assertEquals(0, clientConnMgr.getAllConnections().length);
  }

  public void testShutdown() throws Exception {
    assertEquals(1, serverConnMgr.getAllListeners().length);
    assertEquals(0, clientConnMgr.getAllConnections().length);

    clientConnMgr.createConnection(new NullProtocolAdaptor());

    assertEquals(1, serverConnMgr.getAllListeners().length);
    assertEquals(1, clientConnMgr.getAllConnections().length);

    serverConnMgr.shutdown();
    clientConnMgr.shutdown();

    assertEquals(0, serverConnMgr.getAllListeners().length);
    assertEquals(0, clientConnMgr.getAllConnections().length);

    // double shutdown call
    serverConnMgr.shutdown();
    clientConnMgr.shutdown();
  }

  public void testCreateListenerTCSocketAddressProtocolAdaptorFactory() {
    assertEquals(1, serverConnMgr.getAllListeners().length);
    this.lsnr.stop();
    assertEquals(0, serverConnMgr.getAllListeners().length);
  }

  public void testActiveClientConnections() throws Exception {
    assertEquals(0, clientConnMgr.getAllConnections().length);

    TCConnection conn1 = clientConnMgr.createConnection(new NullProtocolAdaptor());
    TCConnection conn2 = clientConnMgr.createConnection(new NullProtocolAdaptor());
    TCConnection conn3 = clientConnMgr.createConnection(new NullProtocolAdaptor());

    TCConnection conns[] = clientConnMgr.getAllConnections();
    assertEquals(3, conns.length);
    assertTrue(Arrays.asList(conns).containsAll(Arrays.asList(new Object[] { conn1, conn2, conn3 })));

    conn1.connect(lsnr.getBindSocketAddress(), 5000);
    conn2.connect(lsnr.getBindSocketAddress(), 5000);
    conn3.connect(lsnr.getBindSocketAddress(), 5000);

    while (serverConnMgr.getAllConnections().length < 3) {
      System.out.println("Waiting for server conn");
      ThreadUtil.reallySleep(500);
    }

    conn1.setTransportEstablished();
    conn2.setTransportEstablished();
    conn3.setTransportEstablished();

    TCConnection activeConns[] = clientConnMgr.getAllConnections();
    assertEquals(3, activeConns.length);
    assertTrue(Arrays.asList(activeConns).containsAll(Arrays.asList(new Object[] { conn1, conn2, conn3 })));

    while (serverConnMgr.getAllConnections().length < 3) {
      System.out.println("Waiting for server conn");
      ThreadUtil.reallySleep(500);
    }

    clientConnMgr.closeAllConnections();
    assertEquals(0, clientConnMgr.getAllConnections().length);

    while (serverConnMgr.getAllConnections().length > 0) {
      System.out.println("Waiting for server conn close");
      ThreadUtil.reallySleep(500);
    }
    assertEquals(0, serverConnMgr.getAllConnections().length);

    conn1 = clientConnMgr.createConnection(new NullProtocolAdaptor());
    conn2 = clientConnMgr.createConnection(new NullProtocolAdaptor());
    assertEquals(2, clientConnMgr.getAllConnections().length);
    conn1.connect(lsnr.getBindSocketAddress(), 5000);
    conn2.connect(lsnr.getBindSocketAddress(), 5000);

    while (serverConnMgr.getAllConnections().length < 2) {
      System.out.println("Waiting for server conn");
      ThreadUtil.reallySleep(500);
    }

    conn1.setTransportEstablished();
    conn2.setTransportEstablished();
    assertEquals(2, clientConnMgr.getAllConnections().length);

    for (TCConnection c : serverConnMgr.getAllConnections()) {
      c.setTransportEstablished();
    }

    while (serverConnMgr.getAllConnections().length < 2) {
      System.out.println("Waiting for client conns");
      ThreadUtil.reallySleep(500);
    }
    assertEquals(2, serverConnMgr.getAllConnections().length);

    conns = clientConnMgr.getAllConnections();
    assertEquals(2, conns.length);
    assertTrue(Arrays.asList(conns).containsAll(Arrays.asList(new Object[] { conn1, conn2 })));

    activeConns = clientConnMgr.getAllConnections();
    assertEquals(2, activeConns.length);
    assertTrue(Arrays.asList(activeConns).containsAll(Arrays.asList(new Object[] { conn1, conn2 })));

    conn1.close();
    conn2.close();
    
    assertEquals(0, clientConnMgr.getAllConnections().length);

    while (serverConnMgr.getAllConnections().length > 0) {
      System.out.println("Waiting for client conns to close");
      ThreadUtil.reallySleep(500);
    }
    assertEquals(0, serverConnMgr.getAllConnections().length);
  }

  public void testInActiveClientConnections() throws Exception {
    this.serverConnMgr = new TCConnectionManagerImpl("TestConnMgr", 0, new ClearTextBufferManagerFactory());
    this.lsnr = this.serverConnMgr.createListener(new InetSocketAddress(0), new ProtocolAdaptorFactory() {
      @Override
      public TCProtocolAdaptor getInstance() {
        return new NullProtocolAdaptor();
      }
    });

    TCConnection conn1 = clientConnMgr.createConnection(new NullProtocolAdaptor());
    TCConnection conn2 = clientConnMgr.createConnection(new NullProtocolAdaptor());

    conn1.connect(lsnr.getBindSocketAddress(), 3000);
    conn2.connect(lsnr.getBindSocketAddress(), 3000);

    conn1.setTransportEstablished();
    conn2.setTransportEstablished();
    assertEquals(2, clientConnMgr.getAllConnections().length);

    while (serverConnMgr.getAllConnections().length < 2) {
      System.out.println("waiting for clients");
      ThreadUtil.reallySleep(500);
    }

    TCConnection[] conns = serverConnMgr.getAllConnections();
    for (TCConnection conn : conns) {
      conn.setTransportEstablished();
    }

    assertEquals(2, serverConnMgr.getAllConnections().length);
  }

}
