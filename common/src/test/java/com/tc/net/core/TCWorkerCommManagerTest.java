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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tc.net.ServerID;
import com.tc.net.protocol.NetworkStackHarnessFactory;
import com.tc.net.protocol.PlainNetworkStackHarnessFactory;
import com.tc.net.protocol.tcm.ClientMessageChannel;
import com.tc.net.protocol.tcm.CommunicationsManager;
import com.tc.net.protocol.tcm.CommunicationsManagerImpl;
import com.tc.net.protocol.tcm.GeneratedMessageFactory;
import com.tc.net.protocol.tcm.NetworkListener;
import com.tc.net.protocol.tcm.NullMessageMonitor;
import com.tc.net.protocol.tcm.TCMessageRouterImpl;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.net.protocol.transport.ClientMessageTransport;
import com.tc.net.protocol.transport.ConnectionID;
import com.tc.net.protocol.transport.DefaultConnectionIdFactory;
import com.tc.net.protocol.transport.HealthCheckerConfigImpl;
import com.tc.net.protocol.transport.MessageTransport;
import com.tc.net.protocol.transport.NullConnectionPolicy;
import com.tc.net.protocol.transport.TransportHandshakeErrorContext;
import com.tc.net.protocol.transport.TransportHandshakeErrorHandler;
import com.tc.net.protocol.transport.TransportHandshakeErrorNullHandler;
import com.tc.net.protocol.transport.TransportHandshakeMessage;
import com.tc.net.protocol.transport.TransportMessageFactoryImpl;
import com.tc.net.protocol.transport.TransportNetworkStackHarnessFactory;
import com.tc.net.protocol.transport.WireProtocolAdaptorFactoryImpl;
import com.tc.net.proxy.TCPProxy;
import com.tc.properties.TCPropertiesImpl;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;
import com.tc.util.CallableWaiter;
import com.tc.util.TCTimeoutException;
import com.tc.util.concurrent.ThreadUtil;
import com.tc.properties.TCPropertiesConsts;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;
import org.junit.Ignore;
import org.terracotta.utilities.test.net.PortManager;
import com.tc.net.protocol.tcm.TCAction;

public class TCWorkerCommManagerTest extends TCTestCase {

  Logger logger = LoggerFactory.getLogger(TCWorkerCommManager.class);
  List<ClientMessageTransport> transports = new ArrayList<ClientMessageTransport>();
  private final List<TCConnectionManager> clientConnectionMgrs = Collections.synchronizedList(new ArrayList<>());

  public TCWorkerCommManagerTest() {

  }

  private synchronized ClientMessageTransport createClient(String clientName) {
    TCConnectionManager connection = new TCConnectionManagerImpl("Client-TestCommMgr", 0, new ClearTextBufferManagerFactory());
    clientConnectionMgrs.add(connection);
    CommunicationsManager commsMgr = new CommunicationsManagerImpl(new NullMessageMonitor(),
                                                                   new TransportNetworkStackHarnessFactory(),
                                                                   connection,
                                                                   new NullConnectionPolicy());

    ClientMessageTransport cmt = new ClientMessageTransport(commsMgr.getConnectionManager(), createHandshakeErrorHandler(), new TransportMessageFactoryImpl(),
                                      new WireProtocolAdaptorFactoryImpl(), TransportHandshakeMessage.NO_CALLBACK_PORT, 1000);
    transports.add(cmt);
    return cmt;
  }

  private NetworkStackHarnessFactory getNetworkStackHarnessFactory() {
    NetworkStackHarnessFactory networkStackHarnessFactory;
    networkStackHarnessFactory = new PlainNetworkStackHarnessFactory();
    return networkStackHarnessFactory;
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    Assert.assertTrue(this.clientConnectionMgrs.isEmpty());
  }

  public void testReaderandWriterCommThread() throws Exception {
    // comms manager with 4 worker comms
    TCConnectionManager connMgr = new TCConnectionManagerImpl("Server-TestCommsMgr", 4, new ClearTextBufferManagerFactory());
    CommunicationsManager commsMgr = new CommunicationsManagerImpl(new NullMessageMonitor(),
                                                                   new TransportNetworkStackHarnessFactory(),
                                                                   connMgr,
                                                                   new NullConnectionPolicy());
    NetworkListener listener = commsMgr.createListener(new InetSocketAddress(0), (c)->true,
                                                       new DefaultConnectionIdFactory(), (MessageTransport t)->true);
    listener.start(Collections.<ConnectionID>emptySet());
    int port = listener.getBindPort();

    ClientMessageTransport client1 = createClient("client1");
    ClientMessageTransport client2 = createClient("client2");
    ClientMessageTransport client3 = createClient("client3");
    ClientMessageTransport client4 = createClient("client4");
    InetSocketAddress serverAddress = InetSocketAddress.createUnresolved("localhost", port);

    client1.open(serverAddress);
    client2.open(serverAddress);
    client3.open(serverAddress);
    client4.open(serverAddress);

    waitForConnected(client1, client2, client3, client4);

    waitForWeight(commsMgr, 0, 1);
    waitForWeight(commsMgr, 1, 1);
    waitForWeight(commsMgr, 2, 1);
    waitForWeight(commsMgr, 3, 1);

    for (int i = 0; i < 4; i++) {
      CoreNIOServices workerI = ((TCCommImpl) commsMgr.getConnectionManager().getTcComm()).getWorkerComm(i);
      waitForRead(workerI);
      Assert.eval(workerI.getReaderComm().getTotalBytesWritten() <= 0);

      Assert.eval(workerI.getWriterComm().getTotalBytesRead() <= 0);
      waitForWritten(workerI);

      Assert.eval(workerI.getTotalBytesRead() > 0);
      Assert.eval(workerI.getTotalBytesWritten() > 0);

    }

    client1.close();
    client2.close();
    client3.close();
    client4.close();

    listener.stop(5000);
    commsMgr.shutdown();
    connMgr.shutdown();
  }

  public void testWorkerCommDistributionAfterClose() throws Exception {
    TCConnectionManager connMgr = new TCConnectionManagerImpl("Server-TestCommsMgr", 3, new ClearTextBufferManagerFactory());
    // comms manager with 3 worker comms
    CommunicationsManager commsMgr = new CommunicationsManagerImpl(new NullMessageMonitor(),
                                                                   getNetworkStackHarnessFactory(),
                                                                   connMgr,
                                                                   new NullConnectionPolicy());
    NetworkListener listener = commsMgr.createListener(new InetSocketAddress(0), (c)->true,
                                                       new DefaultConnectionIdFactory(), (MessageTransport t)->true);
    listener.start(Collections.<ConnectionID>emptySet());
    int port = listener.getBindPort();

    InetSocketAddress serverAddress = InetSocketAddress.createUnresolved("localhost", port);

    ClientMessageChannel client1 = createClientMsgCh();
    ClientMessageChannel client2 = createClientMsgCh();
    ClientMessageChannel client3 = createClientMsgCh();

    client1.open(serverAddress);
    client2.open(serverAddress);
    client3.open(serverAddress);

    waitForConnected(client1, client2, client3);

    Assert.assertEquals(1, ((TCCommImpl) commsMgr.getConnectionManager().getTcComm()).getWeightForWorkerComm(0));
    Assert.assertEquals(1, ((TCCommImpl) commsMgr.getConnectionManager().getTcComm()).getWeightForWorkerComm(1));
    Assert.assertEquals(1, ((TCCommImpl) commsMgr.getConnectionManager().getTcComm()).getWeightForWorkerComm(2));

    // case 1 :
    // two client closes their connections

    client1.close();
    client2.close();

    waitForTotalWeights(commsMgr, 3, 1);

    ClientMessageChannel client4 = createClientMsgCh();
    ClientMessageChannel client5 = createClientMsgCh();

    // two clients open new connection
    client4.open(serverAddress);
    client5.open(serverAddress);

    waitForConnected(client4, client5);

    Assert.assertEquals(1, ((TCCommImpl) commsMgr.getConnectionManager().getTcComm()).getWeightForWorkerComm(0));
    Assert.assertEquals(1, ((TCCommImpl) commsMgr.getConnectionManager().getTcComm()).getWeightForWorkerComm(1));
    Assert.assertEquals(1, ((TCCommImpl) commsMgr.getConnectionManager().getTcComm()).getWeightForWorkerComm(2));

    commsMgr.getConnectionManager().closeAllConnections();

    waitForWeight(commsMgr, 0, 0);
    waitForWeight(commsMgr, 1, 0);
    waitForWeight(commsMgr, 2, 0);

    listener.stop(5000);
    commsMgr.shutdown();
    connMgr.shutdown();
  }

  @Ignore("this test expects add more weight from a thread not able to do it")
  public void WorkerCommDistributionAfterAddMoreWeight() throws Exception {
    TCConnectionManager connMgr = new TCConnectionManagerImpl("Server-TestCommsMgr", 3, new ClearTextBufferManagerFactory());
    // comms manager with 3 worker comms
    CommunicationsManager commsMgr = new CommunicationsManagerImpl(new NullMessageMonitor(),
                                                                   getNetworkStackHarnessFactory(),
                                                                   connMgr,
                                                                   new NullConnectionPolicy());
    NetworkListener listener = commsMgr.createListener(new InetSocketAddress(0), (c)->true,
                                                       new DefaultConnectionIdFactory(), (MessageTransport t)->true);
    listener.start(Collections.<ConnectionID>emptySet());
    int port = listener.getBindPort();

    InetSocketAddress serverAddress = InetSocketAddress.createUnresolved("localhost", port);

    ClientMessageChannel client1 = createClientMsgCh();
    ClientMessageChannel client2 = createClientMsgCh();
    ClientMessageChannel client3 = createClientMsgCh();

    client1.open(serverAddress);
    waitForConnected(client1);

    TCConnection conns[] = commsMgr.getConnectionManager().getAllConnections();
    Assert.eval(conns.length == 1);

    client2.open(serverAddress);
    client3.open(serverAddress);

    waitForConnected(client2, client3);

    Assert.assertEquals(1, ((TCCommImpl) commsMgr.getConnectionManager().getTcComm()).getWeightForWorkerComm(0));
    Assert.assertEquals(1, ((TCCommImpl) commsMgr.getConnectionManager().getTcComm()).getWeightForWorkerComm(1));
    Assert.assertEquals(1, ((TCCommImpl) commsMgr.getConnectionManager().getTcComm()).getWeightForWorkerComm(2));

    ClientMessageChannel client4 = createClientMsgCh();
    ClientMessageChannel client5 = createClientMsgCh();

    // four clients open new connection
    client4.open(serverAddress);
    client5.open(serverAddress);

    waitForConnected(client4, client5);

    Assert.assertEquals(2, ((TCCommImpl) commsMgr.getConnectionManager().getTcComm()).getWeightForWorkerComm(0));
    Assert.assertEquals(2, ((TCCommImpl) commsMgr.getConnectionManager().getTcComm()).getWeightForWorkerComm(1));
    Assert.assertEquals(2, ((TCCommImpl) commsMgr.getConnectionManager().getTcComm()).getWeightForWorkerComm(2));

    ClientMessageChannel client6 = createClientMsgCh();
    ClientMessageChannel client7 = createClientMsgCh();
    ClientMessageChannel client8 = createClientMsgCh();

    client6.open(serverAddress);
    client7.open(serverAddress);
    client8.open(serverAddress);

    waitForConnected(client6, client7, client8);

    Assert.assertEquals(3, ((TCCommImpl) commsMgr.getConnectionManager().getTcComm()).getWeightForWorkerComm(0));
    Assert.assertEquals(3, ((TCCommImpl) commsMgr.getConnectionManager().getTcComm()).getWeightForWorkerComm(1));
    Assert.assertEquals(3, ((TCCommImpl) commsMgr.getConnectionManager().getTcComm()).getWeightForWorkerComm(2));

    Assert.assertEquals(4, ((TCCommImpl) commsMgr.getConnectionManager().getTcComm()).getWeightForWorkerComm(0));
    client1.close();

    waitForWeight(commsMgr, 0, 1);
    Assert.assertEquals(3, ((TCCommImpl) commsMgr.getConnectionManager().getTcComm()).getWeightForWorkerComm(1));
    Assert.assertEquals(3, ((TCCommImpl) commsMgr.getConnectionManager().getTcComm()).getWeightForWorkerComm(2));

  }

  private ClientMessageChannel createClientMsgCh() {
    TCConnectionManager connection = new TCConnectionManagerImpl("Client-TestCommMgr", 0, new ClearTextBufferManagerFactory());
    clientConnectionMgrs.add(connection);
    CommunicationsManager clientComms = new CommunicationsManagerImpl(new NullMessageMonitor(),
                                                                      getNetworkStackHarnessFactory(),
                                                                      connection,
                                                                      new NullConnectionPolicy());

    ClientMessageChannel clientMsgCh = clientComms
        .createClientChannel(ProductID.STRIPE,
                             1000);
    return clientMsgCh;
  }

  @Ignore("this test expects exact distribution semantics but the implementation is best efforts")
  public void testWorkerCommDistributionAfterReconnect() throws Exception {
    boolean ignored = true; // these old tests don't use annotations
    if (!ignored) {
    // comms manager with 3 worker comms
      Properties props = new Properties();
      TCPropertiesImpl
                                 .getProperties()
                                 .getPropertiesFor(TCPropertiesConsts.L2_L1_HEALTH_CHECK_CATEGORY).addAllPropertiesTo(props);
      props.list(System.out);
      HealthCheckerConfigImpl config = new HealthCheckerConfigImpl(1000, 1000, 1, "test server", false, 1, 1);
      TCConnectionManager connMgr = new TCConnectionManagerImpl("Server-TestCommsMgr", 3, new ClearTextBufferManagerFactory());
      CommunicationsManager commsMgr = new CommunicationsManagerImpl(new NullMessageMonitor(),
                                                                     new TCMessageRouterImpl(),
                                                                     getNetworkStackHarnessFactory(),
                                                                     connMgr,
                                                                     new NullConnectionPolicy(),
                                                                     config,
                                                                     new ServerID(),
                                                                     new TransportHandshakeErrorNullHandler(),
                                                                     Collections.<TCMessageType, Class<? extends TCAction>>emptyMap(),
                                                                     Collections.<TCMessageType, GeneratedMessageFactory>emptyMap());
      NetworkListener listener = commsMgr.createListener(new InetSocketAddress(0), (c)->true,
                                                         new DefaultConnectionIdFactory(), (MessageTransport t)->true);
      listener.start(Collections.<ConnectionID>emptySet());
      int serverPort = listener.getBindPort();

      try {
        try (PortManager.PortRef portRef = PortManager.getInstance().reservePort()) {
          int proxyPort = portRef.port();
          TCPProxy proxy = new TCPProxy(proxyPort, InetAddress.getByName("localhost"), serverPort, 0, false, null);
          try {
            proxy.start();

            InetSocketAddress serverAddress = InetSocketAddress.createUnresolved("localhost", proxyPort);

            ClientMessageChannel client1 = createClientMsgCh();
            ClientMessageChannel client2 = createClientMsgCh();
            ClientMessageChannel client3 = createClientMsgCh();

            client1.open(serverAddress);
            client2.open(serverAddress);
            client3.open(serverAddress);

            waitForConnected(client1, client2, client3);

            Assert.assertEquals(1, ((TCCommImpl)commsMgr.getConnectionManager().getTcComm()).getWeightForWorkerComm(0));
            Assert.assertEquals(1, ((TCCommImpl)commsMgr.getConnectionManager().getTcComm()).getWeightForWorkerComm(1));
            Assert.assertEquals(1, ((TCCommImpl)commsMgr.getConnectionManager().getTcComm()).getWeightForWorkerComm(2));

            // case 1 : network problems .. both ends getting events
            proxy.stop();

            waitForWeight(commsMgr, 0, 0);
            waitForWeight(commsMgr, 1, 0);
            waitForWeight(commsMgr, 2, 0);

            proxy.start();

            waitForConnected(client1, client2, client3);

            Assert.assertEquals(1, ((TCCommImpl)commsMgr.getConnectionManager().getTcComm()).getWeightForWorkerComm(0));
            Assert.assertEquals(1, ((TCCommImpl)commsMgr.getConnectionManager().getTcComm()).getWeightForWorkerComm(1));
            Assert.assertEquals(1, ((TCCommImpl)commsMgr.getConnectionManager().getTcComm()).getWeightForWorkerComm(2));

            // case 2: problem with the client side connections .. but server still thinks clients are connected
            proxy.closeClientConnections(true, false);

            System.out.println("XXX waiting for clients to reconnect");
            waitForWeight(commsMgr, 0, 1);
            waitForWeight(commsMgr, 1, 1);
            waitForWeight(commsMgr, 2, 1);

            // case 3: connecting three more clients through server ports

            ClientMessageChannel client4 = createClientMsgCh();
            ClientMessageChannel client5 = createClientMsgCh();
            ClientMessageChannel client6 = createClientMsgCh();

            serverAddress = InetSocketAddress.createUnresolved("localhost", serverPort);

            client4.open(serverAddress);
            client5.open(serverAddress);
            client6.open(serverAddress);

            waitForConnected(client4, client5, client6);

            // Issue #414:  This intermittently fails so collect more information regarding the state of the workers.  While the
            //  test expects them each to have 2 clients, we fail when one of them has a different number.  I suspect that there
            //  is a race in how the connections are distributed to the worker threads meaning that 2 concurrent connection
            //  attempts may choose the same worker, not realizing that each of them changes its weight.
            int weightFor0 = ((TCCommImpl)commsMgr.getConnectionManager().getTcComm()).getWeightForWorkerComm(0);
            int weightFor1 = ((TCCommImpl)commsMgr.getConnectionManager().getTcComm()).getWeightForWorkerComm(1);
            int weightFor2 = ((TCCommImpl)commsMgr.getConnectionManager().getTcComm()).getWeightForWorkerComm(2);
            System.out.println("Issue #414 debug weights: " + weightFor0 + ", " + weightFor1 + ", " + weightFor2);
            Assert.assertEquals(6, weightFor0 + weightFor1 + weightFor2);
            // distribution may not be even since weighting is best efforts but horribly skewed
            Assert.assertTrue(0 < weightFor0);
            Assert.assertTrue(0 < weightFor1);
            Assert.assertTrue(0 < weightFor2);

            // case 4: closing all connections from server side
            System.out.println("XXX closing all client connections");
            commsMgr.getConnectionManager().closeAllConnections();

            // all clients should reconnect and should be distributed fairly among the worker comms.

            // After connection close and reconnects, the weight balance depends on when comms get the close connection events
            System.out.println("XXX waiting for all clients reconnect");
            waitForTotalWeights(commsMgr, 3, 6);

            // case 5: server detecting long gcs and kicking out the clients
            proxy.setDelay(15 * 1000);

            System.out.println("XXX waiting for HC to kick out the clients those who connected thru proxy ports");
            waitForTotalWeights(commsMgr, 3, 3);

            proxy.setDelay(0);

            ThreadUtil.reallySleep(10000);
            System.out.println("XXX server after seeing client long GC will not open reconnect window for it");
            Assert.assertEquals(3, (((TCCommImpl)commsMgr.getConnectionManager().getTcComm()).getWeightForWorkerComm(0))
                + (((TCCommImpl)commsMgr.getConnectionManager().getTcComm()).getWeightForWorkerComm(1))
                + (((TCCommImpl)commsMgr.getConnectionManager().getTcComm()).getWeightForWorkerComm(2)));

            client1.close();
            client2.close();
            client3.close();
            client4.close();
            client5.close();
            client6.close();
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
        commsMgr.shutdown();
        connMgr.shutdown();
      }
    }
  }

  private static void waitForConnected(final ClientMessageChannel... channels) throws Exception {
    CallableWaiter.waitOnCallable(new Callable<Boolean>() {
      @Override
      public Boolean call() throws Exception {
        for (ClientMessageChannel channel : channels) {
          if (!channel.isConnected()) {
            return false;
          }
        }
        return true;
      }
    });
  }

  private static void waitForConnected(final ClientMessageTransport... transports) throws Exception {
    CallableWaiter.waitOnCallable(new Callable<Boolean>() {
      @Override
      public Boolean call() throws Exception {
        for (ClientMessageTransport transport : transports) {
          if (!transport.isConnected()) {
            return false;
          }
        }
        return true;
      }
    });
  }

  private static void waitForWeight(final CommunicationsManager communicationsManager, final int commId, final int weight) throws Exception {
    CallableWaiter.waitOnCallable(new Callable<Boolean>() {
      @Override
      public Boolean call() throws Exception {
        return ((TCCommImpl)communicationsManager.getConnectionManager()
            .getTcComm()).getWeightForWorkerComm(commId) == weight;
      }
    });
  }

  private static void waitForTotalWeights(final CommunicationsManager communicationsManager, final int workers, final int weight) throws Exception {
    CallableWaiter.waitOnCallable(new Callable<Boolean>() {
      @Override
      public Boolean call() throws Exception {
        int total = 0;
        for (int i = 0; i < workers; i++) {
          total += ((TCCommImpl)communicationsManager.getConnectionManager()
              .getTcComm()).getWeightForWorkerComm(i);
        }
        System.out.println("total weight " + total + " expected " + weight);
        return total == weight;
      }
    });
  }

  private static void waitForRead(final CoreNIOServices commThread) throws Exception {
    CallableWaiter.waitOnCallable(new Callable<Boolean>() {
      @Override
      public Boolean call() throws Exception {
        return commThread.getReaderComm().getTotalBytesRead() > 0;
      }
    });
  }

  private static void waitForWritten(final CoreNIOServices commThread) throws Exception {
    CallableWaiter.waitOnCallable(new Callable<Boolean>() {
      @Override
      public Boolean call() throws Exception {
        return commThread.getWriterComm().getTotalBytesWritten() > 0;
      }
    });
  }

  private TransportHandshakeErrorHandler createHandshakeErrorHandler() {
    return new TransportHandshakeErrorHandler() {

      @Override
      public void handleHandshakeError(TransportHandshakeErrorContext e) {
        new UnsupportedOperationException(e.toString()).printStackTrace();
      }

    };
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    for (ClientMessageTransport t : transports) {
      t.close();
    }
    transports.clear();
    clientConnectionMgrs.forEach(TCConnectionManager::shutdown);
    clientConnectionMgrs.clear();
  }
}
