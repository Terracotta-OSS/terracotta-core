/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

import org.hyperic.sigar.NetConnection;
import org.hyperic.sigar.NetFlags;
import org.hyperic.sigar.NetInfo;
import org.hyperic.sigar.NetInterfaceConfig;
import org.hyperic.sigar.NetStat;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;

import EDU.oswego.cs.dl.util.concurrent.BoundedLinkedQueue;

import com.tc.async.api.Stage;
import com.tc.async.impl.StageManagerImpl;
import com.tc.lang.TCThreadGroup;
import com.tc.lang.ThrowableHandler;
import com.tc.logging.LogLevelImpl;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.TCSocketAddress;
import com.tc.net.core.ConnectionAddressProvider;
import com.tc.net.core.ConnectionInfo;
import com.tc.net.protocol.NetworkStackHarnessFactory;
import com.tc.net.protocol.PlainNetworkStackHarnessFactory;
import com.tc.net.protocol.delivery.OOOEventHandler;
import com.tc.net.protocol.delivery.OOONetworkStackHarnessFactory;
import com.tc.net.protocol.delivery.OnceAndOnlyOnceProtocolNetworkLayerFactoryImpl;
import com.tc.net.protocol.tcm.ClientMessageChannel;
import com.tc.net.protocol.tcm.CommunicationsManager;
import com.tc.net.protocol.tcm.CommunicationsManagerImpl;
import com.tc.net.protocol.tcm.NetworkListener;
import com.tc.net.protocol.tcm.NullMessageMonitor;
import com.tc.net.protocol.tcm.TCMessage;
import com.tc.net.protocol.tcm.TCMessageSink;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.net.protocol.tcm.UnsupportedMessageTypeException;
import com.tc.net.protocol.tcm.msgs.PingMessage;
import com.tc.net.proxy.TCPProxy;
import com.tc.object.session.NullSessionManager;
import com.tc.properties.L1ReconnectConfigImpl;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;
import com.tc.util.PortChooser;
import com.tc.util.SequenceGenerator;
import com.tc.util.concurrent.QueueFactory;
import com.tc.util.concurrent.ThreadUtil;

import java.util.HashSet;

public class ConnectionHealthCheckerLongGCTest extends TCTestCase {

  CommunicationsManager serverComms;
  CommunicationsManager clientComms;
  NetworkListener       serverLsnr;
  TCLogger              logger    = TCLogging.getLogger(ConnectionHealthCheckerImpl.class);
  TCPProxy              proxy     = null;
  int                   proxyPort = 0;

  protected void setUp(HealthCheckerConfig serverHCConf, HealthCheckerConfig clientHCConf) throws Exception {
    setUp(serverHCConf, clientHCConf, false);
  }

  protected void setUp(HealthCheckerConfig serverHCConf, HealthCheckerConfig clientHCConf, boolean EnableReconnect)
      throws Exception {
    super.setUp();

    NetworkStackHarnessFactory networkStackHarnessFactory;

    logger.setLevel(LogLevelImpl.DEBUG);

    if (EnableReconnect) {
      StageManagerImpl stageManager = new StageManagerImpl(new TCThreadGroup(new ThrowableHandler(TCLogging
          .getLogger(StageManagerImpl.class))), new QueueFactory(BoundedLinkedQueue.class.getName()));
      final Stage oooSendStage = stageManager.createStage("OOONetSendStage", new OOOEventHandler(), 1, 5000);
      final Stage oooReceiveStage = stageManager.createStage("OOONetReceiveStage", new OOOEventHandler(), 1, 5000);
      networkStackHarnessFactory = new OOONetworkStackHarnessFactory(
                                                                     new OnceAndOnlyOnceProtocolNetworkLayerFactoryImpl(),
                                                                     oooSendStage.getSink(), oooReceiveStage.getSink(),
                                                                     new L1ReconnectConfigImpl());
    } else {
      networkStackHarnessFactory = new PlainNetworkStackHarnessFactory();
    }

    if (serverHCConf != null) {
      serverComms = new CommunicationsManagerImpl(new NullMessageMonitor(), networkStackHarnessFactory,
                                                  new NullConnectionPolicy(), serverHCConf);
    } else {
      serverComms = new CommunicationsManagerImpl(new NullMessageMonitor(), networkStackHarnessFactory,
                                                  new NullConnectionPolicy());
    }

    if (clientHCConf != null) {
      clientComms = new CommunicationsManagerImpl(new NullMessageMonitor(), networkStackHarnessFactory,
                                                  new NullConnectionPolicy(), clientHCConf);
    } else {
      clientComms = new CommunicationsManagerImpl(new NullMessageMonitor(), networkStackHarnessFactory,
                                                  new NullConnectionPolicy());

    }

    serverLsnr = serverComms.createListener(new NullSessionManager(), new TCSocketAddress(0), false,
                                            new DefaultConnectionIdFactory());

    serverLsnr.addClassMapping(TCMessageType.PING_MESSAGE, PingMessage.class);
    serverLsnr.routeMessageType(TCMessageType.PING_MESSAGE, new TCMessageSink() {

      public void putMessage(TCMessage message) throws UnsupportedMessageTypeException {

        PingMessage pingMsg = (PingMessage) message;
        try {
          pingMsg.hydrate();
          System.out.println("Server RECEIVE - PING seq no " + pingMsg.getSequence());
        } catch (Exception e) {
          System.out.println("Server Exception during PingMessage hydrate:");
          e.printStackTrace();
        }

        PingMessage pingRplyMsg = pingMsg.createResponse();
        pingRplyMsg.send();
      }
    });

    serverLsnr.start(new HashSet());

    int serverPort = serverLsnr.getBindPort();
    proxyPort = new PortChooser().chooseRandomPort();
    proxy = new TCPProxy(proxyPort, serverLsnr.getBindAddress(), serverPort, 0, false, null);
    proxy.start();

  }

  ClientMessageChannel createClientMsgCh() {
    return createClientMsgChProxied(clientComms);
  }

  ClientMessageChannel createClientMsgChProxied(CommunicationsManager clientCommsMgr) {

    ClientMessageChannel clientMsgCh = clientCommsMgr
        .createClientChannel(new NullSessionManager(), 0, serverLsnr.getBindAddress().getHostAddress(), proxyPort,
                             1000, new ConnectionAddressProvider(new ConnectionInfo[] { new ConnectionInfo(serverLsnr
                                 .getBindAddress().getHostAddress(), proxyPort) }),
                             TransportHandshakeMessage.NO_CALLBACK_PORT);

    clientMsgCh.addClassMapping(TCMessageType.PING_MESSAGE, PingMessage.class);
    clientMsgCh.routeMessageType(TCMessageType.PING_MESSAGE, new TCMessageSink() {

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
    return clientMsgCh;
  }

  public long getMinSleepTimeToSendFirstProbe(HealthCheckerConfig config) {
    assertNotNull(config);
    /* Interval time is doubled to give grace period */
    return (config.getPingIdleTimeMillis() + (2 * config.getPingIntervalMillis()));
  }

  public long getMinSleepTimeToStartLongGCTest(HealthCheckerConfig config) {
    assertNotNull(config);
    /* Interval time is doubled to give grace period */
    long exact_time = config.getPingIdleTimeMillis() + (config.getPingIntervalMillis() * config.getPingProbes());
    long grace_time = config.getPingIntervalMillis();
    return (exact_time + grace_time);
  }

  public long getMinScoketConnectResultTime(HealthCheckerConfig config) {
    assertNotNull(config);
    long extraTime = (config.getPingIntervalMillis() * config.getPingProbes())
                     + (config.getSocketConnectTimeout() * config.getPingIntervalMillis());
    long grace_time = config.getPingIntervalMillis();
    return extraTime + grace_time;
  }

  public long getMinScoketConnectResultTimeAfterSocketConnectStart(HealthCheckerConfig config) {
    assertNotNull(config);
    long extraTime = (config.getSocketConnectTimeout() * config.getPingIntervalMillis());
    long grace_time = config.getPingIntervalMillis();
    return extraTime + grace_time;
  }

  public long getFullExtraCheckTime(HealthCheckerConfig config) {
    assertNotNull(config);
    long extraTime = config.getPingIntervalMillis()
                     * ((config.getSocketConnectTimeout() * config.getPingProbes()) * config.getSocketConnectMaxCount());
    return extraTime;
  }

  public void testL2SocketConnectL1() throws Exception {
    HealthCheckerConfig hcConfig = new HealthCheckerConfigImpl(5000, 2000, 1, "ServerCommsHC-Test31", true /*
                                                                                                             * EXTRA
                                                                                                             * CHECK ON
                                                                                                             */);
    this.setUp(hcConfig, null);
    ((CommunicationsManagerImpl) clientComms).setConnHealthChecker(new ConnectionHealthCheckerDummyImpl());
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

    System.out.println("Sleeping for " + getMinSleepTimeToStartLongGCTest(hcConfig));
    ThreadUtil.reallySleep(getMinSleepTimeToStartLongGCTest(hcConfig));

    /*
     * L2 should have started the Extra Check by now; But L2 will not be able to do socket connect to L1.
     */
    ThreadUtil.reallySleep(getMinScoketConnectResultTime(hcConfig));

    /* By Now, the client should have been chucked out */
    assertEquals(0, connHC.getTotalConnsUnderMonitor());

  }

  public void testL1SocketConnectL2() throws Exception {
    HealthCheckerConfig hcConfig = new HealthCheckerConfigImpl(5000, 2000, 2, "ClientCommsHC-Test32", true /*
                                                                                                             * EXTRA
                                                                                                             * CHECK ON
                                                                                                             */);
    this.setUp(null, hcConfig);
    ((CommunicationsManagerImpl) serverComms).setConnHealthChecker(new ConnectionHealthCheckerDummyImpl());
    ClientMessageChannel clientMsgCh = createClientMsgCh();
    clientMsgCh.open();

    // Verifications
    ConnectionHealthCheckerImpl connHC = (ConnectionHealthCheckerImpl) ((CommunicationsManagerImpl) clientComms)
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

    System.out.println("Sleeping for " + getMinSleepTimeToStartLongGCTest(hcConfig));
    ThreadUtil.reallySleep(getMinSleepTimeToStartLongGCTest(hcConfig));

    /*
     * L1 should have started the Extra Check by now; L1 should be able to do socket connect to L2.
     */
    ThreadUtil.reallySleep(getMinScoketConnectResultTime(hcConfig));
    assertEquals(1, connHC.getTotalConnsUnderMonitor());

    /*
     * Though L1 is able to connect, we can't trust the client for too long
     */
    ThreadUtil.reallySleep(getFullExtraCheckTime(hcConfig));
    assertEquals(0, connHC.getTotalConnsUnderMonitor());

  }

  private void getNetInfo(int bindPort) {
    try {
      Sigar s = new Sigar();
      NetInfo info = s.getNetInfo();
      NetInterfaceConfig config = s.getNetInterfaceConfig(null);
      System.out.println(info.toString());
      System.out.println(config.toString());

      int flags = NetFlags.CONN_TCP | NetFlags.TCP_ESTABLISHED;

      NetConnection[] connections = s.getNetConnectionList(flags);

      System.out.println("XXX Established connections if any");
      for (int i = 0; i < connections.length; i++) {
        long port = connections[i].getLocalPort();
        long remotePort = connections[i].getRemotePort();
        if (bindPort == port || bindPort == remotePort) {
          System.out.println("XXX " + connections[i]);
        }
      }
    } catch (SigarException se) {
      System.out.println("Unable to get Sigar NetInfo: " + se);
    }
  }

  public void testL1SocketConnectTimeoutL2() throws Exception {
    HealthCheckerConfig hcConfig = new HealthCheckerConfigImpl(4000, 2000, 2, "ClientCommsHC-Test32", true /*
                                                                                                             * EXTRA
                                                                                                             * CHECK ON
                                                                                                             */);
    Sigar sigar = new Sigar();
    NetStat netstat = null;

    this.setUp(null, null);

    // set up custom HealthCheckers
    ((CommunicationsManagerImpl) serverComms).setConnHealthChecker(new ConnectionHealthCheckerDummyImpl());
    ((CommunicationsManagerImpl) clientComms)
        .setConnHealthChecker(new TestConnectionHealthCheckerImpl(hcConfig, clientComms.getConnectionManager()));

    ClientMessageChannel clientMsgCh = createClientMsgCh();
    clientMsgCh.open();

    // Verifications
    ConnectionHealthCheckerImpl connHC = (ConnectionHealthCheckerImpl) ((CommunicationsManagerImpl) clientComms)
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

    netstat = sigar.getNetStat(serverLsnr.getBindAddress().getAddress(), serverLsnr.getBindPort());
    getNetInfo(serverLsnr.getBindPort());
    Assert.assertEquals(2, netstat.getTcpEstablished());

    System.out.println("Sleeping for " + getMinSleepTimeToStartLongGCTest(hcConfig));
    ThreadUtil.reallySleep(getMinSleepTimeToStartLongGCTest(hcConfig));

    /*
     * L1 should have started the Extra Check by now; since we are using Dumb HC socket connector, socket connect should
     * time out.
     */
    ThreadUtil.reallySleep(getMinScoketConnectResultTime(hcConfig));
    assertEquals(0, connHC.getTotalConnsUnderMonitor());

    ThreadUtil.reallySleep(5000);
    netstat = sigar.getNetStat(serverLsnr.getBindAddress().getAddress(), serverLsnr.getBindPort());
    getNetInfo(serverLsnr.getBindPort());
    Assert.assertEquals(0, netstat.getTcpEstablished());
  }

  public void testL1SocketConnectTimeoutL2AndL1Reconnect() throws Exception {
    HealthCheckerConfig hcConfig = new HealthCheckerConfigImpl(4000, 1000, 2, "ClientCommsHC-Test32", true /*
                                                                                                             * EXTRA
                                                                                                             * CHECK ON
                                                                                                             */);
    Sigar sigar = new Sigar();
    NetStat netstat = null;

    this.setUp(null, null, true);

    // set up custom HealthCheckers
    ((CommunicationsManagerImpl) serverComms).setConnHealthChecker(new ConnectionHealthCheckerDummyImpl());
    ((CommunicationsManagerImpl) clientComms)
        .setConnHealthChecker(new TestConnectionHealthCheckerImpl(hcConfig, clientComms.getConnectionManager()));

    ClientMessageChannel clientMsgCh = createClientMsgCh();
    clientMsgCh.open();

    // Verifications
    ConnectionHealthCheckerImpl connHC = (ConnectionHealthCheckerImpl) ((CommunicationsManagerImpl) clientComms)
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

    netstat = sigar.getNetStat(serverLsnr.getBindAddress().getAddress(), serverLsnr.getBindPort());
    getNetInfo(serverLsnr.getBindPort());
    Assert.assertEquals(2, netstat.getTcpEstablished());

    System.out.println("Sleeping for " + getMinSleepTimeToStartLongGCTest(hcConfig));
    ThreadUtil.reallySleep(getMinSleepTimeToStartLongGCTest(hcConfig));

    // set some delay in proxy so that even if reconnect started, we will have sufficient time to check established
    // connections
    proxy.setDelay(2000);
    System.out.println("woke up");

    /*
     * L1 should have started the Extra Check by now; since we are using Dumb HC socket connector, socket connect should
     * time out.
     */
    ThreadUtil.reallySleep(getMinScoketConnectResultTimeAfterSocketConnectStart(hcConfig));
    while (!connHC.isRunning() && (connHC.getTotalConnsUnderMonitor() != 0)) {
      System.out.println("waiting for client to disconnect");
      ThreadUtil.reallySleep(1000);
    }

    /*
     * Client is suppose to start the reconnect immediately after its disconnect. Lets remove the obstacle from the road
     */
    proxy.setDelay(0);
    while (!connHC.isRunning() && (connHC.getTotalConnsUnderMonitor() != 1)) {
      System.out.println("waiting for client to rejoin");
      ThreadUtil.reallySleep(1000);
    }

    ThreadUtil.reallySleep(5000);
    /*
     * Client disconnected after it found socket connect timeout. After the successful reconnect there should be no
     * connection leak. DEV-1963
     */
    netstat = sigar.getNetStat(serverLsnr.getBindAddress().getAddress(), serverLsnr.getBindPort());
    getNetInfo(serverLsnr.getBindPort());
    System.out.println("XXX reconnect- tcp estd : " + netstat.getTcpEstablished());
    Assert.assertEquals(2, netstat.getTcpEstablished());
  }

  public void testL2SocketConnectL1WithProxyDelay() throws Exception {
    HealthCheckerConfig hcConfig = new HealthCheckerConfigImpl(5000, 2000, 2, "ServerCommsHC-Test33", true /*
                                                                                                             * EXTRA
                                                                                                             * CHECK ON
                                                                                                             */);
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

    /* Setting a LONG delay on the ROAD :D */
    proxy.setDelay(100 * 1000);

    System.out.println("Sleeping for " + getMinSleepTimeToStartLongGCTest(hcConfig));
    ThreadUtil.reallySleep(getMinSleepTimeToStartLongGCTest(hcConfig));

    /*
     * L2 should have started the Extra Check by now; But L2 will not be able to do socket connect to L1.
     */
    ThreadUtil.reallySleep(getMinScoketConnectResultTime(hcConfig));

    /* By Now, the client should be chuked out */
    assertEquals(0, connHC.getTotalConnsUnderMonitor());

  }

  protected void closeCommsMgr() throws Exception {
    if (serverLsnr != null) serverLsnr.stop(1000);
    if (serverComms != null) serverComms.shutdown();
    if (clientComms != null) clientComms.shutdown();
  }

  public void tearDown() throws Exception {
    super.tearDown();
    logger.setLevel(LogLevelImpl.INFO);
    closeCommsMgr();
  }

}
