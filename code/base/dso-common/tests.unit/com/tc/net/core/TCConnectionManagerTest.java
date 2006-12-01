/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.core;

import com.tc.net.TCSocketAddress;
import com.tc.net.protocol.NullProtocolAdaptor;
import com.tc.net.protocol.ProtocolAdaptorFactory;
import com.tc.net.protocol.TCProtocolAdaptor;

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
    this.clientConnMgr = new TCConnectionManagerFactory().getInstance();
    this.serverConnMgr = new TCConnectionManagerFactory().getInstance();
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

}