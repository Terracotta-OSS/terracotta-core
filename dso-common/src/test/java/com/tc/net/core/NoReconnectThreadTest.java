/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.net.core;


import com.tc.net.ServerID;
import com.tc.net.TCSocketAddress;
import com.tc.net.protocol.NetworkStackHarnessFactory;
import com.tc.net.protocol.PlainNetworkStackHarnessFactory;
import com.tc.net.protocol.delivery.OOONetworkStackHarnessFactory;
import com.tc.net.protocol.delivery.OnceAndOnlyOnceProtocolNetworkLayerFactoryImpl;
import com.tc.net.protocol.tcm.ChannelEvent;
import com.tc.net.protocol.tcm.ChannelEventListener;
import com.tc.net.protocol.tcm.ChannelEventType;
import com.tc.net.protocol.tcm.ClientMessageChannel;
import com.tc.net.protocol.tcm.CommunicationsManager;
import com.tc.net.protocol.tcm.CommunicationsManagerImpl;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.NetworkListener;
import com.tc.net.protocol.tcm.NullMessageMonitor;
import com.tc.net.protocol.tcm.TCMessageRouterImpl;
import com.tc.net.protocol.transport.ClientConnectionEstablisher;
import com.tc.net.protocol.transport.DefaultConnectionIdFactory;
import com.tc.net.protocol.transport.HealthCheckerConfigImpl;
import com.tc.net.protocol.transport.NullConnectionPolicy;
import com.tc.net.protocol.transport.TransportHandshakeErrorNullHandler;
import com.tc.net.proxy.TCPProxy;
import com.tc.object.session.NullSessionManager;
import com.tc.properties.L1ReconnectConfigImpl;
import com.tc.properties.TCPropertiesImpl;
import com.tc.test.TCTestCase;
import com.tc.util.PortChooser;
import com.tc.util.concurrent.ThreadUtil;
import com.tc.util.runtime.ThreadDumpUtil;

import java.net.InetAddress;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

public class NoReconnectThreadTest extends TCTestCase implements ChannelEventListener {
  private final int             L1_RECONNECT_TIMEOUT = 5000;
  private final AtomicInteger connections          = new AtomicInteger(0);

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    connections.set(0);
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

  private ClientMessageChannel createClientMsgCh(int port) {
    return createClientMsgCh(port, false);
  }

  private ClientMessageChannel createClientMsgCh(int port, boolean ooo) {

    CommunicationsManager clientComms = new CommunicationsManagerImpl("TestCommsMgr", new NullMessageMonitor(),
                                                                      getNetworkStackHarnessFactory(ooo),
                                                                      new NullConnectionPolicy());
    ClientMessageChannel clientMsgCh = clientComms
        .createClientChannel(new NullSessionManager(),
                             0,
                             "localhost",
                             port,
                             1000,
                             new ConnectionAddressProvider(
                                                           new ConnectionInfo[] { new ConnectionInfo("localhost", port) }));
    return clientMsgCh;
  }

  public void testConnectionEstablisherThreadExit() throws Exception {

    CommunicationsManager serverCommsMgr = new CommunicationsManagerImpl("TestCommsMgr-Server",
                                                                         new NullMessageMonitor(),
                                                                         new TCMessageRouterImpl(),
                                                                         getNetworkStackHarnessFactory(false),
                                                                         new NullConnectionPolicy(), 3,
                                                                         new HealthCheckerConfigImpl(TCPropertiesImpl
                                                                             .getProperties()
                                                                             .getPropertiesFor("l2.healthcheck.l2"),
                                                                                                     "Test Server"),
                                                                         new ServerID(),
                                                                         new TransportHandshakeErrorNullHandler(),
                                                                         Collections.EMPTY_MAP, Collections.EMPTY_MAP,
                                                                         null);
    NetworkListener listener = serverCommsMgr.createListener(new NullSessionManager(), new TCSocketAddress(0), true,
                                                             new DefaultConnectionIdFactory());
    listener.start(Collections.EMPTY_SET);
    int serverPort = listener.getBindPort();

    int proxyPort = new PortChooser().chooseRandomPort();
    TCPProxy proxy = new TCPProxy(proxyPort, InetAddress.getByName("localhost"), serverPort, 0, false, null);
    proxy.start();

    ClientMessageChannel client1 = createClientMsgCh(proxyPort);
    ClientMessageChannel client2 = createClientMsgCh(proxyPort);
    ClientMessageChannel client3 = createClientMsgCh(proxyPort);

    client1.addListener(this);
    client1.open();

    client2.addListener(this);
    client2.open();

    client3.addListener(this);
    client3.open();

    ThreadUtil.reallySleep(2000);
    assertTrue(client1.isConnected());
    assertTrue(client2.isConnected());
    assertTrue(client3.isConnected());

    // closing all connections from server side
    System.err.println("XXX closing all client connections");
    serverCommsMgr.getConnectionManager().closeAllConnections(1000);

    while (connections.get() != 0) {
      ThreadUtil.reallySleep(2000);
      System.err.println(".");
    }

    // None of the clients should start the ClientConnectionEstablisher Thread for reconnect as the Client
    // CommsManager is created with reconnect 0. we might need to wait till the created CCE gets quit request.
    while (getThreadCount(ClientConnectionEstablisher.RECONNECT_THREAD_NAME) > 0) {
      ThreadUtil.reallySleep(1000);
      System.err.println("-");
    }

    listener.stop(5000);

  }

  public void testConnectionEstablisherThreadExitAfterOOO() throws Exception {
    CommunicationsManager serverCommsMgr = new CommunicationsManagerImpl("TestCommsMgr-Server",
                                                                         new NullMessageMonitor(),
                                                                         new TCMessageRouterImpl(),
                                                                         getNetworkStackHarnessFactory(true),
                                                                         new NullConnectionPolicy(), 3,
                                                                         new HealthCheckerConfigImpl(TCPropertiesImpl
                                                                             .getProperties()
                                                                             .getPropertiesFor("l2.healthcheck.l2"),
                                                                                                     "Test Server"),
                                                                         new ServerID(),
                                                                         new TransportHandshakeErrorNullHandler(),
                                                                         Collections.EMPTY_MAP, Collections.EMPTY_MAP,
                                                                         null);
    NetworkListener listener = serverCommsMgr.createListener(new NullSessionManager(), new TCSocketAddress(0), true,
                                                             new DefaultConnectionIdFactory());
    listener.start(Collections.EMPTY_SET);
    int serverPort = listener.getBindPort();

    int proxyPort = new PortChooser().chooseRandomPort();
    TCPProxy proxy = new TCPProxy(proxyPort, InetAddress.getByName("localhost"), serverPort, 0, false, null);
    proxy.start();

    ClientMessageChannel client1 = createClientMsgCh(proxyPort, true);
    ClientMessageChannel client2 = createClientMsgCh(proxyPort, true);
    ClientMessageChannel client3 = createClientMsgCh(proxyPort, true);

    client1.addListener(this);
    client1.open();

    client2.addListener(this);
    client2.open();

    client3.addListener(this);
    client3.open();

    ThreadUtil.reallySleep(2000);
    assertTrue(client1.isConnected());
    assertTrue(client2.isConnected());
    assertTrue(client3.isConnected());

    // closing all connections from server side
    System.err.println("XXX closing all client connections");
    proxy.stop();

    // let the OOO settle down
    ThreadUtil.reallySleep(L1_RECONNECT_TIMEOUT);

    while (connections.get() != 0) {
      ThreadUtil.reallySleep(2000);
      System.err.println(".");
    }

    // None of the clients should start the ClientConnectionEstablisher Thread for reconnect as the Client CommsManager
    // is created with reconnect 0
    while (getThreadCount(ClientConnectionEstablisher.RECONNECT_THREAD_NAME) > 0) {
      ThreadUtil.reallySleep(1000);
      System.err.println("-");
    }

    listener.stop(5000);
  }

  private int getThreadCount(String absentThreadName) {
    Thread[] allThreads = ThreadDumpUtil.getAllThreads();
    int count = 0;
    for (Thread t : allThreads) {
      if (t.getName().contains(ClientConnectionEstablisher.RECONNECT_THREAD_NAME)) {
        count++;
      }
    }
    return count;
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  @Override
  public void notifyChannelEvent(ChannelEvent event) {
    MessageChannel channel = event.getChannel();

    if (ChannelEventType.CHANNEL_CLOSED_EVENT.matches(event)) {
      // test doesn't care
    } else if (ChannelEventType.TRANSPORT_DISCONNECTED_EVENT.matches(event)) {
      // simulating TCGrpoupManager->StateMachine->disconnect event handling
      channel.close();
      connections.decrementAndGet();
      System.out.println("XXX CLOSED " + event.getChannel());
    } else if (ChannelEventType.TRANSPORT_CONNECTED_EVENT.matches(event)) {
      connections.incrementAndGet();
      System.out.println("XXX CONNECTED " + event.getChannel());
    }
  }

}
