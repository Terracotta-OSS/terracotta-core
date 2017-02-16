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
package com.tc.net.core;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.ClientID;
import com.tc.net.ServerID;
import com.tc.net.TCSocketAddress;
import com.tc.net.protocol.NetworkStackHarnessFactory;
import com.tc.net.protocol.PlainNetworkStackHarnessFactory;
import com.tc.net.protocol.delivery.OOONetworkStackHarnessFactory;
import com.tc.net.protocol.delivery.OnceAndOnlyOnceProtocolNetworkLayerFactoryImpl;
import com.tc.net.protocol.tcm.ClientMessageChannel;
import com.tc.net.protocol.tcm.CommunicationsManager;
import com.tc.net.protocol.tcm.CommunicationsManagerImpl;
import com.tc.net.protocol.tcm.GeneratedMessageFactory;
import com.tc.net.protocol.tcm.NetworkListener;
import com.tc.net.protocol.tcm.NullMessageMonitor;
import com.tc.net.protocol.tcm.TCMessage;
import com.tc.net.protocol.tcm.TCMessageRouterImpl;
import com.tc.net.protocol.tcm.TCMessageType;
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
import com.tc.properties.TCPropertiesConsts;

import java.net.InetAddress;
import java.util.Collections;
import org.junit.Ignore;

// TODO: Fix test
@Ignore
public class OOOReconnectTimeoutTest extends TCTestCase {
  //

  TCLogger          logger               = TCLogging.getLogger(TCWorkerCommManager.class);
  private final int L1_RECONNECT_TIMEOUT = 15000;

  private ClientMessageChannel createClientMsgCh() {
    return createClientMsgCh(true);
  }

  private ClientMessageChannel createClientMsgCh(boolean ooo) {

    CommunicationsManager clientComms = new CommunicationsManagerImpl("Client-TestCommsMgr", new NullMessageMonitor(),
                                                                      getNetworkStackHarnessFactory(ooo),
                                                                      new NullConnectionPolicy());

    ClientMessageChannel clientMsgCh = clientComms
        .createClientChannel(new NullSessionManager(),
                             -1,
                             1000, true);
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
                                                                       .getPropertiesFor(TCPropertiesConsts.L2_L1_HEALTH_CHECK_CATEGORY),
                                                                                               "Test Server"),
                                                                   new ServerID(),
                                                                   new TransportHandshakeErrorNullHandler(),
                                                                   Collections.<TCMessageType, Class<? extends TCMessage>>emptyMap(),
                                                                   Collections.<TCMessageType, GeneratedMessageFactory>emptyMap(), null);
    NetworkListener listener = commsMgr.createListener(new TCSocketAddress(0), true,
                                                       new DefaultConnectionIdFactory());
    listener.start(Collections.<ClientID>emptySet());
    int serverPort = listener.getBindPort();

    int proxyPort = new PortChooser().chooseRandomPort();
    TCPProxy proxy = new TCPProxy(proxyPort, InetAddress.getByName("localhost"), serverPort, 0, false, null);
    proxy.start();

    ClientMessageChannel client1 = createClientMsgCh();
    ClientMessageChannel client2 = createClientMsgCh();
    ClientMessageChannel client3 = createClientMsgCh();
    
    ConnectionInfo connectTo = new ConnectionInfo("localhost", proxyPort);

    client1.open(connectTo);
    client2.open(connectTo);
    client3.open(connectTo);

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

    connectTo = new ConnectionInfo("localhost", serverPort);
    // case 3: connecting three more clients through server ports
    ClientMessageChannel client4 = createClientMsgCh();
    ClientMessageChannel client5 = createClientMsgCh();
    ClientMessageChannel client6 = createClientMsgCh();

    client4.open(connectTo);
    client5.open(connectTo);
    client6.open(connectTo);

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
    ThreadUtil.reallySleep(L1_RECONNECT_TIMEOUT + 5);

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
