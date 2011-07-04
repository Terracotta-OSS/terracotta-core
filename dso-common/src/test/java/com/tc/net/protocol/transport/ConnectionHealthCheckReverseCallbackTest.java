/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

import EDU.oswego.cs.dl.util.concurrent.WaitableInt;

import com.tc.logging.LogLevelImpl;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.TCSocketAddress;
import com.tc.net.core.ConnectionAddressProvider;
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
import com.tc.util.concurrent.ThreadUtil;

import java.io.IOException;
import java.util.Collections;

public class ConnectionHealthCheckReverseCallbackTest extends TCTestCase {

  static {
    TCLogging.getLogger(ConnectionHealthCheckerImpl.class).setLevel(LogLevelImpl.DEBUG);
  }

  private final TCLogger            logger               = TCLogging.getLogger(getClass());

  private final PortChooser         pc                   = new PortChooser();
  private final int                 listenPort           = pc.chooseRandomPort();
  private final int                 proxyPort            = pc.chooseRandomPort();

  // private final int clientListenPort = pc.chooseRandomPort();

  private final TCPProxy            proxy                = new TCPProxy(proxyPort, TCSocketAddress.LOOPBACK_ADDR,
                                                                        listenPort, 0, false, null);

  private final WaitableInt         serverRecvCount      = new WaitableInt(0);
  private final WaitableInt         serverSocketConnects = new WaitableInt(0);

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
                                                                     0);

    clientComms = new CommunicationsManagerImpl("TestCommsMgr-Client", new NullMessageMonitor(),
                                                new PlainNetworkStackHarnessFactory(), new NullConnectionPolicy(),
                                                clientHC);

    TCConnectionManager serverConnMgr = new MyConnectionManager(serverSocketConnects);

    serverComms = new CommunicationsManagerImpl("TestCommsMgr-Server", new NullMessageMonitor(),
                                                new TCMessageRouterImpl(), new PlainNetworkStackHarnessFactory(),
                                                serverConnMgr, new NullConnectionPolicy(), 0, serverHC,
                                                new TransportHandshakeErrorNullHandler(), Collections.EMPTY_MAP,
                                                Collections.EMPTY_MAP);
    String host = "localhost";

    serverComms.addClassMapping(TCMessageType.PING_MESSAGE, PingMessage.class);
    (serverComms).getMessageRouter().routeMessageType(TCMessageType.PING_MESSAGE, new TCMessageSink() {
      public void putMessage(TCMessage message) {
        logger.info("server recv: " + message.getMessageType().getTypeName());
        serverRecvCount.increment();
      }
    });

    NetworkListener listener = serverComms
        .createListener(new NullSessionManager(), new TCSocketAddress(TCSocketAddress.WILDCARD_ADDR, listenPort), true,
                        new DefaultConnectionIdFactory());

    listener.start(Collections.EMPTY_SET);

    ConnectionAddressProvider addrProvider = new ConnectionAddressProvider(
                                                                           new ConnectionInfo[] { new ConnectionInfo(
                                                                                                                     host,
                                                                                                                     proxyPort) });

    clientComms.addClassMapping(TCMessageType.PING_MESSAGE, PingMessage.class);
    channel = clientComms.createClientChannel(new NullSessionManager(), -1, host, proxyPort, 30000, addrProvider);
    channel.open();
  }

  public void testReverseCallback() throws Exception {
    int numPings = 5;
    for (int i = 0; i < numPings; i++) {
      channel.createMessage(TCMessageType.PING_MESSAGE).send();
    }

    serverRecvCount.whenEqual(numPings, null);

    // prevent all ping probes from flowing (in both directions)
    proxy.setDelay(Long.MAX_VALUE);

    // observe some socket connects (which will succeed)
    serverSocketConnects.whenGreaterEqual(3, null);

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
    private final WaitableInt             connects;

    MyConnectionManager(WaitableInt serverSocketConnects) {
      this.delegate = new TCConnectionManagerImpl();
      this.connects = serverSocketConnects;
    }

    public void asynchCloseAllConnections() {
      delegate.asynchCloseAllConnections();
    }

    public void closeAllConnections(long timeout) {
      delegate.closeAllConnections(timeout);
    }

    public void closeAllListeners() {
      delegate.closeAllListeners();
    }

    public final TCConnection createConnection(TCProtocolAdaptor adaptor) {
      connects.increment();
      return delegate.createConnection(adaptor);
    }

    public final TCListener createListener(TCSocketAddress addr, ProtocolAdaptorFactory factory, int backlog,
                                           boolean reuseAddr) throws IOException {
      return delegate.createListener(addr, factory, backlog, reuseAddr);
    }

    public final TCListener createListener(TCSocketAddress addr, ProtocolAdaptorFactory factory) throws IOException {
      return delegate.createListener(addr, factory);
    }

    public TCConnection[] getAllConnections() {
      return delegate.getAllConnections();
    }

    public TCListener[] getAllListeners() {
      return delegate.getAllListeners();
    }

    public TCComm getTcComm() {
      return delegate.getTcComm();
    }

    public final void shutdown() {
      delegate.shutdown();
    }

    public TCConnection[] getAllActiveConnections() {
      return delegate.getAllConnections();
    }
  }

}
