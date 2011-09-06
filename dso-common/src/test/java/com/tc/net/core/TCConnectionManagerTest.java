/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.core;

import com.tc.net.TCSocketAddress;
import com.tc.net.protocol.NullProtocolAdaptor;
import com.tc.net.protocol.ProtocolAdaptorFactory;
import com.tc.net.protocol.TCProtocolAdaptor;
import com.tc.net.protocol.transport.ConnectionHealthCheckerUtil;
import com.tc.net.protocol.transport.HealthCheckerConfig;
import com.tc.net.protocol.transport.HealthCheckerConfigImpl;
import com.tc.util.concurrent.ThreadUtil;

import java.util.Arrays;

import junit.framework.TestCase;

/**
 * TODO Jan 13, 2005: comment describing what this class is for.
 */
public class TCConnectionManagerTest extends TestCase {

  private TCConnectionManager clientConnMgr;
  private TCConnectionManager serverConnMgr;
  private TCListener          lsnr;

  protected void setUp() throws Exception {
    super.setUp();
    this.clientConnMgr = new TCConnectionManagerImpl();
    this.serverConnMgr = new TCConnectionManagerImpl();
    this.lsnr = this.serverConnMgr.createListener(new TCSocketAddress(0), new ProtocolAdaptorFactory() {
      public TCProtocolAdaptor getInstance() {
        return new NullProtocolAdaptor();
      }
    });
  }

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

    clientConnMgr.closeAllConnections(5000);

    assertEquals(0, clientConnMgr.getAllConnections().length);

    conn1 = clientConnMgr.createConnection(new NullProtocolAdaptor());
    assertEquals(1, clientConnMgr.getAllConnections().length);

    conn1.close(5000);
    assertEquals(0, clientConnMgr.getAllConnections().length);

    conn1 = clientConnMgr.createConnection(new NullProtocolAdaptor());
    assertEquals(1, clientConnMgr.getAllConnections().length);
    conn1.connect(lsnr.getBindSocketAddress(), 3000);
    assertEquals(1, clientConnMgr.getAllConnections().length);
    conn1.close(5000);
    assertEquals(0, clientConnMgr.getAllConnections().length);
  }

  public void testShutdown() {
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

    conn1.setTransportEstablished();
    conn2.setTransportEstablished();
    conn3.setTransportEstablished();

    TCConnection activeConns[] = clientConnMgr.getAllActiveConnections();
    assertEquals(3, activeConns.length);
    assertTrue(Arrays.asList(activeConns).containsAll(Arrays.asList(new Object[] { conn1, conn2, conn3 })));

    clientConnMgr.closeAllConnections(5000);
    assertEquals(0, clientConnMgr.getAllConnections().length);
    assertEquals(0, clientConnMgr.getAllActiveConnections().length);

    while (serverConnMgr.getAllConnections().length > 0) {
      System.out.println("Waiting for server conn close");
      ThreadUtil.reallySleep(500);
    }

    conn1 = clientConnMgr.createConnection(new NullProtocolAdaptor());
    conn2 = clientConnMgr.createConnection(new NullProtocolAdaptor());
    assertEquals(2, clientConnMgr.getAllConnections().length);
    conn1.connect(lsnr.getBindSocketAddress(), 5000);
    conn2.connect(lsnr.getBindSocketAddress(), 5000);
    conn1.setTransportEstablished();
    conn2.setTransportEstablished();
    assertEquals(2, clientConnMgr.getAllActiveConnections().length);

    while (serverConnMgr.getAllConnections().length < 2) {
      System.out.println("Waiting for client conns");
      ThreadUtil.reallySleep(500);
    }
    
    conns = serverConnMgr.getAllConnections();
    assertEquals(2, conns.length);
    
    for (TCConnection conn : conns) {
      conn.setTransportEstablished();
    }
    assertEquals(2, serverConnMgr.getAllActiveConnections().length);

    conns = clientConnMgr.getAllConnections();
    assertEquals(2, conns.length);
    assertTrue(Arrays.asList(conns).containsAll(Arrays.asList(new Object[] { conn1, conn2 })));

    activeConns = clientConnMgr.getAllActiveConnections();
    assertEquals(2, activeConns.length);
    assertTrue(Arrays.asList(activeConns).containsAll(Arrays.asList(new Object[] { conn1, conn2 })));

    conn1.close(5000);
    conn2.close(5000);

    assertEquals(0, clientConnMgr.getAllConnections().length);
    assertEquals(0, clientConnMgr.getAllActiveConnections().length);

    while (serverConnMgr.getAllConnections().length > 0) {
      System.out.println("Waiting for client conns to close");
      ThreadUtil.reallySleep(500);
    }
    assertEquals(0, serverConnMgr.getAllActiveConnections().length);
  }

  public void testInActiveClientConnections() throws Exception {
    HealthCheckerConfig hcConfig = new HealthCheckerConfigImpl(1000, 1000, 5, "testInActiveClientConnections", false);
    this.serverConnMgr = new TCConnectionManagerImpl("TestConnMgr", 0, hcConfig);
    this.lsnr = this.serverConnMgr.createListener(new TCSocketAddress(0), new ProtocolAdaptorFactory() {
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
    assertEquals(2, clientConnMgr.getAllActiveConnections().length);

    while (serverConnMgr.getAllConnections().length < 2) {
      System.out.println("waiting for clients");
      ThreadUtil.reallySleep(500);
    }

    TCConnection[] conns = serverConnMgr.getAllConnections();
    for (TCConnection conn : conns) {
      conn.setTransportEstablished();
    }

    assertEquals(2, serverConnMgr.getAllActiveConnections().length);

    long sleepTime = ConnectionHealthCheckerUtil.getMaxIdleTimeForAlive(hcConfig, false) + 2000 /* buffer sleep time */;
    System.out.println("making client connections inactive. sleeping for " + sleepTime + "ms.");
    ThreadUtil.reallySleep(sleepTime);

    assertEquals(2, serverConnMgr.getAllConnections().length);
    assertEquals(0, serverConnMgr.getAllActiveConnections().length);
  }

}