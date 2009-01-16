/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.tcm;

import com.tc.cluster.Cluster;
import com.tc.config.schema.SettableConfigItem;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.L1TVSConfigurationSetupManager;
import com.tc.config.schema.setup.L2TVSConfigurationSetupManager;
import com.tc.config.schema.setup.TestTVSConfigurationSetupManagerFactory;
import com.tc.lang.StartupHelper;
import com.tc.lang.TCThreadGroup;
import com.tc.lang.ThrowableHandler;
import com.tc.logging.TCLogging;
import com.tc.net.core.MockTCConnection;
import com.tc.net.core.TCConnection;
import com.tc.net.core.event.TCConnectionEvent;
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
import com.tc.util.Assert;
import com.tc.util.PortChooser;
import com.tc.util.concurrent.ThreadUtil;

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

        ClientMessageChannel clientChannel = client.getChannel().channel();
        ServerMessageChannelImpl serverChannel = (ServerMessageChannelImpl) server.getDSOServer().getChannelManager()
            .getChannel(clientChannel.getChannelID());

        // setup server to send ping message
        server.getDSOServer().addClassMapping(TCMessageType.PING_MESSAGE, PingMessage.class);
        PingMessageSink serverSink = new PingMessageSink();
        serverChannel.routeMessageType(TCMessageType.PING_MESSAGE, serverSink);

        // set up client to receive ping message
        clientChannel.addClassMapping(TCMessageType.PING_MESSAGE, PingMessage.class);
        PingMessageSink clientSink = new PingMessageSink();
        clientChannel.routeMessageType(TCMessageType.PING_MESSAGE, clientSink);

        // server ping client
        TCMessage msg = serverChannel.createMessage(TCMessageType.PING_MESSAGE);
        msg.send();
        ThreadUtil.reallySleep(100);
        Assert.assertEquals(1, clientSink.getReceivedCount());
        PingMessage pingReceived = clientSink.getReceivedPing();
        Assert.assertTrue(msg.getSourceNodeID().equals(server.getDSOServer().getServerNodeID()));
        Assert.assertTrue(msg.getDestinationNodeID().equals(pingReceived.getDestinationNodeID()));

        // client ping server
        msg = clientChannel.createMessage(TCMessageType.PING_MESSAGE);
        msg.send();
        ThreadUtil.reallySleep(100);
        Assert.assertEquals(1, serverSink.getReceivedCount());
        pingReceived = serverSink.getReceivedPing();
        Assert.assertTrue(msg.getSourceNodeID().equals(pingReceived.getSourceNodeID()));
        Assert.assertTrue(server.getDSOServer().getServerNodeID().equals(pingReceived.getDestinationNodeID()));

        // two transport disconnect events to client.
        ClientMessageChannelImpl cmci = (ClientMessageChannelImpl) clientChannel;
        ClientMessageTransport cmt = (ClientMessageTransport) cmci.getSendLayer();
        cmt.setAllowConnectionReplace(true);

        // send first event
        TCConnection tccomm = new MockTCConnection();
        cmt.attachNewConnection(tccomm);
        cmt.closeEvent(new TCConnectionEvent(tccomm));

        // send second event
        tccomm = new MockTCConnection();
        cmt.attachNewConnection(tccomm);
        cmt.closeEvent(new TCConnectionEvent(tccomm));
        ThreadUtil.reallySleep(200);
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

  private void waitUntilUnpaused(DistributedObjectClient client) {
    ClientHandshakeManager mgr = client.getClientHandshakeManager();
    mgr.waitForHandshake();
  }

  private class PingMessageSink implements TCMessageSink {
    private int         receivedCount = 0;
    private PingMessage pingReceived;

    public void putMessage(TCMessage message) throws UnsupportedMessageTypeException {

      PingMessage ping = (PingMessage) message;

      try {
        message.hydrate();
      } catch (Exception e) {
        //
      }
      ++receivedCount;
      pingReceived = ping;
    }

    public int getReceivedCount() {
      return receivedCount;
    }

    public PingMessage getReceivedPing() {
      return pingReceived;
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
    configFactory().addServerToL1Config(null, dsoPort, jmxPort);
    L1TVSConfigurationSetupManager manager = super.createL1ConfigManager();

    DistributedObjectClient client = new DistributedObjectClient(new StandardDSOClientConfigHelperImpl(manager),
                                                                 new TCThreadGroup(new ThrowableHandler(TCLogging
                                                                     .getLogger(DistributedObjectClient.class))),
                                                                 new MockClassProvider(),
                                                                 new PreparedComponentsFromL2Connection(manager),
                                                                 NullManager.getInstance(), new Cluster(),
                                                                 new NullRuntimeLogger());
    client.start();
    return client;
  }

  protected final TCThreadGroup group = new TCThreadGroup(new ThrowableHandler(TCLogging
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
      TestTVSConfigurationSetupManagerFactory factory = configFactory();
      L2TVSConfigurationSetupManager manager = factory.createL2TVSConfigurationSetupManager(null);
      ((SettableConfigItem) factory.l2DSOConfig().bind()).setValue("127.0.0.1");
      ((SettableConfigItem) factory.l2DSOConfig().listenPort()).setValue(dsoPort);
      ((SettableConfigItem) factory.l2CommonConfig().jmxPort()).setValue(jmxPort);

      server = new TCServerImpl(manager);
      server.start();
    }
  }
}
