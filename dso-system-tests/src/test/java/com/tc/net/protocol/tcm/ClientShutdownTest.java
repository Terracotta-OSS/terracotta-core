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
import com.tc.object.ClientShutdownManager;
import com.tc.object.DistributedObjectClient;
import com.tc.object.bytecode.MockClassProvider;
import com.tc.object.bytecode.NullManager;
import com.tc.object.bytecode.hook.impl.PreparedComponentsFromL2Connection;
import com.tc.object.config.StandardDSOClientConfigHelperImpl;
import com.tc.object.handshakemanager.ClientHandshakeManager;
import com.tc.object.logging.NullRuntimeLogger;
import com.tc.object.tx.RemoteTransactionManagerImpl;
import com.tc.objectserver.impl.DistributedObjectServer;
import com.tc.objectserver.managedobject.ManagedObjectStateFactory;
import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.server.TCServer;
import com.tc.server.TCServerImpl;
import com.tc.statistics.StatisticsAgentSubSystemImpl;
import com.tc.util.Assert;
import com.tc.util.PortChooser;
import com.tc.util.concurrent.ThreadUtil;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Test to make sure, during a Client shutdown Channel close is done only after RemoteTransactionManager shutdown. For
 * more info, please refer DEV-3894, DEV-3960
 */

public class ClientShutdownTest extends BaseDSOTestCase {

  private PreparedComponentsFromL2Connection preparedComponentsFromL2Connection;
  private boolean                            originalReconnect;
  private final TCProperties                 tcProps = TCPropertiesImpl.getProperties();

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    originalReconnect = tcProps.getBoolean(TCPropertiesConsts.L2_L1RECONNECT_ENABLED);
    tcProps.setProperty(TCPropertiesConsts.L2_L1RECONNECT_ENABLED, "true");
    System.setProperty("com.tc." + TCPropertiesConsts.L2_L1RECONNECT_ENABLED, "true");
  }

  public void testClientShutdownNotFromHook() throws Exception {
    final PortChooser pc = new PortChooser();
    final int dsoPort = pc.chooseRandomPort();
    final int jmxPort = pc.chooseRandomPort();

    final TCServerImpl server = (TCServerImpl) startupServer(dsoPort, jmxPort);
    server.getDSOServer().getCommunicationsManager().addClassMapping(TCMessageType.PING_MESSAGE, PingMessage.class);

    configFactory().addServerToL1Config("127.0.0.1", dsoPort, jmxPort);
    L1ConfigurationSetupManager manager = super.createL1ConfigManager();
    preparedComponentsFromL2Connection = new PreparedComponentsFromL2Connection(manager);

    final DistributedObjectClient client1 = startupClient(dsoPort, jmxPort, manager, preparedComponentsFromL2Connection);
    runTaskAndShutDownClient(server, client1, true);

    final DistributedObjectClient client2 = startupClient(dsoPort, jmxPort, manager, preparedComponentsFromL2Connection);
    runTaskAndShutDownClient(server, client2, false);

    server.stop();
  }

  public void runTaskAndShutDownClient(TCServerImpl server, DistributedObjectClient client, boolean shutDownHook)
      throws Exception {
    final ChEvntLsnr chLsnr = new ChEvntLsnr((RemoteTransactionManagerImpl) client.getRemoteTransactionManager());
    // wait until client handshake is complete...
    waitUntilUnpaused(client);

    ClientMessageChannelImpl clientChannel = (ClientMessageChannelImpl) client.getChannel().channel();
    clientChannel.addListener(chLsnr);

    ServerMessageChannelImpl serverChannel = (ServerMessageChannelImpl) server.getDSOServer().getChannelManager()
        .getChannel(clientChannel.getChannelID());

    // setup server to send ping message
    PingMessageSink serverSink = new PingMessageSink();
    try {
      ((AbstractMessageChannel) serverChannel).addClassMapping(TCMessageType.PING_MESSAGE, PingMessage.class);
    } catch (IllegalStateException ise) {
      // might have been already added. these hacks are just for tests.
    }
    ((CommunicationsManagerImpl) server.getDSOServer().getCommunicationsManager()).getMessageRouter()
        .routeMessageType(TCMessageType.PING_MESSAGE, serverSink);

    // set up client to receive ping message
    clientChannel.addClassMapping(TCMessageType.PING_MESSAGE, PingMessage.class);
    PingMessageSink clientSink = new PingMessageSink();
    ((CommunicationsManagerImpl) client.getCommunicationsManager()).getMessageRouter()
        .routeMessageType(TCMessageType.PING_MESSAGE, clientSink);

    // server ping client
    TCMessage msg = serverChannel.createMessage(TCMessageType.PING_MESSAGE);
    msg.send();
    System.out.println("XXX waiting for client to receive ping message");
    while (clientSink.getReceivedCount() != 1) {
      ThreadUtil.reallySleep(500);
      System.out.println("~");
    }
    PingMessage pingReceived = clientSink.getReceivedPing();
    Assert.assertTrue(msg.getSourceNodeID().equals(server.getDSOServer().getServerNodeID()));
    Assert.assertTrue(msg.getDestinationNodeID().equals(pingReceived.getDestinationNodeID()));

    // client ping server
    msg = clientChannel.createMessage(TCMessageType.PING_MESSAGE);
    msg.send();
    System.out.println("XXX waiting for server to receive ping message");
    while (serverSink.getReceivedCount() != 1) {
      ThreadUtil.reallySleep(500);
      System.out.println("~");
    }
    pingReceived = serverSink.getReceivedPing();
    Assert.assertTrue(msg.getSourceNodeID().equals(pingReceived.getSourceNodeID()));
    Assert.assertTrue(server.getDSOServer().getServerNodeID().equals(pingReceived.getDestinationNodeID()));

    ThreadUtil.reallySleep(5000);

    new ClientShutdownManager(client.getObjectManager(), client, preparedComponentsFromL2Connection)
        .execute(shutDownHook, false);

    System.out.println("XXX waiting for client close event");
    chLsnr.waitForComplete();
  }

  private void waitUntilUnpaused(final DistributedObjectClient client) {
    ClientHandshakeManager mgr = client.getClientHandshakeManager();
    mgr.waitForHandshake();
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

  protected DistributedObjectClient startupClient(final int dsoPort, final int jmxPort,
                                                  L1ConfigurationSetupManager manager,
                                                  PreparedComponentsFromL2Connection preparedComponentsFromL2Connection2)
      throws ConfigurationSetupException {

    DistributedObjectClient client = new DistributedObjectClient(new StandardDSOClientConfigHelperImpl(manager),
                                                                 new TCThreadGroup(new ThrowableHandler(TCLogging
                                                                     .getLogger(DistributedObjectClient.class))),
                                                                 new MockClassProvider(),
                                                                 preparedComponentsFromL2Connection,
                                                                 NullManager.getInstance(),
                                                                 new StatisticsAgentSubSystemImpl(),
                                                                 new DsoClusterImpl(), new NullRuntimeLogger());
    client.start();
    return client;
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

  class ChEvntLsnr implements ChannelEventListener {
    private final RemoteTransactionManagerImpl txManager;
    private volatile boolean                   done = false;

    public ChEvntLsnr(RemoteTransactionManagerImpl remoteTransactionManager) {
      this.txManager = remoteTransactionManager;
    }

    public void notifyChannelEvent(ChannelEvent event) {
      if (ChannelEventType.CHANNEL_CLOSED_EVENT.matches(event)) {
        System.out.println("XXX CH CLOSED");
        Assert.assertTrue(this.txManager.isShutdown());
        done = true;
      } else if (ChannelEventType.TRANSPORT_DISCONNECTED_EVENT.matches(event)) {
        System.out.println("XXX TX DISCONN");
      } else if (ChannelEventType.TRANSPORT_CONNECTED_EVENT.matches(event)) {
        System.out.println("XXX TX CONN");
      }
    }

    public void waitForComplete() {
      while (!this.done) {
        ThreadUtil.reallySleep(5000);
        System.out.println("~");
      }
    }
  }

  @Override
  protected synchronized void tearDown() throws Exception {
    super.tearDown();
    tcProps.setProperty(TCPropertiesConsts.L2_L1RECONNECT_ENABLED, "" + originalReconnect);
    System.setProperty("com.tc." + TCPropertiesConsts.L2_L1RECONNECT_ENABLED, "" + originalReconnect);
  }

}
