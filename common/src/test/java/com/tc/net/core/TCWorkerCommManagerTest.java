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

import com.tc.net.basic.BasicConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tc.net.protocol.NetworkStackHarnessFactory;
import com.tc.net.protocol.PlainNetworkStackHarnessFactory;
import com.tc.net.protocol.tcm.ClientMessageChannel;
import com.tc.net.protocol.tcm.CommunicationsManager;
import com.tc.net.protocol.tcm.CommunicationsManagerImpl;
import com.tc.net.protocol.tcm.NetworkListener;
import com.tc.net.protocol.tcm.NullMessageMonitor;
import com.tc.net.protocol.transport.ClientMessageTransport;
import com.tc.net.protocol.transport.ConnectionID;
import com.tc.net.protocol.transport.DefaultConnectionIdFactory;
import com.tc.net.protocol.transport.MessageTransport;
import com.tc.net.protocol.transport.NullConnectionPolicy;
import com.tc.net.protocol.transport.TransportHandshakeErrorContext;
import com.tc.net.protocol.transport.TransportHandshakeErrorHandler;
import com.tc.net.protocol.transport.TransportMessageFactoryImpl;
import com.tc.net.protocol.transport.TransportNetworkStackHarnessFactory;
import com.tc.net.protocol.transport.WireProtocolAdaptorFactoryImpl;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;
import com.tc.util.CallableWaiter;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

public class TCWorkerCommManagerTest extends TCTestCase {

  Logger logger = LoggerFactory.getLogger(TCWorkerCommManager.class);
  List<ClientMessageTransport> transports = new ArrayList<ClientMessageTransport>();
  private final List<TCConnectionManager> clientConnectionMgrs = Collections.synchronizedList(new ArrayList<>());

  public TCWorkerCommManagerTest() {

  }

  private synchronized ClientMessageTransport createClient(String clientName) {
    TCConnectionManager connection = new TCConnectionManagerImpl("Client-TestCommMgr-" + clientName, 0, new ClearTextSocketEndpointFactory());
    clientConnectionMgrs.add(connection);
    CommunicationsManager commsMgr = new CommunicationsManagerImpl(new NullMessageMonitor(),
                                                                   new TransportNetworkStackHarnessFactory(),
                                                                   connection,
                                                                   new NullConnectionPolicy());

    ClientMessageTransport cmt = new ClientMessageTransport(commsMgr.getConnectionManager(), createHandshakeErrorHandler(), new TransportMessageFactoryImpl(),
                                      new WireProtocolAdaptorFactoryImpl(), 1000);
    transports.add(cmt);
    return cmt;
  }

  private synchronized ClientMessageTransport createBasicClient(String clientName) {
    TCConnectionManager connection = new BasicConnectionManager(clientName, new ClearTextSocketEndpointFactory());
    clientConnectionMgrs.add(connection);
    CommunicationsManager commsMgr = new CommunicationsManagerImpl(new NullMessageMonitor(),
                                                                   new TransportNetworkStackHarnessFactory(),
                                                                   connection,
                                                                   new NullConnectionPolicy());

    ClientMessageTransport cmt = new ClientMessageTransport(commsMgr.getConnectionManager(), createHandshakeErrorHandler(), new TransportMessageFactoryImpl(),
                                      new WireProtocolAdaptorFactoryImpl(), 1000);
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
  public void testSimpleOpenAndClose() throws Exception {
    // comms manager with 4 worker comms
    logger.debug("Running target test");
    TCConnectionManager connMgr = new TCConnectionManagerImpl("Target-Server-TestCommsMgr", 1, new ClearTextSocketEndpointFactory());
    CommunicationsManager commsMgr = new CommunicationsManagerImpl(new NullMessageMonitor(),
                                                                   new TransportNetworkStackHarnessFactory(),
                                                                   connMgr,
                                                                   new NullConnectionPolicy());
    NetworkListener listener = commsMgr.createListener(new InetSocketAddress(0), (c)->true,
                                                       new DefaultConnectionIdFactory(), (MessageTransport t)->true);
    listener.start(Collections.<ConnectionID>emptySet());
    int port = listener.getBindPort();

    ClientMessageTransport client1 = createBasicClient("client1");

    InetSocketAddress serverAddress = InetSocketAddress.createUnresolved("localhost", port);

    client1.open(serverAddress);

    waitForConnected(client1);

    waitForWeight(commsMgr, 0, 1);

    client1.close();

    waitForTotalWeights(commsMgr, 1, 0);
    while (connMgr.getAllConnections().length >  0) {
      Thread.sleep(1000);
    }
    listener.stop(5000);
    commsMgr.shutdown();
    connMgr.shutdown();
  }
  
  public void testOverweight() throws Exception {
    TCConnectionManager connMgr = new TCConnectionManagerImpl("Target-Server-TestCommsMgr", 2, new ClearTextSocketEndpointFactory());
    CommunicationsManager commsMgr = new CommunicationsManagerImpl(new NullMessageMonitor(),
                                                                   new TransportNetworkStackHarnessFactory(),
                                                                   connMgr,
                                                                   new NullConnectionPolicy());
    NetworkListener listener = commsMgr.createListener(new InetSocketAddress(0), (c)->true,
                                                       new DefaultConnectionIdFactory(), (MessageTransport t)->true);
    listener.start(Collections.<ConnectionID>emptySet());
    int port = listener.getBindPort();
    
    ClientMessageTransport[] clients = new ClientMessageTransport[32];

    for (int x=0;x<clients.length;x++) {
      clients[x] = createBasicClient("client" + x);
    }

    InetSocketAddress serverAddress = InetSocketAddress.createUnresolved("localhost", port);

    for (ClientMessageTransport t : clients) {
      t.open(serverAddress);
    }

    waitForConnected(clients);
    waitForTotalWeights(commsMgr,2, 32);
    
    for (int x=0;x<clients.length;x++) {
      if (x%2 == 0) {
        clients[x].close();
      }
    }
        
    waitForTotalWeights(commsMgr,2, 16);

    TCCommImpl comm = (TCCommImpl)connMgr.getTcComm();
    Assert.assertEquals(0, comm.getWeightForWorkerComm(0));
    Assert.assertEquals(16, comm.getWeightForWorkerComm(1));
    
    TCConnection[] all = connMgr.getAllConnections();
    for (int x=0;x<all.length/2;x++) {
      ((TCConnectionImpl)all[x]).migrate();
    }
    waitForWeight(commsMgr,0,8);
    Assert.assertEquals(comm.getWeightForWorkerComm(0), 8);
    Assert.assertEquals(comm.getWeightForWorkerComm(1), 8);
  }

  public void testReaderandWriterCommThread() throws Exception {
    // comms manager with 4 worker comms
    logger.debug("Running target test");
    TCConnectionManager connMgr = new TCConnectionManagerImpl("Target-Server-TestCommsMgr", 4, new ClearTextSocketEndpointFactory());
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
      System.out.println("Activity on " + workerI.getName() + " " + workerI.getReaderComm().getTotalBytesRead() + " " + workerI.getWriterComm().getTotalBytesWritten()+ " " + workerI.getWeight());
    }
    CoreNIOServices mainL = ((TCCommImpl) commsMgr.getConnectionManager().getTcComm()).nioServiceThreadForNewConnection();
    System.out.println("Activity on " + mainL.getName() + " " + mainL.getReaderComm().getTotalBytesRead() + " " + mainL.getWriterComm().getTotalBytesWritten() + " " + mainL.getWeight());

    client1.close();
    client2.close();
    client3.close();
    client4.close();

    waitForTotalWeights(commsMgr, 4, 0);
    while (connMgr.getAllConnections().length >  0) {
      Thread.sleep(1000);
    }
    listener.stop(5000);
    commsMgr.shutdown();
    connMgr.shutdown();
  }

  public void testWorkerCommDistributionAfterClose() throws Exception {
    TCConnectionManager connMgr = new TCConnectionManagerImpl("Server-TestCommsMgr", 3, new ClearTextSocketEndpointFactory());
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

//  @Ignore("this test expects add more weight from a thread not able to do it")
  public void testWorkerCommDistributionAfterAddMoreWeight() throws Exception {
    TCConnectionManager connMgr = new TCConnectionManagerImpl("Server-TestCommsMgr", 3, new ClearTextSocketEndpointFactory());
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
    Assert.assertEquals(1, ((TCCommImpl) commsMgr.getConnectionManager().getTcComm()).getWeightForWorkerComm(2));

    ClientMessageChannel client6 = createClientMsgCh();
    ClientMessageChannel client7 = createClientMsgCh();
    ClientMessageChannel client8 = createClientMsgCh();

    client6.open(serverAddress);
    client7.open(serverAddress);
    client8.open(serverAddress);

    waitForConnected(client6, client7, client8);

    Assert.assertEquals(3, ((TCCommImpl) commsMgr.getConnectionManager().getTcComm()).getWeightForWorkerComm(0));
    Assert.assertEquals(3, ((TCCommImpl) commsMgr.getConnectionManager().getTcComm()).getWeightForWorkerComm(1));
    Assert.assertEquals(2, ((TCCommImpl) commsMgr.getConnectionManager().getTcComm()).getWeightForWorkerComm(2));

    client1.close();

    waitForTotalWeights(commsMgr, 3, 7);
    Assert.assertEquals(3, ((TCCommImpl) commsMgr.getConnectionManager().getTcComm()).getWeightForWorkerComm(1));
    Assert.assertEquals(2, ((TCCommImpl) commsMgr.getConnectionManager().getTcComm()).getWeightForWorkerComm(2));

  }

  private ClientMessageChannel createClientMsgCh() {
    TCConnectionManager connection = new TCConnectionManagerImpl("Client-TestCommMgr", 0, new ClearTextSocketEndpointFactory());
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
          System.out.println("checking connected " + transport.isConnected() + " " + transport.wasOpened());
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
        int w = ((TCCommImpl)communicationsManager.getConnectionManager()
            .getTcComm()).getWeightForWorkerComm(commId);
        System.out.println("waiting for id:" + commId + " weight " + w + " expected " + weight);
        return w == weight;
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
        System.out.println("Wait for read " + commThread.getName() + " " + commThread.getReaderComm().getTotalBytesRead() + " " +  + commThread.getWriterComm().getTotalBytesRead());
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
