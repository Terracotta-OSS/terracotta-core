/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.tcm;

import com.tc.cluster.DsoClusterImpl;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.L1ConfigurationSetupManager;
import com.tc.config.schema.setup.L2ConfigurationSetupManager;
import com.tc.config.schema.setup.TestConfigurationSetupManagerFactory;
import com.tc.lang.StartupHelper;
import com.tc.lang.TCThreadGroup;
import com.tc.lang.ThrowableHandler;
import com.tc.logging.TCLogging;
import com.tc.net.protocol.tcm.msgs.PingMessage;
import com.tc.object.BaseDSOTestCase;
import com.tc.object.DistributedObjectClient;
import com.tc.object.MockRemoteSearchRequestManager;
import com.tc.object.TestClientObjectManager;
import com.tc.object.bytecode.ManagerImpl;
import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.bytecode.hook.DSOContext;
import com.tc.object.bytecode.hook.impl.ClassProcessorHelper;
import com.tc.object.bytecode.hook.impl.DSOContextImpl;
import com.tc.object.bytecode.hook.impl.PreparedComponentsFromL2Connection;
import com.tc.object.config.StandardDSOClientConfigHelperImpl;
import com.tc.object.locks.MockClientLockManager;
import com.tc.object.tx.MockTransactionManager;
import com.tc.objectserver.impl.DistributedObjectServer;
import com.tc.objectserver.managedobject.ManagedObjectStateFactory;
import com.tc.server.TCServer;
import com.tc.server.TCServerImpl;
import com.tc.util.Assert;
import com.tc.util.PortChooser;
import com.tc.util.concurrent.ThreadUtil;
import com.tcclient.cluster.DsoNode;

import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class DsoNodeGetIpAndHostNameTest extends BaseDSOTestCase {

  static {
    ManagerUtil.enable();
  }

  /**
   * Test for DsoNodeImpl. To make sure getIp() and getHostanme() working fine when connected to L2 or disconnected.
   */
  public void testDsoNodeGetIpAndHostName() throws Exception {
    final PortChooser pc = new PortChooser();
    final int dsoPort = pc.chooseRandomPort();
    final int jmxPort = pc.chooseRandomPort();
    DsoClusterImpl dsoCluster1;
    DsoClusterImpl dsoCluster2;
    final DistributedObjectClient client1;
    final DistributedObjectClient client2;
    final TCServerImpl server = (TCServerImpl) startupServer(dsoPort, jmxPort);
    configFactory().addServerToL1Config("127.0.0.1", dsoPort, jmxPort);

    try {
      final ManagerImpl mgr1 = startupClient(dsoPort, jmxPort);
      client1 = mgr1.getDso();
      dsoCluster1 = (DsoClusterImpl) mgr1.getDsoCluster();
      final ManagerImpl mgr2 = startupClient(dsoPort, jmxPort);
      client2 = mgr2.getDso();
      dsoCluster2 = (DsoClusterImpl) mgr2.getDsoCluster();

      try {
        ClientMessageChannelImpl clientChannel = (ClientMessageChannelImpl) client1.getChannel().channel();
        ServerMessageChannelImpl serverChannel = (ServerMessageChannelImpl) server.getDSOServer().getChannelManager()
            .getChannel(clientChannel.getChannelID());

        // setup server to send ping message
        serverChannel.addClassMapping(TCMessageType.PING_MESSAGE, PingMessage.class);
        PingMessageSink serverSink = new PingMessageSink();
        ((CommunicationsManagerImpl) server.getDSOServer().getCommunicationsManager()).getMessageRouter()
            .routeMessageType(TCMessageType.PING_MESSAGE, serverSink);

        // set up client to receive ping message
        clientChannel.addClassMapping(TCMessageType.PING_MESSAGE, PingMessage.class);
        PingMessageSink clientSink = new PingMessageSink();
        ((CommunicationsManagerImpl) client1.getCommunicationsManager()).getMessageRouter()
            .routeMessageType(TCMessageType.PING_MESSAGE, clientSink);

        // server ping client
        TCMessage msg = serverChannel.createMessage(TCMessageType.PING_MESSAGE);
        msg.send();
        while (clientSink.getReceivedCount() != 1) {
          ThreadUtil.reallySleep(500);
          System.out.println(".");
        }
        PingMessage pingReceived = clientSink.getReceivedPing();
        Assert.assertTrue(msg.getSourceNodeID().equals(server.getDSOServer().getServerNodeID()));
        Assert.assertTrue(msg.getDestinationNodeID().equals(pingReceived.getDestinationNodeID()));

        // client ping server
        msg = clientChannel.createMessage(TCMessageType.PING_MESSAGE);
        msg.send();
        while (serverSink.getReceivedCount() != 1) {
          ThreadUtil.reallySleep(500);
          System.out.println(".");
        }
        pingReceived = serverSink.getReceivedPing();
        Assert.assertTrue(msg.getSourceNodeID().equals(pingReceived.getSourceNodeID()));
        Assert.assertTrue(server.getDSOServer().getServerNodeID().equals(pingReceived.getDestinationNodeID()));

        // check for client1's cluster topology view
        System.out.println("XXX DsoNode get IP/hostname when connected - Client 1");
        Collection<DsoNode> nodes = dsoCluster1.getClusterTopology().getNodes();
        for (DsoNode node : nodes) {
          System.out.println("XXX node: " + node);
          System.out.println("XXX IP:" + node.getIp() + " hostname: " + node.getHostname());
        }

        // check for client2's cluster topology view
        System.out.println("XXX DsoNode get IP/hostname when connected - Client 2");
        nodes = dsoCluster2.getClusterTopology().getNodes();
        for (DsoNode node : nodes) {
          System.out.println("XXX node: " + node);
          System.out.println("XXX IP:" + node.getIp() + " hostname: " + node.getHostname());
        }

      } finally {
        client1.stopForTests();
      }
    } finally {
      server.stop();
    }
    ThreadUtil.reallySleep(1000);
    // check for client2. To see if blocked when disconnected from L2.
    System.out.println("XXX DsoNode get IP/hostname when disconnected");
    Collection<DsoNode> nodes = dsoCluster2.getClusterTopology().getNodes();
    for (DsoNode node : nodes) {
      System.out.println("XXX node: " + node);
      System.out.println("XXX IP:" + node.getIp() + " hostname: " + node.getHostname());
    }
    client2.stopForTests();
  }

  private class PingMessageSink implements TCMessageSink {
    Queue<PingMessage> queue = new LinkedBlockingQueue<PingMessage>();

    public void putMessage(final TCMessage message) throws UnsupportedMessageTypeException {

      PingMessage ping = (PingMessage) message;

      try {
        message.hydrate();
      } catch (Exception e) {
        //
      }
      queue.add(ping);
    }

    public int getReceivedCount() {
      return queue.size();
    }

    public PingMessage getReceivedPing() {
      return queue.peek();
    }

  }

  protected TCServer startupServer(final int dsoPort, final int jmxPort) {
    StartAction start_action = new StartAction(dsoPort, jmxPort);
    new StartupHelper(group, start_action).startUp();
    final TCServer server = start_action.getServer();
    return server;
  }

  protected ManagerImpl startupClient(final int dsoPort, final int jmxPort) throws ConfigurationSetupException {
    L1ConfigurationSetupManager manager = super.createL1ConfigManager();
    StandardDSOClientConfigHelperImpl configHelper = new StandardDSOClientConfigHelperImpl(manager);
    PreparedComponentsFromL2Connection l2Connection = new PreparedComponentsFromL2Connection(manager);
    ManagerImpl tcmanager = new ManagerImpl(true, new TestClientObjectManager(), new MockTransactionManager(),
                                            new MockClientLockManager(), new MockRemoteSearchRequestManager(),
                                            configHelper, l2Connection);
    DSOContext context = DSOContextImpl.createContext(configHelper, tcmanager);
    ClassProcessorHelper.setContext(Thread.currentThread().getContextClassLoader(), context);
    tcmanager.initForTests(null);
    return tcmanager;
  }

  protected final TCThreadGroup group = new TCThreadGroup(
                                                          new ThrowableHandler(TCLogging
                                                              .getLogger(DistributedObjectServer.class)));

  protected class StartAction implements StartupHelper.StartupAction {
    private final int dsoPort;
    private final int jmxPort;
    private TCServer  server = null;

    private StartAction(final int dsoPort, final int jmxPort) {
      this.dsoPort = dsoPort;
      this.jmxPort = jmxPort;
    }

    public int getDsoPort() {
      return dsoPort;
    }

    public int getJmxPort() {
      return jmxPort;
    }

    public TCServer getServer() {
      return server;
    }

    public void execute() throws Throwable {
      ManagedObjectStateFactory.disableSingleton(true);
      TestConfigurationSetupManagerFactory factory = configFactory();
      L2ConfigurationSetupManager manager = factory.createL2TVSConfigurationSetupManager(null);
      manager.dsoL2Config().dsoPort().setIntValue(dsoPort);
      manager.dsoL2Config().dsoPort().setBind("127.0.0.1");

      manager.commonl2Config().jmxPort().setIntValue(jmxPort);
      manager.commonl2Config().jmxPort().setBind("127.0.0.1");

      server = new TCServerImpl(manager);
      server.start();
    }
  }
}
