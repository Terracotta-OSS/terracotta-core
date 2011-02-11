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
import com.tc.net.core.MockTCConnection;
import com.tc.net.core.TCConnection;
import com.tc.net.core.event.TCConnectionEvent;
import com.tc.net.protocol.delivery.OnceAndOnlyOnceProtocolNetworkLayerImpl;
import com.tc.net.protocol.tcm.msgs.PingMessage;
import com.tc.net.protocol.transport.ClientMessageTransport;
import com.tc.object.BaseDSOTestCase;
import com.tc.object.DistributedObjectClient;
import com.tc.object.bytecode.MockClassProvider;
import com.tc.object.bytecode.NullManager;
import com.tc.object.bytecode.hook.impl.PreparedComponentsFromL2Connection;
import com.tc.object.config.StandardDSOClientConfigHelperImpl;
import com.tc.object.handshakemanager.ClientHandshakeManager;
import com.tc.object.logging.NullRuntimeLogger;
import com.tc.objectserver.impl.DistributedObjectServer;
import com.tc.objectserver.managedobject.ManagedObjectStateFactory;
import com.tc.server.TCServer;
import com.tc.server.TCServerImpl;
import com.tc.statistics.StatisticsAgentSubSystemImpl;
import com.tc.util.Assert;
import com.tc.util.PortChooser;
import com.tc.util.concurrent.ThreadUtil;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class TwoDisconnectEventsTest extends BaseDSOTestCase {

  public void testTwoDisconnectEvents() throws Exception {
    final PortChooser pc = new PortChooser();
    final int dsoPort = pc.chooseRandomPort();
    final int jmxPort = pc.chooseRandomPort();
    final TCServerImpl server = (TCServerImpl) startupServer(dsoPort, jmxPort);

    try {
      final DistributedObjectClient client = startupClient(dsoPort, jmxPort);
      try {
        // wait until client handshake is complete...
        waitUntilUnpaused(client);

        ClientMessageChannelImpl clientChannel = (ClientMessageChannelImpl) client.getChannel().channel();
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
        ((CommunicationsManagerImpl) client.getCommunicationsManager()).getMessageRouter()
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

        // two transport disconnect events to client.
        ClientMessageChannelImpl cmci = clientChannel;
        ClientMessageTransport cmt;
        if (cmci.getSendLayer() instanceof ClientMessageTransport) {
          cmt = (ClientMessageTransport) cmci.getSendLayer();
        } else {
          cmt = (ClientMessageTransport) ((OnceAndOnlyOnceProtocolNetworkLayerImpl) cmci.getSendLayer()).getSendLayer();
        }
        cmt.setAllowConnectionReplace(true);

        // send first event
        TCConnection tccomm = new MockTCConnection();
        cmt.attachNewConnection(tccomm);
        cmt.closeEvent(new TCConnectionEvent(tccomm));

        // send second event
        tccomm = new MockTCConnection();
        cmt.attachNewConnection(tccomm);
        cmt.closeEvent(new TCConnectionEvent(tccomm));
        ThreadUtil.reallySleep(2000);
        msg = clientChannel.createMessage(TCMessageType.PING_MESSAGE);
        Assert.assertEquals(msg.getLocalSessionID(), cmci.getSessionID());

      } finally {
        client.getCommunicationsManager().shutdown();
        client.stopForTests();
      }
    } finally {
      server.stop();
    }
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

  protected DistributedObjectClient startupClient(final int dsoPort, final int jmxPort)
      throws ConfigurationSetupException {
    configFactory().addServerToL1Config("127.0.0.1", dsoPort, jmxPort);
    L1ConfigurationSetupManager manager = super.createL1ConfigManager();

    DistributedObjectClient client = new DistributedObjectClient(new StandardDSOClientConfigHelperImpl(manager),
                                                                 new TCThreadGroup(new ThrowableHandler(TCLogging
                                                                     .getLogger(DistributedObjectClient.class))),
                                                                 new MockClassProvider(),
                                                                 new PreparedComponentsFromL2Connection(manager),
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
}
