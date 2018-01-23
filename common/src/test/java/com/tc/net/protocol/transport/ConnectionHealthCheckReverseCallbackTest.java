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
package com.tc.net.protocol.transport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tc.net.ClientID;
import com.tc.net.TCSocketAddress;
import com.tc.net.core.ConnectionInfo;
import com.tc.net.core.TCComm;
import com.tc.net.core.TCConnection;
import com.tc.net.core.TCConnectionManager;
import com.tc.net.core.TCConnectionManagerImpl;
import com.tc.net.core.TCListener;
import com.tc.net.protocol.PlainNetworkStackHarnessFactory;
import com.tc.net.protocol.ProtocolAdaptorFactory;
import com.tc.net.protocol.TCProtocolAdaptor;
import com.tc.net.protocol.tcm.ClientMessageChannel;
import com.tc.net.protocol.tcm.CommunicationsManagerImpl;
import com.tc.net.protocol.tcm.GeneratedMessageFactory;
import com.tc.net.protocol.tcm.NetworkListener;
import com.tc.net.protocol.tcm.NullMessageMonitor;
import com.tc.net.protocol.tcm.TCMessage;
import com.tc.net.protocol.tcm.TCMessageRouterImpl;
import com.tc.net.protocol.tcm.TCMessageSink;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.net.protocol.tcm.msgs.PingMessage;
import com.tc.net.proxy.TCPProxy;
import com.tc.object.session.NullSessionManager;
import com.tc.test.TCTestCase;
import com.tc.util.PortChooser;
import com.tc.util.ProductID;
import com.tc.util.concurrent.ThreadUtil;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class ConnectionHealthCheckReverseCallbackTest extends TCTestCase {

  private final Logger logger = LoggerFactory.getLogger(getClass());

  private final PortChooser         pc                   = new PortChooser();
  private final int                 listenPort           = pc.chooseRandomPort();
  private final int                 proxyPort            = pc.chooseRandomPort();
  private static final int          numPings             = 5;

  // private final int clientListenPort = pc.chooseRandomPort();

  private final TCPProxy            proxy                = new TCPProxy(proxyPort, TCSocketAddress.LOOPBACK_ADDR,
                                                                        listenPort, 0, false, null);

  private final CountDownLatch      serverRecvCount      = new CountDownLatch(numPings);
  private final CountDownLatch      serverSocketConnects = new CountDownLatch(3);

  private CommunicationsManagerImpl clientComms;
  private CommunicationsManagerImpl serverComms;
  private ClientMessageChannel      channel;
  private HealthCheckerConfig       serverHC;

  private static final int          SERVER_PING_PROBES   = 5;
  private static final long         SERVER_PING_INTERVAL = 3000;
  private static final long         SERVER_PING_IDLE     = 3000;

  @Override
  protected void tearDown() throws Exception {
    proxy.stop();
    super.tearDown();
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    proxy.start();

    serverHC = new HealthCheckerConfigImpl(SERVER_PING_IDLE, SERVER_PING_INTERVAL, SERVER_PING_PROBES, "server-HC",
                                           true, Integer.MAX_VALUE, 2);
    HealthCheckerConfig clientHC = new HealthCheckerConfigClientImpl(1000, 1000, Integer.MAX_VALUE, "client-HC", false,
                                                                     Integer.MAX_VALUE, 2, TCSocketAddress.WILDCARD_IP,
                                                                     "0");

    clientComms = new CommunicationsManagerImpl("TestCommsMgr-Client", new NullMessageMonitor(),
                                                new PlainNetworkStackHarnessFactory(), new NullConnectionPolicy(),
                                                clientHC);

    TCConnectionManager serverConnMgr = new MyConnectionManager(serverSocketConnects);

    serverComms = new CommunicationsManagerImpl("TestCommsMgr-Server", new NullMessageMonitor(),
                                                new TCMessageRouterImpl(), new PlainNetworkStackHarnessFactory(),
                                                serverConnMgr, new NullConnectionPolicy(), 0, serverHC,
                                                new TransportHandshakeErrorNullHandler(),
                                                Collections.<TCMessageType, Class<? extends TCMessage>>emptyMap(),
                                                Collections.<TCMessageType, GeneratedMessageFactory>emptyMap());
    String host = "localhost";

    serverComms.addClassMapping(TCMessageType.PING_MESSAGE, PingMessage.class);
    (serverComms).getMessageRouter().routeMessageType(TCMessageType.PING_MESSAGE, new TCMessageSink() {
      @Override
      public void putMessage(TCMessage message) {
        logger.info("server recv: " + message.getMessageType().getTypeName());
        serverRecvCount.countDown();
      }
    });

    NetworkListener listener = serverComms
        .createListener(new TCSocketAddress(TCSocketAddress.WILDCARD_ADDR, listenPort), true,
                        new DefaultConnectionIdFactory());

    listener.start(Collections.<ClientID>emptySet());
    
    clientComms.addClassMapping(TCMessageType.PING_MESSAGE, PingMessage.class);
    channel = clientComms.createClientChannel(ProductID.STRIPE, new NullSessionManager(),30000);
    try {
      channel.open(new ConnectionInfo(host, proxyPort));
    } catch (Throwable t) {
      t.printStackTrace();
      throw new RuntimeException(t);
    }
  }

  public void testReverseCallback() throws Exception {

    for (int i = 0; i < numPings; i++) {
      channel.createMessage(TCMessageType.PING_MESSAGE).send();
    }


    serverRecvCount.await();

    // prevent all ping probes from flowing (in both directions)
    proxy.setDelay(Long.MAX_VALUE);

    // observe some socket connects (which will succeed)
    serverSocketConnects.await();

    assertEquals(1, numServerConnections());

    clientComms.getCallbackPortListener().stop(Long.MAX_VALUE);

    // with the listener stopped, the socket connect will fail and server side health check
    // will consider the client to be DEAD
    ThreadUtil.reallySleep(SERVER_PING_INTERVAL * (SERVER_PING_PROBES * 2));

    assertEquals(0, numServerConnections());
  }

  private int numServerConnections() {
    return ((ConnectionHealthCheckerImpl) serverComms.getConnHealthChecker()).getTotalConnsUnderMonitor();
  }

  private static class MyConnectionManager implements TCConnectionManager {
    private final TCConnectionManagerImpl delegate;
    private final CountDownLatch          connects;

    MyConnectionManager(CountDownLatch serverSocketConnects) {
      this.delegate = new TCConnectionManagerImpl();
      this.connects = serverSocketConnects;
    }

    @Override
    public void asynchCloseAllConnections() {
      delegate.asynchCloseAllConnections();
    }

    @Override
    public void closeAllConnections(long timeout) {
      delegate.closeAllConnections(timeout);
    }

    @Override
    public void closeAllListeners() {
      delegate.closeAllListeners();
    }

    @Override
    public final TCConnection createConnection(TCProtocolAdaptor adaptor) {
      connects.countDown();
      return delegate.createConnection(adaptor);
    }

    @Override
    public final TCListener createListener(TCSocketAddress addr, ProtocolAdaptorFactory factory, int backlog,
                                           boolean reuseAddr) throws IOException {
      return delegate.createListener(addr, factory, backlog, reuseAddr);
    }

    @Override
    public final TCListener createListener(TCSocketAddress addr, ProtocolAdaptorFactory factory) throws IOException {
      return delegate.createListener(addr, factory);
    }

    @Override
    public TCConnection[] getAllConnections() {
      return delegate.getAllConnections();
    }

    @Override
    public TCListener[] getAllListeners() {
      return delegate.getAllListeners();
    }

    @Override
    public TCComm getTcComm() {
      return delegate.getTcComm();
    }

    @Override
    public final void shutdown() {
      delegate.shutdown();
    }

    @Override
    public TCConnection[] getAllActiveConnections() {
      return delegate.getAllConnections();
    }

    @Override
    public Map<String, ?> getStateMap() {
      return Collections.emptyMap();
    }
    
    
  }

}
