/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
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
package com.tc.net.protocol.tcm;

import com.tc.net.core.ClearTextSocketEndpointFactory;
import com.tc.net.core.TCConnectionManager;
import com.tc.net.core.TCConnectionManagerImpl;
import com.tc.net.protocol.PlainNetworkStackHarnessFactory;
import com.tc.net.protocol.transport.ConnectionID;
import com.tc.net.protocol.transport.ConnectionIDFactory;
import com.tc.net.protocol.transport.DefaultConnectionIdFactory;
import com.tc.net.protocol.transport.MessageTransport;
import com.tc.net.protocol.transport.NullConnectionPolicy;
import com.tc.util.TCTimeoutException;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Collections;

import junit.framework.TestCase;

/**
 * TODO: Document me
 * 
 * @author teck
 */
public class NetworkListenerTest extends TestCase {

  TCConnectionManager connMgr;
  CommunicationsManager commsMgr;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    connMgr = new TCConnectionManagerImpl("TestCommMgr", 0, new ClearTextSocketEndpointFactory());
    commsMgr = new CommunicationsManagerImpl(new NullMessageMonitor(),
                                             new PlainNetworkStackHarnessFactory(), connMgr, new NullConnectionPolicy());
  }

  @Override
  public void tearDown() throws Exception {
    super.tearDown();

    if (commsMgr != null) {
      commsMgr.shutdown();
    }
    if (connMgr != null) {
      connMgr.shutdown();
    }
  }

  public void testBindException() throws Exception {
    assertTrue(commsMgr.getAllListeners().length == 0);

    ConnectionIDFactory cidf = new DefaultConnectionIdFactory();
    NetworkListener lsnr = commsMgr.createListener(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), (c)->true, cidf, (MessageTransport t)->true);

    try {
      lsnr.start(Collections.<ConnectionID>emptySet());
    } catch (IOException ioe) {
      fail(ioe.getMessage());
    }

    NetworkListener lsnr2 = commsMgr.createListener(new InetSocketAddress(InetAddress.getLoopbackAddress(), lsnr.getBindPort()), (c)->true, cidf, (MessageTransport t)->true);
    try {
      lsnr2.start(Collections.<ConnectionID>emptySet());
      // NOTE (issue-529):  When running on Windows, in a pre-Java7u25 JVM, this bind succeeds.
      if (isWindows() && isJava6()) {
        System.err.println("WARNING:  bind success due to lack of SO_EXCLUSIVEADDRUSE - ignoring test failure");
        lsnr2.stop();
      } else {
        fail();
      }
    } catch (IOException ioe) {
      // expect a bind exception
    }

    assertTrue(commsMgr.getAllListeners().length == 1);

    lsnr.stop();

    assertTrue(commsMgr.getAllListeners().length == 0);
  }

  public void testMany() throws UnknownHostException, TCTimeoutException {
    assertTrue(commsMgr.getAllListeners().length == 0);

    final int cnt = 20;
    NetworkListener[] listeners = new NetworkListener[cnt];

    for (int i = 0; i < cnt; i++) {
      NetworkListener lsnr = commsMgr.createListener(new InetSocketAddress(InetAddress
          .getByName("127.0.0.1"), 0), (c)->true, new DefaultConnectionIdFactory(), (MessageTransport t)->true);

      try {
        lsnr.start(Collections.<ConnectionID>emptySet());
        listeners[i] = lsnr;
      } catch (IOException ioe) {
        fail(ioe.getMessage());
      }
    }

    assertTrue(commsMgr.getAllListeners().length == cnt);

    for (int i = 0; i < cnt; i++) {
      // try stop() twice, shouldn't fail

      listeners[i].stop();
      listeners[i].stop();
    }

    assertTrue(commsMgr.getAllListeners().length == 0);
  }

  private static boolean isWindows() {
    return (-1 != System.getProperty("os.name").toLowerCase().indexOf("windows"));
  }

  private static boolean isJava6() {
    return (0 == System.getProperty("java.version").toLowerCase().indexOf("1.6"));
  }
}
