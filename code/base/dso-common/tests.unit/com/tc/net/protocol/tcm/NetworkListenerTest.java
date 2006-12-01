/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.protocol.tcm;

import com.tc.net.TCSocketAddress;
import com.tc.net.protocol.PlainNetworkStackHarnessFactory;
import com.tc.net.protocol.transport.ConnectionIdFactory;
import com.tc.net.protocol.transport.DefaultConnectionIdFactory;
import com.tc.net.protocol.transport.NullConnectionPolicy;
import com.tc.object.session.NullSessionManager;
import com.tc.object.session.SessionProvider;
import com.tc.util.TCTimeoutException;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;

import junit.framework.TestCase;

/**
 * TODO: Document me
 * 
 * @author teck
 */
public class NetworkListenerTest extends TestCase {

  CommunicationsManager commsMgr;

  public void setUp() throws Exception {
    super.setUp();
    commsMgr = new CommunicationsManagerImpl(new NullMessageMonitor(), new PlainNetworkStackHarnessFactory(), new NullConnectionPolicy());
  }

  public void tearDown() throws Exception {
    super.tearDown();

    if (commsMgr != null) {
      commsMgr.shutdown();
    }
  }

  public void testBindException() throws Exception {
    assertTrue(commsMgr.getAllListeners().length == 0);

    ConnectionIdFactory cidf = new DefaultConnectionIdFactory();
    SessionProvider sessionProvider = new NullSessionManager();
    NetworkListener lsnr = commsMgr.createListener(sessionProvider, new TCSocketAddress(0), true, new HashSet(), cidf, false);

    try {
      lsnr.start();
    } catch (IOException ioe) {
      fail(ioe.getMessage());
    }

    NetworkListener lsnr2 = commsMgr.createListener(sessionProvider, new TCSocketAddress(lsnr.getBindPort()), true, new HashSet(), cidf,
                                                    false);
    try {
      lsnr2.start();
      fail();
    } catch (IOException ioe) {
      // expect a bind exception
    }

    assertTrue(commsMgr.getAllListeners().length == 1);

    lsnr.stop(5000);

    assertTrue(commsMgr.getAllListeners().length == 0);
  }

  public void testMany() throws UnknownHostException, TCTimeoutException {
    assertTrue(commsMgr.getAllListeners().length == 0);

    final int cnt = 20;
    NetworkListener[] listeners = new NetworkListener[cnt];

    for (int i = 0; i < cnt; i++) {
      NetworkListener lsnr = commsMgr.createListener(new NullSessionManager(), new TCSocketAddress(InetAddress.getByName("127.0.0.1"), 0), true,
                                                     new HashSet(), new DefaultConnectionIdFactory());

      try {
        lsnr.start();
        listeners[i] = lsnr;
      } catch (IOException ioe) {
        fail(ioe.getMessage());
      }
    }

    assertTrue(commsMgr.getAllListeners().length == cnt);

    for (int i = 0; i < cnt; i++) {
      // try stop() twice, shouldn't fail

      listeners[i].stop(5000);
      listeners[i].stop(5000);
    }

    assertTrue(commsMgr.getAllListeners().length == 0);
  }

}
