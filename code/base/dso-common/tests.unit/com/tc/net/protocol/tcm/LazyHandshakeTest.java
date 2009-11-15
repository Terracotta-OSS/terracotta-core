/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.tcm;

import com.tc.lang.TCThreadGroup;
import com.tc.lang.ThrowableHandler;
import com.tc.logging.TCLogging;
import com.tc.net.CommStackMismatchException;
import com.tc.net.MaxConnectionsExceededException;
import com.tc.net.TCSocketAddress;
import com.tc.net.core.ConnectionAddressProvider;
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

  private NetworkListener       listener;
  private ClientMessageChannel  channel[]          = new ClientMessageChannel[CLIENT_COUNT];
  private int                   currentClient      = 0;

  protected void setUp() throws Exception {
    super.setUp();
  }

  private void lazySetUp() {
    pc = new PortChooser();
    serverComms = new CommunicationsManagerImpl("TestCommsMgr", new NullMessageMonitor(), new PlainNetworkStackHarnessFactory(),
                                                new NullConnectionPolicy());
    clientComms = new CommunicationsManagerImpl("TestCommsMgr", new NullMessageMonitor(), new PlainNetworkStackHarnessFactory(),
                                                new NullConnectionPolicy());

    listener = serverComms.createListener(new NullSessionManager(), new TCSocketAddress(0), true,
                                          new DefaultConnectionIdFactory());

    try {
      listener.start(new HashSet());
    } catch (Exception e) {
      System.out.println("lsnr Excep");
    }

    proxyPort = pc.chooseRandomPort();
    proxy = new TCPProxy(proxyPort, listener.getBindAddress(), listener.getBindPort(), PROXY_SYNACK_DELAY, false, null);

    try {
      proxy.start();
    } catch (Exception e) {
      System.out.println("proxy lsnr Excep");
    }
  }

  private ClientMessageChannel createClientMessageChannel() {
    return clientComms
        .createClientChannel(new NullSessionManager(), 0, listener.getBindAddress().getHostAddress(), proxyPort,
                             (int) PROXY_SYNACK_DELAY,
                             new ConnectionAddressProvider(new ConnectionInfo[] { new ConnectionInfo(listener
                                 .getBindAddress().getHostAddress(), proxyPort) }));
  }

  protected void tearDown() throws Exception {
    super.tearDown();
  }

  public void testLazyHandshake() {
    TCThreadGroup threadGroup = new TCThreadGroup(new ThrowableHandler(TCLogging.getLogger(this.getClass())));
    // imitating TCGroupManager implementation of StaticMemberDiscovery on handshake timeouts

    Thread lazyThread = new Thread(threadGroup, new Runnable() {
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
        public void run() {
          channel[currentClient] = createClientMessageChannel();
          try {
            channel[currentClient].open();
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