/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.logging.LogLevelImpl;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.TCSocketAddress;
import com.tc.net.core.ConnectionAddressProvider;
import com.tc.net.core.ConnectionInfo;
import com.tc.net.protocol.NetworkStackHarnessFactory;
import com.tc.net.protocol.PlainNetworkStackHarnessFactory;
import com.tc.net.protocol.delivery.OOONetworkStackHarnessFactory;
import com.tc.net.protocol.delivery.OnceAndOnlyOnceProtocolNetworkLayerFactoryImpl;
import com.tc.net.protocol.tcm.ClientMessageChannel;
import com.tc.net.protocol.tcm.CommunicationsManager;
import com.tc.net.protocol.tcm.CommunicationsManagerImpl;
import com.tc.net.protocol.tcm.NetworkListener;
import com.tc.net.protocol.tcm.NullMessageMonitor;
import com.tc.net.protocol.tcm.TCMessage;
import com.tc.net.protocol.tcm.TCMessageRouter;
import com.tc.net.protocol.tcm.TCMessageRouterImpl;
import com.tc.net.protocol.tcm.TCMessageSink;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.net.protocol.tcm.UnsupportedMessageTypeException;
import com.tc.net.protocol.tcm.msgs.PingMessage;
import com.tc.net.proxy.TCPProxy;
import com.tc.object.session.NullSessionManager;
import com.tc.properties.L1ReconnectConfigImpl;
import com.tc.test.TCTestCase;
import com.tc.util.PortChooser;
import com.tc.util.SequenceGenerator;
import com.tc.util.concurrent.ThreadUtil;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;

public class ConnectionHealthCheckerReconnectTest extends TCTestCase {

  CommunicationsManager                                   serverComms;
  CommunicationsManager                                   clientComms;

  TCMessageRouter                                         serverMessageRouter;
  TCMessageRouter                                         clientMessageRouter;

  NetworkListener                                         serverLsnr;
  TCLogger                                                logger       = TCLogging
                                                                           .getLogger(ConnectionHealthCheckerImpl.class);
  TCPProxy                                                proxy        = null;
  int                                                     proxyPort    = 0;
  private final LinkedBlockingQueue<ClientMessageChannel> channelQueue = new LinkedBlockingQueue<ClientMessageChannel>();

  protected void setUp(HealthCheckerConfig serverHCConf, HealthCheckerConfig clientHCConf) throws Exception {
    super.setUp();

    NetworkStackHarnessFactory networkStackHarnessFactory;

    logger.setLevel(LogLevelImpl.DEBUG);

    serverMessageRouter = new TCMessageRouterImpl();
    clientMessageRouter = new TCMessageRouterImpl();

    if (true /* TCPropertiesImpl.getProperties().getBoolean(L1ReconnectProperties.L1_RECONNECT_ENABLED) */) {
      networkStackHarnessFactory = new OOONetworkStackHarnessFactory(
                                                                     new OnceAndOnlyOnceProtocolNetworkLayerFactoryImpl(),
                                                                     new L1ReconnectConfigImpl(true, 120000, 5000, 16,
                                                                                               32));
    } else {
      networkStackHarnessFactory = new PlainNetworkStackHarnessFactory();
    }

    if (serverHCConf != null) {
      serverComms = new CommunicationsManagerImpl("TestCommsMgr-Server", new NullMessageMonitor(), serverMessageRouter,
                                                  networkStackHarnessFactory, new NullConnectionPolicy(), serverHCConf,
                                                  Collections.EMPTY_MAP, Collections.EMPTY_MAP);
    } else {
      serverComms = new CommunicationsManagerImpl("TestCommsMgr-Server", new NullMessageMonitor(), serverMessageRouter,
                                                  networkStackHarnessFactory, new NullConnectionPolicy(),
                                                  new DisabledHealthCheckerConfigImpl(), Collections.EMPTY_MAP,
                                                  Collections.EMPTY_MAP);
    }

    if (clientHCConf != null) {
      clientComms = new CommunicationsManagerImpl("TestCommsMgr-Client", new NullMessageMonitor(), clientMessageRouter,
                                                  networkStackHarnessFactory, new NullConnectionPolicy(), clientHCConf,
                                                  Collections.EMPTY_MAP, Collections.EMPTY_MAP);
    } else {
      clientComms = new CommunicationsManagerImpl("TestCommsMgr-Client", new NullMessageMonitor(), clientMessageRouter,
                                                  networkStackHarnessFactory, new NullConnectionPolicy(),
                                                  new DisabledHealthCheckerConfigImpl(), Collections.EMPTY_MAP,
                                                  Collections.EMPTY_MAP);

    }

    serverComms.addClassMapping(TCMessageType.PING_MESSAGE, PingMessage.class);
    ((CommunicationsManagerImpl) serverComms).getMessageRouter().routeMessageType(TCMessageType.PING_MESSAGE,
                                                                                  new TCMessageSink() {

                                                                                    public void putMessage(TCMessage message)
                                                                                        throws UnsupportedMessageTypeException {

                                                                                      PingMessage pingMsg = (PingMessage) message;
                                                                                      try {
                                                                                        pingMsg.hydrate();
                                                                                        System.out
                                                                                            .println("Server RECEIVE - PING seq no "
                                                                                                     + pingMsg
                                                                                                         .getSequence());
                                                                                      } catch (Exception e) {
                                                                                        System.out
                                                                                            .println("Server Exception during PingMessage hydrate:");
                                                                                        e.printStackTrace();
                                                                                      }

                                                                                      PingMessage pingRplyMsg = pingMsg
                                                                                          .createResponse();
                                                                                      pingRplyMsg.send();
                                                                                    }
                                                                                  });

    serverLsnr = serverComms.createListener(new NullSessionManager(), new TCSocketAddress(0), false,
                                            new DefaultConnectionIdFactory());

    serverLsnr.start(new HashSet());

    int serverPort = serverLsnr.getBindPort();
    proxyPort = new PortChooser().chooseRandomPort();
    proxy = new TCPProxy(proxyPort, serverLsnr.getBindAddress(), serverPort, 0, false, null);
    proxy.start();
  }

  ClientMessageChannel createClientMsgCh() {
    return createClientMsgChProxied(null);
  }

  ClientMessageChannel createClientMsgChProxied(CommunicationsManager clientCommsMgr) {

    CommunicationsManager commsMgr = (clientCommsMgr == null ? clientComms : clientCommsMgr);

    commsMgr.addClassMapping(TCMessageType.PING_MESSAGE, PingMessage.class);
    clientMessageRouter.routeMessageType(TCMessageType.PING_MESSAGE, new TCMessageSink() {

      public void putMessage(TCMessage message) throws UnsupportedMessageTypeException {
        PingMessage pingMsg = (PingMessage) message;
        try {
          pingMsg.hydrate();
          System.out.println(" Client RECEIVE - PING seq no " + pingMsg.getSequence());
        } catch (Exception e) {
          System.out.println("Client Exception during PingMessage hydrate:");
          e.printStackTrace();
        }
      }
    });
    ClientMessageChannel clientMsgCh = commsMgr
        .createClientChannel(new NullSessionManager(), 0, serverLsnr.getBindAddress().getHostAddress(), proxyPort,
                             1000, new ConnectionAddressProvider(new ConnectionInfo[] { new ConnectionInfo(serverLsnr
                                 .getBindAddress().getHostAddress(), proxyPort) }));

    return clientMsgCh;
  }

  public long getMinSleepTimeToSendFirstProbe(HealthCheckerConfig config) {
    assertNotNull(config);
    /* Interval time is doubled to give grace period */
    return config.getPingIdleTimeMillis() + (2 * config.getPingIntervalMillis());
  }

  public long getMinSleepTimeToConirmDeath(HealthCheckerConfig config) {
    assertNotNull(config);
    /* Interval time is doubled to give grace period */
    long exact_time = config.getPingIdleTimeMillis() + (config.getPingIntervalMillis() * config.getPingProbes());
    long grace_time = config.getPingIntervalMillis();
    return (exact_time + grace_time);
  }

  public void testL1DisconnectAndL1Reconnect() throws Exception {
    HealthCheckerConfig hcConfig = new HealthCheckerConfigImpl(10000, 4000, 2, "ServerCommsHC-Test11", false);
    this.setUp(hcConfig, null);
    ClientMessageChannel clientMsgCh = createClientMsgCh();
    clientMsgCh.open();

    // Verifications
    ConnectionHealthCheckerImpl connHC = (ConnectionHealthCheckerImpl) ((CommunicationsManagerImpl) serverComms)
        .getConnHealthChecker();
    assertNotNull(connHC);

    while (!connHC.isRunning() && (connHC.getTotalConnsUnderMonitor() != 1)) {
      System.out.println("Yet to start the connection health cheker thread...");
      ThreadUtil.reallySleep(1000);
    }

    SequenceGenerator sq = new SequenceGenerator();
    for (int i = 1; i <= 5; i++) {
      PingMessage ping = (PingMessage) clientMsgCh.createMessage(TCMessageType.PING_MESSAGE);
      ping.initialize(sq);
      ping.send();
    }

    System.out.println("Sleeping for " + getMinSleepTimeToSendFirstProbe(hcConfig));
    ThreadUtil.reallySleep(getMinSleepTimeToSendFirstProbe(hcConfig));
    System.out.println("Successfully sent " + connHC.getTotalProbesSentOnAllConns() + " Probes");
    assertTrue(connHC.getTotalProbesSentOnAllConns() > 0);

    proxy.closeClientConnections(true, true);
    System.out.println("Client Socket Closed by proxy");

    System.out.println("Sleeping for " + getMinSleepTimeToConirmDeath(hcConfig));
    ThreadUtil.reallySleep(getMinSleepTimeToConirmDeath(hcConfig));

    /* By Now, the client would have reconnected */
    assertEquals(1, connHC.getTotalConnsUnderMonitor());

    proxy.closeClientConnections(true, true);
    System.out.println("Client Socket Closed by proxy");

    PingMessage ping = (PingMessage) clientMsgCh.createMessage(TCMessageType.PING_MESSAGE);
    ping.initialize(sq);
    ping.send();
    System.out.println("PNG sent to Client");

    System.out.println("Sleeping for " + getMinSleepTimeToConirmDeath(hcConfig));
    ThreadUtil.reallySleep(getMinSleepTimeToConirmDeath(hcConfig));
  }

  public void testL2CloseL1Reconnect() throws Exception {
    HealthCheckerConfig hcConfig = new HealthCheckerConfigImpl(10000, 4000, 2, "ServerCommsHC-Test12", false);
    this.setUp(hcConfig, null);
    ClientMessageChannel clientMsgCh = createClientMsgCh();
    clientMsgCh.open();

    // Verifications
    ConnectionHealthCheckerImpl connHC = (ConnectionHealthCheckerImpl) ((CommunicationsManagerImpl) serverComms)
        .getConnHealthChecker();
    assertNotNull(connHC);

    while (!connHC.isRunning() && (connHC.getTotalConnsUnderMonitor() != 1)) {
      System.out.println("Yet to start the connection health cheker thread...");
      ThreadUtil.reallySleep(1000);
    }

    SequenceGenerator sq = new SequenceGenerator();
    for (int i = 1; i <= 5; i++) {
      PingMessage ping = (PingMessage) clientMsgCh.createMessage(TCMessageType.PING_MESSAGE);
      ping.initialize(sq);
      ping.send();
    }

    System.out.println("Sleeping for " + getMinSleepTimeToSendFirstProbe(hcConfig));
    ThreadUtil.reallySleep(getMinSleepTimeToSendFirstProbe(hcConfig));
    System.out.println("Successfully sent " + connHC.getTotalProbesSentOnAllConns() + " Probes");
    assertTrue(connHC.getTotalProbesSentOnAllConns() > 0);

    proxy.stop();
    System.out.println("Proxy STOPPED");

    System.out.println("Sleeping for " + getMinSleepTimeToSendFirstProbe(hcConfig));
    ThreadUtil.reallySleep(getMinSleepTimeToSendFirstProbe(hcConfig));

    proxy.start();
    System.out.println("Proxy STARTED");

    System.out.println("Sleeping for " + getMinSleepTimeToSendFirstProbe(hcConfig));
    ThreadUtil.reallySleep(getMinSleepTimeToSendFirstProbe(hcConfig));

    /* By Now, the client would have reconnected */
    assertEquals(1, connHC.getTotalConnsUnderMonitor());
  }

  protected void closeCommsMgr() throws Exception {
    if (serverLsnr != null) serverLsnr.stop(1000);
    if (serverComms != null) {
      serverComms.shutdown();
    }
    if (clientComms != null) {
      clientComms.shutdown();
    }
    closeClientMessageChannels();
  }

  private void closeClientMessageChannels() {
    Iterator<ClientMessageChannel> i = channelQueue.iterator();
    while (i.hasNext()) {
      i.next().close();
    }
    channelQueue.clear();
  }

  @Override
  public void tearDown() throws Exception {
    super.tearDown();
    logger.setLevel(LogLevelImpl.INFO);
    closeCommsMgr();
  }
}
