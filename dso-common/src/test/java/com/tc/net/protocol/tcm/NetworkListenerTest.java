/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.net.protocol.tcm;

import com.tc.net.TCSocketAddress;
import com.tc.net.protocol.PlainNetworkStackHarnessFactory;
import com.tc.net.protocol.transport.ConnectionIDFactory;
import com.tc.net.protocol.transport.DefaultConnectionIdFactory;
import com.tc.net.protocol.transport.NullConnectionPolicy;
import com.tc.object.session.NullSessionManager;
import com.tc.object.session.SessionProvider;
import com.tc.util.TCTimeoutException;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;

import junit.framework.TestCase;

/**
 * TODO: Document me
 * 
 * @author teck
 */
public class NetworkListenerTest extends TestCase {

  CommunicationsManager commsMgr;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    commsMgr = new CommunicationsManagerImpl("TestCommMgr", new NullMessageMonitor(),
                                             new PlainNetworkStackHarnessFactory(), new NullConnectionPolicy(), 0);
  }

  @Override
  public void tearDown() throws Exception {
    super.tearDown();

    if (commsMgr != null) {
      commsMgr.shutdown();
    }
  }

  public void testBindException() throws Exception {
    assertTrue(commsMgr.getAllListeners().length == 0);

    ConnectionIDFactory cidf = new DefaultConnectionIdFactory();
    SessionProvider sessionProvider = new NullSessionManager();
    NetworkListener lsnr = commsMgr.createListener(sessionProvider, new TCSocketAddress(0), true, cidf, false);

    try {
      lsnr.start(Collections.EMPTY_SET);
    } catch (IOException ioe) {
      fail(ioe.getMessage());
    }

    NetworkListener lsnr2 = commsMgr.createListener(sessionProvider, new TCSocketAddress(lsnr.getBindPort()), true,
                                                    cidf, false);
    try {
      lsnr2.start(Collections.EMPTY_SET);
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
      NetworkListener lsnr = commsMgr.createListener(new NullSessionManager(), new TCSocketAddress(InetAddress
          .getByName("127.0.0.1"), 0), true, new DefaultConnectionIdFactory());

      try {
        lsnr.start(Collections.EMPTY_SET);
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
