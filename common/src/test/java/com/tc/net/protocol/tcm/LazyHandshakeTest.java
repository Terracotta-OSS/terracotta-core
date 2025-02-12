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

import org.slf4j.LoggerFactory;
import org.terracotta.utilities.test.net.PortManager;

import com.tc.lang.TCThreadGroup;
import com.tc.lang.ThrowableHandlerImpl;
import com.tc.net.CommStackMismatchException;
import com.tc.net.MaxConnectionsExceededException;
import com.tc.net.core.ClearTextSocketEndpointFactory;
import com.tc.net.core.TCConnectionManager;
import com.tc.net.core.TCConnectionManagerImpl;
import com.tc.net.protocol.PlainNetworkStackHarnessFactory;
import com.tc.net.protocol.transport.ClientMessageTransport;
import com.tc.net.protocol.transport.ConnectionID;
import com.tc.net.protocol.transport.DefaultConnectionIdFactory;
import com.tc.net.protocol.transport.MessageTransport;
import com.tc.net.protocol.transport.NullConnectionPolicy;
import com.tc.net.proxy.TCPProxy;
import com.tc.net.core.ProductID;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;
import com.tc.util.TCTimeoutException;
import com.tc.util.concurrent.ThreadUtil;

import java.io.IOException;
import java.net.InetSocketAddress;
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

  private TCConnectionManager   serverConn;
  private TCConnectionManager   clientConn;
  private CommunicationsManager serverComms;
  private CommunicationsManager clientComms;
  private TCPProxy              proxy;
  private InetSocketAddress serverAddress;

  private NetworkListener       listener;
  private final ClientMessageChannel  channel[]          = new ClientMessageChannel[CLIENT_COUNT];
  private int                   currentClient      = 0;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
  }

  private void lazySetUp(int proxyPort) {
    serverConn = new TCConnectionManagerImpl("Server-Connections",  0, new ClearTextSocketEndpointFactory());
    clientConn = new TCConnectionManagerImpl("Client-Connections", 0, new ClearTextSocketEndpointFactory());
    serverComms = new CommunicationsManagerImpl(new NullMessageMonitor(), new PlainNetworkStackHarnessFactory(),
                                                serverConn,
                                                new NullConnectionPolicy());
    clientComms = new CommunicationsManagerImpl(new NullMessageMonitor(), new PlainNetworkStackHarnessFactory(),
            clientConn,
            new NullConnectionPolicy());

    listener = serverComms.createListener(new InetSocketAddress(0), (c)->true,
                                          new DefaultConnectionIdFactory(), (MessageTransport t)->true);

    try {
      listener.start(new HashSet<ConnectionID>());
    } catch (Exception e) {
      System.out.println("lsnr Excep");
    }

    proxy = new TCPProxy(proxyPort, listener.getBindAddress(), listener.getBindPort(), PROXY_SYNACK_DELAY, false, null);

    serverAddress = InetSocketAddress.createUnresolved(listener.getBindAddress().getHostAddress(), proxyPort);
    try {
      proxy.start();
    } catch (Exception e) {
      System.out.println("proxy lsnr Excep");
    }
  }

  private ClientMessageChannel createClientMessageChannel() {
    return clientComms
        .createClientChannel(ProductID.STRIPE,
                             (int) PROXY_SYNACK_DELAY);
  }

  @Override
  protected void tearDown() throws Exception {
    try {
      listener.stop(5000);
    } catch (TCTimeoutException e) {
      // ignored
    }
    clientComms.shutdown();
    serverComms.shutdown();
    clientConn.shutdown();
    serverConn.shutdown();
    super.tearDown();
  }

  public void testLazyHandshake() {
    TCThreadGroup threadGroup = new TCThreadGroup(new ThrowableHandlerImpl(LoggerFactory.getLogger(this.getClass())));
    // imitating TCGroupManager implementation of StaticMemberDiscovery on handshake timeouts

    try (PortManager.PortRef portRef = PortManager.getInstance().reservePort()) {
      Thread lazyThread = new Thread(threadGroup, new Runnable() {
        @Override
        public void run() {
          lazySetUp(portRef.port());
          handshaker();
        }
      });

      try {
        lazyThread.start();
        try {
          lazyThread.join();
        } catch (InterruptedException e) {
          System.out.println("Received the UNexpected exception: " + e);
        }
        ThreadUtil.reallySleep(3 * PROXY_SYNACK_DELAY);
        Assert.eval((channel[0].getConnectCount() + channel[1].getConnectCount() + channel[2].getConnectCount()) == 0);
      } finally {
        if (proxy != null) {
          proxy.stop();
        }
      }
    }
  }

  private void handshaker() {
    for (currentClient = 0; currentClient < 3; currentClient++) {
      System.out.println("Connecting to peer node. Attempt :" + currentClient);
      Thread t = new Thread(new Runnable() {
        @Override
        public void run() {
          channel[currentClient] = createClientMessageChannel();
          try {
            channel[currentClient].open(serverAddress);
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
