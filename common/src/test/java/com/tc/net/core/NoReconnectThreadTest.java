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


import com.tc.net.ServerID;
import com.tc.net.protocol.NetworkStackHarnessFactory;
import com.tc.net.protocol.PlainNetworkStackHarnessFactory;
import com.tc.net.protocol.tcm.ChannelEvent;
import com.tc.net.protocol.tcm.ChannelEventListener;
import com.tc.net.protocol.tcm.ChannelEventType;
import com.tc.net.protocol.tcm.ClientMessageChannel;
import com.tc.net.protocol.tcm.CommunicationsManager;
import com.tc.net.protocol.tcm.CommunicationsManagerImpl;
import com.tc.net.protocol.tcm.GeneratedMessageFactory;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.NetworkListener;
import com.tc.net.protocol.tcm.NullMessageMonitor;
import com.tc.net.protocol.tcm.TCMessageRouterImpl;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.net.protocol.transport.ClientConnectionEstablisher;
import com.tc.net.protocol.transport.DefaultConnectionIdFactory;
import com.tc.net.protocol.transport.HealthCheckerConfigImpl;
import com.tc.net.protocol.transport.MessageTransport;
import com.tc.net.protocol.transport.NullConnectionPolicy;
import com.tc.net.protocol.transport.TransportHandshakeErrorNullHandler;
import com.tc.net.proxy.TCPProxy;
import com.tc.properties.TCPropertiesImpl;
import com.tc.test.TCTestCase;
import com.tc.util.TCTimeoutException;
import com.tc.util.concurrent.ThreadUtil;
import com.tc.util.runtime.ThreadDumpUtil;
import com.tc.properties.TCPropertiesConsts;
import com.tc.util.Assert;

import org.terracotta.utilities.test.net.PortManager;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import com.tc.net.protocol.tcm.TCAction;

public class NoReconnectThreadTest extends TCTestCase implements ChannelEventListener {
  private final int             L1_RECONNECT_TIMEOUT = 5000;
  private final AtomicInteger connections          = new AtomicInteger(0);
  private int baseAsyncThreads;
  private final List<TCConnectionManager> clientConnectionMgrs = Collections.synchronizedList(new ArrayList<>());


  @Override
  protected void setUp() throws Exception {
    super.setUp();
    connections.set(0);
    baseAsyncThreads = getThreadCount(ClientConnectionEstablisher.RECONNECT_THREAD_NAME);
    Assert.assertTrue(clientConnectionMgrs.isEmpty());
  }

  private NetworkStackHarnessFactory getNetworkStackHarnessFactory() {
    NetworkStackHarnessFactory networkStackHarnessFactory;
    networkStackHarnessFactory = new PlainNetworkStackHarnessFactory();
    return networkStackHarnessFactory;
  }

  private ClientMessageChannel createClientMsgCh() {
    TCConnectionManager connMgr = new TCConnectionManagerImpl("TestCommMgr-Client", 0, new ClearTextBufferManagerFactory());
    clientConnectionMgrs.add(connMgr);
    CommunicationsManager clientComms = new CommunicationsManagerImpl(new NullMessageMonitor(),
                                                                      getNetworkStackHarnessFactory(),
                                                                      connMgr,
                                                                      new NullConnectionPolicy());
    ClientMessageChannel clientMsgCh = clientComms
        .createClientChannel(ProductID.SERVER,
                             1000);
    return clientMsgCh;
  }

  public void testConnectionEstablisherThreadExit() throws Exception {
    TCConnectionManager connectionMgr = new TCConnectionManagerImpl("TestCommsMgr-Server", 3, new ClearTextBufferManagerFactory());
    CommunicationsManager serverCommsMgr = new CommunicationsManagerImpl(
                                                                         new NullMessageMonitor(),
                                                                         new TCMessageRouterImpl(),
                                                                         getNetworkStackHarnessFactory(),
                                                                         connectionMgr,
                                                                         new NullConnectionPolicy(),
//            new DisabledHealthCheckerConfigImpl(),
                                                                         new HealthCheckerConfigImpl(TCPropertiesImpl
                                                                             .getProperties()
                                                                             .getPropertiesFor(TCPropertiesConsts.L2_L2_HEALTH_CHECK_CATEGORY),
                                                                                                     "Test Server"),
                                                                         new ServerID(),
                                                                         new TransportHandshakeErrorNullHandler(),
                                                                         Collections.<TCMessageType, Class<? extends TCAction>>emptyMap(),
                                                                         Collections.<TCMessageType, GeneratedMessageFactory>emptyMap()
    );
    NetworkListener listener = serverCommsMgr.createListener(new InetSocketAddress(0), (c)->true,
                                                             new DefaultConnectionIdFactory(), (MessageTransport t)->true);
    listener.start(Collections.emptySet());
    try {
      int serverPort = listener.getBindPort();

      try (PortManager.PortRef portRef = PortManager.getInstance().reservePort()) {
        int proxyPort = portRef.port();
        TCPProxy proxy = new TCPProxy(proxyPort, InetAddress.getByName("localhost"), serverPort, 0, false, null);
        try {
          proxy.start();

          ClientMessageChannel client1 = createClientMsgCh();
          ClientMessageChannel client2 = createClientMsgCh();
          ClientMessageChannel client3 = createClientMsgCh();

          InetSocketAddress serverAddress = new InetSocketAddress("localhost", proxyPort);

          client1.addListener(this);
          client1.open(serverAddress);

          client2.addListener(this);
          client2.open(serverAddress);

          client3.addListener(this);
          client3.open(serverAddress);

          ThreadUtil.reallySleep(2000);
          assertTrue(client1.isConnected());
          assertTrue(client2.isConnected());
          assertTrue(client3.isConnected());

          // closing all connections from server side
          System.err.println("XXX closing all client connections");
          serverCommsMgr.getConnectionManager().closeAllConnections();

          while (connections.get() != 0) {
            ThreadUtil.reallySleep(2000);
            System.err.println(".");
          }

          // None of the clients should start the ClientConnectionEstablisher Thread for reconnect as the Client
          // CommsManager is created with reconnect 0. we might need to wait till the created CCE gets quit request.
          while (getThreadCount(ClientConnectionEstablisher.RECONNECT_THREAD_NAME) > baseAsyncThreads) {
            ThreadUtil.reallySleep(1000);
            System.err.println("-");
          }
        } finally {
          proxy.stop();
        }
      }
    } finally {
      try {
        listener.stop(5000);
      } catch (TCTimeoutException e) {
        // ignored
      }
      serverCommsMgr.shutdown();
      connectionMgr.shutdown();
    }
  }

  private int getThreadCount(String absentThreadName) {
    Thread[] allThreads = ThreadDumpUtil.getAllThreads();
    int count = 0;
    for (Thread t : allThreads) {
      if (t.getName().contains(absentThreadName)) {
        count++;
      }
    }
    return count;
  }

  @Override
  protected void tearDown() throws Exception {
    clientConnectionMgrs.forEach(TCConnectionManager::shutdown);
    clientConnectionMgrs.clear();
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
