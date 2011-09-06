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
import com.tc.net.protocol.tcm.ClientMessageChannel;
import com.tc.net.protocol.tcm.CommunicationsManager;
import com.tc.net.protocol.tcm.CommunicationsManagerImpl;
import com.tc.net.protocol.tcm.NetworkListener;
import com.tc.net.protocol.tcm.NullMessageMonitor;
import com.tc.net.protocol.tcm.TCMessage;
import com.tc.net.protocol.tcm.TCMessageRouterImpl;
import com.tc.net.protocol.tcm.TCMessageSink;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.net.protocol.tcm.UnsupportedMessageTypeException;
import com.tc.net.protocol.tcm.msgs.PingMessage;
import com.tc.object.session.NullSessionManager;
import com.tc.test.TCTestCase;
import com.tc.util.SequenceGenerator;
import com.tc.util.concurrent.ThreadUtil;

import java.util.Collections;
import java.util.HashSet;

public class ConnectionHealthCheckerTest extends TCTestCase {

  CommunicationsManager serverComms;
  CommunicationsManager clientComms;

  TCMessageRouterImpl   serverMessageRouter;
  TCMessageRouterImpl   clientMessageRouter;

  NetworkListener       serverLsnr;
  TCLogger              logger = TCLogging.getLogger(ConnectionHealthCheckerImpl.class);

  protected void setUp(HealthCheckerConfig serverHCConf, HealthCheckerConfig clientHCConf) throws Exception {
    super.setUp();

    NetworkStackHarnessFactory networkStackHarnessFactory = new PlainNetworkStackHarnessFactory();

    logger.setLevel(LogLevelImpl.DEBUG);

    serverMessageRouter = new TCMessageRouterImpl();
    clientMessageRouter = new TCMessageRouterImpl();

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
  }

  ClientMessageChannel createClientMsgCh() {
    return createClientMsgCh(null);
  }

  ClientMessageChannel createClientMsgCh(CommunicationsManager clientCommsMgr) {

    CommunicationsManager commsMgr = (clientCommsMgr == null ? clientComms : clientCommsMgr);
    commsMgr.addClassMapping(TCMessageType.PING_MESSAGE, PingMessage.class);
    ((CommunicationsManagerImpl) commsMgr).getMessageRouter().routeMessageType(TCMessageType.PING_MESSAGE,
                                                                               new TCMessageSink() {

                                                                                 public void putMessage(TCMessage message)
                                                                                     throws UnsupportedMessageTypeException {
                                                                                   PingMessage pingMsg = (PingMessage) message;
                                                                                   try {
                                                                                     pingMsg.hydrate();
                                                                                     System.out
                                                                                         .println(" Client RECEIVE - PING seq no "
                                                                                                  + pingMsg
                                                                                                      .getSequence());
                                                                                   } catch (Exception e) {
                                                                                     System.out
                                                                                         .println("Client Exception during PingMessage hydrate:");
                                                                                     e.printStackTrace();
                                                                                   }
                                                                                 }
                                                                               });

    ClientMessageChannel clientMsgCh = commsMgr
        .createClientChannel(new NullSessionManager(),
                             0,
                             TCSocketAddress.LOOPBACK_IP,
                             serverLsnr.getBindPort(),
                             1000,
                             new ConnectionAddressProvider(
                                                           new ConnectionInfo[] { new ConnectionInfo("localhost",
                                                                                                     serverLsnr
                                                                                                         .getBindPort()) }));

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

  public void testL1ProbingL2() throws Exception {
    HealthCheckerConfig hcConfig = new HealthCheckerConfigImpl(4000, 2000, 1, "ClientCommsHC-Test01", false);
    this.setUp(null, hcConfig);
    ClientMessageChannel clientMsgCh = createClientMsgCh();
    clientMsgCh.open();

    // Verifications
    ConnectionHealthCheckerImpl connHC = (ConnectionHealthCheckerImpl) ((CommunicationsManagerImpl) clientComms)
        .getConnHealthChecker();
    assertNotNull(connHC);

    while (!connHC.isRunning() || (connHC.getTotalConnsUnderMonitor() <= 0)) {
      System.out.println("Yet to start the connection health cheker thread...");
      ThreadUtil.reallySleep(1000);
    }

    SequenceGenerator sq = new SequenceGenerator();
    for (int i = 1; i <= 5; i++) {
      PingMessage ping = (PingMessage) clientMsgCh.createMessage(TCMessageType.PING_MESSAGE);
      ping.initialize(sq);
      ping.send();
    }

    ThreadUtil.reallySleep(getMinSleepTimeToSendFirstProbe(hcConfig));
    System.out.println("Successfully sent " + connHC.getTotalProbesSentOnAllConns() + " Probes");
    assertTrue(connHC.getTotalProbesSentOnAllConns() > 0);

    clientMsgCh.close();
  }

  public void testL2ProbingL1AndClientClose() throws Exception {
    HealthCheckerConfig hcConfig = new HealthCheckerConfigImpl("ServerCommsHC-Test02");
    this.setUp(hcConfig, new DisabledHealthCheckerConfigImpl());
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

    ThreadUtil.reallySleep(getMinSleepTimeToSendFirstProbe(hcConfig));
    System.out.println("Successfully sent " + connHC.getTotalProbesSentOnAllConns() + " Probes");
    assertTrue(connHC.getTotalProbesSentOnAllConns() > 0);

    clientMsgCh.close();
    System.out.println("ClientMessasgeChannel Closed");

    System.out.println("Sleeping for " + getMinSleepTimeToConirmDeath(hcConfig));
    ThreadUtil.reallySleep(getMinSleepTimeToConirmDeath(hcConfig));

    while (connHC.getTotalConnsUnderMonitor() != 0) {
      ThreadUtil.reallySleep(1000);
      System.out.println("Waiting for Client removal from Health Checker");
    }

    System.out.println("Success");
  }

  public void testL1L2ProbingBothsideAndClientClose() throws Exception {
    HealthCheckerConfig hcConfig = new HealthCheckerConfigImpl(4000, 2000, 5, "ServerCommsHC-Test03", false);
    HealthCheckerConfig hcConfig2 = new HealthCheckerConfigImpl(10000, 4000, 3, "ClientCommsHC-Test03", false);
    this.setUp(hcConfig, new DisabledHealthCheckerConfigImpl());

    CommunicationsManager clientComms1 = new CommunicationsManagerImpl("TestCommsMgr-Client1",
                                                                       new NullMessageMonitor(),
                                                                       new PlainNetworkStackHarnessFactory(true),
                                                                       new NullConnectionPolicy(), hcConfig2);
    CommunicationsManager clientComms2 = new CommunicationsManagerImpl("TestCommsMgr-Client2",
                                                                       new NullMessageMonitor(),
                                                                       new PlainNetworkStackHarnessFactory(true),
                                                                       new NullConnectionPolicy(), hcConfig2);
    ClientMessageChannel clientMsgCh1 = createClientMsgCh(clientComms1);
    clientMsgCh1.open();

    ClientMessageChannel clientMsgCh2 = createClientMsgCh(clientComms2);
    clientMsgCh2.open();

    // Verifications
    ConnectionHealthCheckerImpl connHC = (ConnectionHealthCheckerImpl) ((CommunicationsManagerImpl) serverComms)
        .getConnHealthChecker();

    assertNotNull(connHC);
    while (!connHC.isRunning() && (connHC.getTotalConnsUnderMonitor() != 2)) {
      System.out.println("Yet to start the connection health cheker thread...");
      ThreadUtil.reallySleep(1000);
    }

    SequenceGenerator sq = new SequenceGenerator();
    for (int i = 1; i <= 5; i++) {
      PingMessage ping = (PingMessage) clientMsgCh1.createMessage(TCMessageType.PING_MESSAGE);
      ping.initialize(sq);
      ping.send();
      ping = (PingMessage) clientMsgCh2.createMessage(TCMessageType.PING_MESSAGE);
      ping.initialize(sq);
      ping.send();
    }

    ThreadUtil.reallySleep(getMinSleepTimeToSendFirstProbe(hcConfig));
    System.out.println("Successfully sent " + connHC.getTotalProbesSentOnAllConns() + " Probes");
    assertTrue(connHC.getTotalProbesSentOnAllConns() > 0);

    clientMsgCh1.close();
    System.out.println("ClientMessasgeChannel 1 Closed");

    ThreadUtil.reallySleep(getMinSleepTimeToSendFirstProbe(hcConfig));
    assertEquals(1, connHC.getTotalConnsUnderMonitor());

  }

  public void testL2ProbingL1AndClientUnResponsive() throws Exception {
    HealthCheckerConfig hcConfig = new HealthCheckerConfigImpl(5000, 2000, 2, "ServerCommsHC-Test04", false);
    this.setUp(hcConfig, null);
    ((CommunicationsManagerImpl) clientComms).setConnHealthChecker(new ConnectionHealthCheckerDummyImpl());
    ClientMessageChannel clientMsgCh = createClientMsgCh();
    clientMsgCh.open();

    // Verifications
    ConnectionHealthCheckerImpl connHC = (ConnectionHealthCheckerImpl) ((CommunicationsManagerImpl) serverComms)
        .getConnHealthChecker();
    assertNotNull(connHC);

    while (!connHC.isRunning() && (connHC.getTotalConnsUnderMonitor() <= 0)) {
      System.out.println("Yet to start the connection health cheker thread...");
      ThreadUtil.reallySleep(1000);
    }

    SequenceGenerator sq = new SequenceGenerator();
    for (int i = 1; i <= 5; i++) {
      PingMessage ping = (PingMessage) clientMsgCh.createMessage(TCMessageType.PING_MESSAGE);
      ping.initialize(sq);
      ping.send();
    }

    System.out.println("Sleeping for " + getMinSleepTimeToConirmDeath(hcConfig));
    ThreadUtil.reallySleep(getMinSleepTimeToConirmDeath(hcConfig));

    assertEquals(0, connHC.getTotalConnsUnderMonitor());

  }

  public void testL1ProbingL2AndServerUnResponsive() throws Exception {
    HealthCheckerConfig hcConfig = new HealthCheckerConfigImpl(5000, 2000, 2, "ClientCommsHC-Test05", false);
    this.setUp(null, hcConfig);
    ((CommunicationsManagerImpl) serverComms).setConnHealthChecker(new ConnectionHealthCheckerDummyImpl());
    ClientMessageChannel clientMsgCh = createClientMsgCh();
    clientMsgCh.open();

    // Verifications
    ConnectionHealthCheckerImpl connHC = (ConnectionHealthCheckerImpl) ((CommunicationsManagerImpl) clientComms)
        .getConnHealthChecker();
    assertNotNull(connHC);

    while (!connHC.isRunning() && (connHC.getTotalConnsUnderMonitor() <= 0)) {
      System.out.println("Yet to start the connection health cheker thread...");
      ThreadUtil.reallySleep(1000);
    }

    SequenceGenerator sq = new SequenceGenerator();
    for (int i = 1; i <= 5; i++) {
      PingMessage ping = (PingMessage) clientMsgCh.createMessage(TCMessageType.PING_MESSAGE);
      ping.initialize(sq);
      ping.send();
    }

    System.out.println("Sleeping for " + getMinSleepTimeToConirmDeath(hcConfig));
    ThreadUtil.reallySleep(getMinSleepTimeToConirmDeath(hcConfig));

    assertEquals(0, connHC.getTotalConnsUnderMonitor());

  }

  public void testL2L1WrongConfig() throws Exception {
    try {
      HealthCheckerConfig hcConfig = new HealthCheckerConfigImpl(30000, 40000, 3, "ServerCommsHC-Test06", false);
      this.setUp(hcConfig, null);
    } catch (AssertionError e) {
      // Expected.
      System.out.println("Got the Expected error");
    }

    closeCommsMgr();

    try {
      HealthCheckerConfig hcConfig = new HealthCheckerConfigImpl(30000, 0, 3, "ClientCommsHC-Test06", false);
      this.setUp(null, hcConfig);
    } catch (AssertionError e) {
      // Expected.
      System.out.println("Got the Expected error");
    }

  }

  protected void closeCommsMgr() throws Exception {
    if (serverLsnr != null) serverLsnr.stop(1000);
    if (serverComms != null) serverComms.shutdown();
    if (clientComms != null) clientComms.shutdown();
  }

  @Override
  public void tearDown() throws Exception {
    super.tearDown();
    logger.setLevel(LogLevelImpl.INFO);
    closeCommsMgr();
  }

}
