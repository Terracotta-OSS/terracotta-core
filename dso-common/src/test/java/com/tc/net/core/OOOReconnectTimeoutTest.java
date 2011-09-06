/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.core;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.ServerID;
import com.tc.net.TCSocketAddress;
import com.tc.net.protocol.NetworkStackHarnessFactory;
import com.tc.net.protocol.PlainNetworkStackHarnessFactory;
import com.tc.net.protocol.delivery.OOONetworkStackHarnessFactory;
import com.tc.net.protocol.delivery.OnceAndOnlyOnceProtocolNetworkLayerFactoryImpl;
import com.tc.net.protocol.tcm.ClientMessageChannel;
import com.tc.net.protocol.tcm.CommunicationsManager;
import com.tc.net.protocol.tcm.CommunicationsManagerImpl;
import com.tc.net.protocol.tcm.NetworkListener;
import com.tc.net.protocol.tcm.NullMessageMonitor;
import com.tc.net.protocol.tcm.TCMessageRouterImpl;
import com.tc.net.protocol.transport.DefaultConnectionIdFactory;
import com.tc.net.protocol.transport.HealthCheckerConfigImpl;
import com.tc.net.protocol.transport.NullConnectionPolicy;
import com.tc.net.protocol.transport.TransportHandshakeErrorNullHandler;
import com.tc.net.proxy.TCPProxy;
import com.tc.object.session.NullSessionManager;
import com.tc.properties.L1ReconnectConfigImpl;
import com.tc.properties.TCPropertiesImpl;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;
import com.tc.util.PortChooser;
import com.tc.util.concurrent.ThreadUtil;
import com.tc.util.runtime.ThreadDumpUtil;

import java.net.InetAddress;
import java.util.Collections;

public class OOOReconnectTimeoutTest extends TCTestCase {
  //

  TCLogger          logger               = TCLogging.getLogger(TCWorkerCommManager.class);
  private final int L1_RECONNECT_TIMEOUT = 15000;

  private ClientMessageChannel createClientMsgCh(int port) {
    return createClientMsgCh(port, true);
  }

  private ClientMessageChannel createClientMsgCh(int port, boolean ooo) {

    CommunicationsManager clientComms = new CommunicationsManagerImpl("Client-TestCommsMgr", new NullMessageMonitor(),
                                                                      getNetworkStackHarnessFactory(ooo),
                                                                      new NullConnectionPolicy());

    ClientMessageChannel clientMsgCh = clientComms
        .createClientChannel(new NullSessionManager(),
                             -1,
                             "localhost",
                             port,
                             1000,
                             new ConnectionAddressProvider(
                                                           new ConnectionInfo[] { new ConnectionInfo("localhost", port) }));
    return clientMsgCh;
  }

  private NetworkStackHarnessFactory getNetworkStackHarnessFactory(boolean enableReconnect) {
    NetworkStackHarnessFactory networkStackHarnessFactory;
    if (enableReconnect) {
      networkStackHarnessFactory = new OOONetworkStackHarnessFactory(
                                                                     new OnceAndOnlyOnceProtocolNetworkLayerFactoryImpl(),
                                                                     new L1ReconnectConfigImpl(true,
                                                                                               L1_RECONNECT_TIMEOUT,
                                                                                               5000, 16, 32));
    } else {
      networkStackHarnessFactory = new PlainNetworkStackHarnessFactory();
    }
    return networkStackHarnessFactory;

  }

  public void testWorkerCommDistributionAfterReconnect() throws Exception {
    // comms manager with 3 worker comms
    CommunicationsManager commsMgr = new CommunicationsManagerImpl("TestCommsMgr", new NullMessageMonitor(),
                                                                   new TCMessageRouterImpl(),
                                                                   getNetworkStackHarnessFactory(true),
                                                                   new NullConnectionPolicy(), 3,
                                                                   new HealthCheckerConfigImpl(TCPropertiesImpl
                                                                       .getProperties()
                                                                       .getPropertiesFor("l2.healthcheck.l1"),
                                                                                               "Test Server"),
                                                                   new ServerID(),
                                                                   new TransportHandshakeErrorNullHandler(),
                                                                   Collections.EMPTY_MAP, Collections.EMPTY_MAP);
    NetworkListener listener = commsMgr.createListener(new NullSessionManager(), new TCSocketAddress(0), true,
                                                       new DefaultConnectionIdFactory());
    listener.start(Collections.EMPTY_SET);
    int serverPort = listener.getBindPort();

    int proxyPort = new PortChooser().chooseRandomPort();
    TCPProxy proxy = new TCPProxy(proxyPort, InetAddress.getByName("localhost"), serverPort, 0, false, null);
    proxy.start();

    ClientMessageChannel client1 = createClientMsgCh(proxyPort);
    ClientMessageChannel client2 = createClientMsgCh(proxyPort);
    ClientMessageChannel client3 = createClientMsgCh(proxyPort);

    client1.open();
    client2.open();
    client3.open();

    ThreadUtil.reallySleep(2000);
    assertTrue(client1.isConnected());
    assertTrue(client2.isConnected());
    assertTrue(client3.isConnected());

    Assert.assertEquals(1, ((TCCommImpl) commsMgr.getConnectionManager().getTcComm()).getWeightForWorkerComm(0));
    Assert.assertEquals(1, ((TCCommImpl) commsMgr.getConnectionManager().getTcComm()).getWeightForWorkerComm(1));
    Assert.assertEquals(1, ((TCCommImpl) commsMgr.getConnectionManager().getTcComm()).getWeightForWorkerComm(2));

    // case 1 : network problems .. both ends getting events. Server opens up reconnect window
    proxy.stop();

    ThreadUtil.reallySleep(5000);
    Assert.assertEquals(0, ((TCCommImpl) commsMgr.getConnectionManager().getTcComm()).getWeightForWorkerComm(0));
    Assert.assertEquals(0, ((TCCommImpl) commsMgr.getConnectionManager().getTcComm()).getWeightForWorkerComm(1));
    Assert.assertEquals(0, ((TCCommImpl) commsMgr.getConnectionManager().getTcComm()).getWeightForWorkerComm(2));

    proxy.start();

    ThreadUtil.reallySleep(5000);
    assertTrue(client1.isConnected());
    assertTrue(client2.isConnected());
    Assert.assertEquals(1, ((TCCommImpl) commsMgr.getConnectionManager().getTcComm()).getWeightForWorkerComm(0));
    Assert.assertEquals(1, ((TCCommImpl) commsMgr.getConnectionManager().getTcComm()).getWeightForWorkerComm(1));
    Assert.assertEquals(1, ((TCCommImpl) commsMgr.getConnectionManager().getTcComm()).getWeightForWorkerComm(2));

    // case 2: problem with the client side connections .. but server still thinks clients are connected
    proxy.closeClientConnections(true, false);

    ThreadUtil.reallySleep(5000);

    System.out.println("XXX waiting for clients to reconnect");
    while ((((TCCommImpl) commsMgr.getConnectionManager().getTcComm()).getWeightForWorkerComm(0) != 1)
           && (((TCCommImpl) commsMgr.getConnectionManager().getTcComm()).getWeightForWorkerComm(1) != 1)
           && (((TCCommImpl) commsMgr.getConnectionManager().getTcComm()).getWeightForWorkerComm(2) != 1)) {
      System.out.print("~");
      ThreadUtil.reallySleep(5000);
    }

    // case 3: connecting three more clients through server ports
    ClientMessageChannel client4 = createClientMsgCh(serverPort);
    ClientMessageChannel client5 = createClientMsgCh(serverPort);
    ClientMessageChannel client6 = createClientMsgCh(serverPort);

    client4.open();
    client5.open();
    client6.open();

    ThreadUtil.reallySleep(2000);
    assertTrue(client4.isConnected());
    assertTrue(client5.isConnected());
    assertTrue(client6.isConnected());

    Assert.assertEquals(2, ((TCCommImpl) commsMgr.getConnectionManager().getTcComm()).getWeightForWorkerComm(0));
    Assert.assertEquals(2, ((TCCommImpl) commsMgr.getConnectionManager().getTcComm()).getWeightForWorkerComm(1));
    Assert.assertEquals(2, ((TCCommImpl) commsMgr.getConnectionManager().getTcComm()).getWeightForWorkerComm(2));

    ensureThreadCount(OnceAndOnlyOnceProtocolNetworkLayerFactoryImpl.RESTORE_TIMERTHREAD_NAME, 1);

    proxy.stop();

    // reconnect window should get closed after this
    ThreadUtil.reallySleep(L1_RECONNECT_TIMEOUT);

    System.out.println("XXX waiting for client count 3");
    while (((((TCCommImpl) commsMgr.getConnectionManager().getTcComm()).getWeightForWorkerComm(0))
            + (((TCCommImpl) commsMgr.getConnectionManager().getTcComm()).getWeightForWorkerComm(1)) + (((TCCommImpl) commsMgr
        .getConnectionManager().getTcComm()).getWeightForWorkerComm(2))) != 3) {
      System.out.print("~");
      ThreadUtil.reallySleep(5000);
    }

    ensureThreadCount(OnceAndOnlyOnceProtocolNetworkLayerFactoryImpl.RESTORE_TIMERTHREAD_NAME, 1);

    listener.stop(5000);
  }

  private void ensureThreadCount(String absentThreadName, int count) {
    Thread[] allThreads = ThreadDumpUtil.getAllThreads();
    int tmp = 0;
    for (Thread t : allThreads) {
      System.out.println("XXX " + t);
      if (t.getName().contains(absentThreadName)) {
        tmp++;
      }
    }
    Assert.assertEquals(count, tmp);
  }

}
