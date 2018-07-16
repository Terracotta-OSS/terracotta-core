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
package com.tc.net.protocol.tcm;

import org.slf4j.LoggerFactory;

import com.tc.lang.TCThreadGroup;
import com.tc.lang.ThrowableHandlerImpl;
import com.tc.net.ClientID;
import com.tc.net.CommStackMismatchException;
import com.tc.net.MaxConnectionsExceededException;
import com.tc.net.TCSocketAddress;
import com.tc.net.core.ConnectionInfo;
import com.tc.net.protocol.PlainNetworkStackHarnessFactory;
import com.tc.net.protocol.transport.ClientMessageTransport;
import com.tc.net.protocol.transport.DefaultConnectionIdFactory;
import com.tc.net.protocol.transport.NullConnectionPolicy;
import com.tc.net.proxy.TCPProxy;
import com.tc.object.session.NullSessionManager;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;
import com.tc.util.PortChooser;
import com.tc.util.ProductID;
import com.tc.util.TCTimeoutException;
import com.tc.util.concurrent.ThreadUtil;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.HashSet;

/**
 * CDV-1320: Whenever there is a Transport Handshake timeout(10secs), the connected client transport is left as is and a
 * new connection is opened with the clients. If for any reason, any of the handshake messages arrive on the old
 * connection, we assert saying wrong state. This test tries to reproduce the same scenario and with the fix the test
 * should go through fine.
 * 
 * @author Manoj
 */

public class LazyHandshakeTest extends TCTestCase {

  // proxy timeouts : one-way
  private static final long     PROXY_SYNACK_DELAY = ClientMessageTransport.TRANSPORT_HANDSHAKE_SYNACK_TIMEOUT;
  private static final int      CLIENT_COUNT       = 3;

  private CommunicationsManager serverComms;
  private CommunicationsManager clientComms;
  private PortChooser           pc;
  private TCPProxy              proxy;
  private int                   proxyPort;
  private ConnectionInfo        connectTo;

  private NetworkListener       listener;
  private final ClientMessageChannel  channel[]          = new ClientMessageChannel[CLIENT_COUNT];
  private int                   currentClient      = 0;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
  }

  private void lazySetUp() {
    pc = new PortChooser();
    serverComms = new CommunicationsManagerImpl("TestCommsMgr", new NullMessageMonitor(), new PlainNetworkStackHarnessFactory(),
                                                new NullConnectionPolicy());
    clientComms = new CommunicationsManagerImpl("TestCommsMgr", new NullMessageMonitor(), new PlainNetworkStackHarnessFactory(),
                                                new NullConnectionPolicy());

    listener = serverComms.createListener(new TCSocketAddress(0), true,
                                          new DefaultConnectionIdFactory(), (t)->true);

    try {
      listener.start(new HashSet<ClientID>());
    } catch (Exception e) {
      System.out.println("lsnr Excep");
    }

    proxyPort = pc.chooseRandomPort();
    proxy = new TCPProxy(proxyPort, listener.getBindAddress(), listener.getBindPort(), PROXY_SYNACK_DELAY, false, null);

    connectTo = new ConnectionInfo(listener
                                 .getBindAddress().getHostAddress(), proxyPort);
    try {
      proxy.start();
    } catch (Exception e) {
      System.out.println("proxy lsnr Excep");
    }
  }

  private ClientMessageChannel createClientMessageChannel() {
    return clientComms
        .createClientChannel(ProductID.STRIPE, new NullSessionManager(),
                             (int) PROXY_SYNACK_DELAY);
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  public void testLazyHandshake() {
    TCThreadGroup threadGroup = new TCThreadGroup(new ThrowableHandlerImpl(LoggerFactory.getLogger(this.getClass())));
    // imitating TCGroupManager implementation of StaticMemberDiscovery on handshake timeouts

    Thread lazyThread = new Thread(threadGroup, new Runnable() {
      @Override
      public void run() {
        lazySetUp();
        handshaker();
      }
    });

    lazyThread.start();
    try {
      lazyThread.join();
    } catch (InterruptedException e) {
      System.out.println("Received the UNexpected exception: " + e);
    }
    ThreadUtil.reallySleep(3 * PROXY_SYNACK_DELAY);
    Assert.eval((channel[0].getConnectCount() + channel[1].getConnectCount() + channel[2].getConnectCount()) == 0);

  }

  private void handshaker() {
    for (currentClient = 0; currentClient < 3; currentClient++) {
      System.out.println("Connecting to peer node. Attempt :" + currentClient);
      Thread t = new Thread(new Runnable() {
        @Override
        public void run() {
          channel[currentClient] = createClientMessageChannel();
          try {
            channel[currentClient].open(connectTo);
          } catch (UnknownHostException e) {
            // who am I, then
          } catch (MaxConnectionsExceededException e) {
            // so what
          } catch (CommStackMismatchException e) {
            // so what
          } catch (IOException e) {
            // IO-OI
          } catch (TCTimeoutException e) {
            // the expected baby
            System.out.println("Received the expected exception: " + e);
          }
        }
      }, "Lazy Handshaker");

      t.start();
      try {
        t.join();
      } catch (InterruptedException e) {
        System.out.println("Received the UNexpected exception: " + e);
      }
    }
  }
}
