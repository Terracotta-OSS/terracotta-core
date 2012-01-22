/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.logging.TCLogging;
import com.tc.net.core.MockTCConnection;
import com.tc.net.core.TCConnection;
import com.tc.net.core.TestTCConnection;
import com.tc.net.protocol.IllegalReconnectException;
import com.tc.net.protocol.NetworkLayer;
import com.tc.net.protocol.StackNotFoundException;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.MessageChannelInternal;
import com.tc.net.protocol.tcm.MockMessageChannel;
import com.tc.net.protocol.tcm.MockMessageChannelFactory;
import com.tc.net.protocol.transport.MockTransportMessageFactory.CallContext;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

public class ServerStackProviderTest extends TCTestCase {

  private ServerStackProvider            provider;
  private MockStackHarnessFactory        harnessFactory;
  private MockNetworkStackHarness        harness;
  private MockTransportMessageFactory    transportHandshakeMessageFactory;
  private ConnectionID                   connId;
  private ConnectionIDFactory            connectionIdFactory;
  private TestConnectionPolicy           connectionPolicy;
  private TestWireProtocolAdaptorFactory wpaFactory;
  private MockMessageTransportFactory    transportFactory;
  private DefaultConnectionIdFactory     connectionIDProvider;

  public ServerStackProviderTest() {
    super();
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    this.harness = new MockNetworkStackHarness();

    this.harnessFactory = new MockStackHarnessFactory();
    this.harnessFactory.harness = this.harness;
    this.connectionIdFactory = new TestConnectionIDFactory();

    transportFactory = new MockMessageTransportFactory();
    transportHandshakeMessageFactory = new MockTransportMessageFactory();
    connectionPolicy = new TestConnectionPolicy();
    wpaFactory = new TestWireProtocolAdaptorFactory();
    this.provider = new ServerStackProvider(TCLogging.getLogger(ServerStackProvider.class), new HashSet(),
                                            this.harnessFactory, null, transportFactory,
                                            transportHandshakeMessageFactory, this.connectionIdFactory,
                                            connectionPolicy, wpaFactory, new ReentrantLock());
    connectionIDProvider = new DefaultConnectionIdFactory();
    this.connId = connectionIDProvider.nextConnectionId(JvmIDUtil.getJvmID());
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  /**
   * Test to make sure that the connection accounting is done properly.
   */
  public void testConnectionPolicyInteraction() throws Exception {

    // 1. client connect

    assertNull(wpaFactory.newWireProtocolAdaptorCalls.poll(0));
    // XXX: This is yucky. This has the effect of creating a new TCProtocolAdapter which creates a wire protocol
    // message sink which is the thing we need to drop messages on.
    provider.getInstance();

    WireProtocolMessageSink sink = (WireProtocolMessageSink) wpaFactory.newWireProtocolAdaptorCalls.take();
    TestSynMessage syn = new TestSynMessage();
    TestSynAckMessage synAck = new TestSynAckMessage();
    TestTCConnection connection = new TestTCConnection();

    this.transportHandshakeMessageFactory.synAck = synAck;
    syn.connection = connection;

    MockMessageTransport transport = new MockMessageTransport();

    transportFactory.transport = transport;
    // make sure that createSynACk calls in the transport handshake message factory are clear
    assertNull(transportHandshakeMessageFactory.createSynAckCalls.poll(0));
    // make sure the send calls in the transport are clear
    assertNull(transport.sendToConnectionCalls.poll(0));
    connectionPolicy.maxConnections = 13;
    connectionPolicy.maxConnectionsExceeded = false;
    // Send SYN message
    sink.putMessage(syn);
    // the client should have sent the SYN_ACK message
    assertSame(synAck, transport.sendToConnectionCalls.take());

    // The connected client count should have been incremented
    assertEquals(1, connectionPolicy.clientConnected);
    // make sure that the transport message factory was called with the proper arguments
    MockTransportMessageFactory.CallContext args = (CallContext) transportHandshakeMessageFactory.createSynAckCalls
        .poll(0);
    assertEquals(new Boolean(connectionPolicy.maxConnectionsExceeded), args.getIsMaxConnectionsExceeded());
    assertEquals(new Integer(connectionPolicy.maxConnections), args.getMaxConnections());

    provider.notifyTransportDisconnected(transport, false);
    assertEquals(0, connectionPolicy.clientConnected);

    provider.notifyTransportClosed(transport);
    assertEquals(0, connectionPolicy.clientConnected);

    // reset
    connectionPolicy.clientConnected = 0;
    provider.getInstance();
    sink = (WireProtocolMessageSink) wpaFactory.newWireProtocolAdaptorCalls.take();

    // 2. Client connect when the max connections reached.

    connectionPolicy.maxConnectionsExceeded = true;
    sink.putMessage(syn);
    assertSame(synAck, transport.sendToConnectionCalls.take());

    assertEquals(0, connectionPolicy.clientConnected);
    args = (CallContext) transportHandshakeMessageFactory.createSynAckCalls.poll(0);
    assertEquals(new Boolean(connectionPolicy.maxConnectionsExceeded), args.getIsMaxConnectionsExceeded());
    assertEquals(new Integer(connectionPolicy.maxConnections), args.getMaxConnections());

    // At the server side, for Tx un-ESTABLISHED clients we don't fire any disconnect/closed events.
    assertEquals(0, connectionPolicy.clientConnected);

  }

  private MessageChannelInternal getNewDummyClientChannel(ChannelID clientID) {
    return new MockMessageChannel(clientID) {
      @Override
      public void notifyTransportDisconnected(MessageTransport transport, boolean forcedDisconnect) {
        //
      }
    };
  }

  private TCConnection getNewDummyTCConnection() {
    return new TestTCConnection() {
      @Override
      public void asynchClose() {
        //
      }
    };
  }

  private MockMessageTransport connectNewClient(ServerStackProvider serverProvider, String jvmID,
                                                boolean checkNonNullConnID) throws WireProtocolException {
    serverProvider.getInstance();
    MockMessageTransport serverTxForClient = new MockMessageTransport();
    transportFactory.transport = serverTxForClient;
    WireProtocolMessageSink sink = (WireProtocolMessageSink) wpaFactory.newWireProtocolAdaptorCalls.take();
    TestSynMessage syn = new TestSynMessage();
    syn.connection = new TestTCConnection();
    syn.connectionID = new ConnectionID(jvmID, ChannelID.NULL_ID.toLong());
    sink.putMessage(syn);
    SynAckMessage synAckMessage = (SynAckMessage) serverTxForClient.sendToConnectionCalls.take();
    System.out.println("XXX Client connect :" + synAckMessage.getConnectionId());
    if (checkNonNullConnID) {
      Assert.eval(!synAckMessage.getConnectionId().equals(ConnectionID.NULL_ID));
    } else {
      Assert.eval(synAckMessage.getConnectionId().equals(ConnectionID.NULL_ID));
    }
    return serverTxForClient;
  }

  public void testConnectionPolicyExtended() throws Exception {

    connectionPolicy.maxConnectionsExceeded = false;
    connectionPolicy.maxConnections = 2;
    connectionPolicy.clientConnected = 0;
    connectionPolicy.clientsByJvm.clear();

    WireProtocolMessageSink sink;
    TestSynMessage syn;
    SynAckMessage synAckMessage;

    MockMessageChannelFactory messageChannelFactory = new MockMessageChannelFactory();
    this.provider = new ServerStackProvider(TCLogging.getLogger(ServerStackProvider.class), new HashSet(),
                                            new TransportNetworkStackHarnessFactory(), messageChannelFactory,
                                            transportFactory, new TransportMessageFactoryImpl(),
                                            this.connectionIdFactory, connectionPolicy, wpaFactory, new ReentrantLock());

    // Client1
    MockMessageTransport serverTxForClietn1 = connectNewClient(this.provider, "jvm1", true);
    Assert.assertEquals(1, connectionPolicy.clientConnected);

    // Client2
    provider.getInstance();
    syn = new TestSynMessage();
    syn.connection = getNewDummyTCConnection();
    syn.connectionID = new ConnectionID(JvmIDUtil.getJvmID(), ChannelID.NULL_ID.toLong());
    syn.flag = NetworkLayer.TYPE_TRANSPORT_LAYER;
    MockServerMessageTransport serverTxForClietn2 = new MockServerMessageTransport(ConnectionID.NULL_ID,
                                                                                   syn.connection, null,
                                                                                   new TransportMessageFactoryImpl());
    serverTxForClietn2.setAllowConnectionReplace(true);
    transportFactory.transport = serverTxForClietn2;
    sink = (WireProtocolMessageSink) wpaFactory.newWireProtocolAdaptorCalls.take();
    messageChannelFactory.channel = getNewDummyClientChannel(new ChannelID(2));
    sink.putMessage(syn);
    synAckMessage = (SynAckMessage) serverTxForClietn2.sendToConnectionCalls.take();
    System.out.println("XXX Client 2 :" + synAckMessage.getConnectionId());
    ConnectionID client2ConnID = synAckMessage.getConnectionId();
    Assert.eval(!synAckMessage.getConnectionId().equals(ConnectionID.NULL_ID));
    Assert.assertEquals(2, connectionPolicy.clientConnected);
    serverTxForClietn2.status.established();
    serverTxForClietn2.addTransportListener(provider);

    // Client3 cannot connect
    connectNewClient(this.provider, "jvm3", false);
    Assert.assertEquals(2, connectionPolicy.clientConnected);

    // Client4 cannot connect
    connectNewClient(this.provider, "jvm4", false);
    Assert.assertEquals(2, connectionPolicy.clientConnected);

    // client1 disconencted
    provider.notifyTransportDisconnected(serverTxForClietn1, false);
    Assert.assertEquals(1, connectionPolicy.clientConnected);

    // Client5 connected
    MockMessageTransport serverTxForClietn5 = connectNewClient(this.provider, "jvm5", true);
    Assert.assertEquals(2, connectionPolicy.clientConnected);

    // client 2 reconnecting without disconnecting
    provider.getInstance();
    MockMessageTransport notUsedTx = new MockMessageTransport();
    transportFactory.transport = notUsedTx;
    sink = (WireProtocolMessageSink) wpaFactory.newWireProtocolAdaptorCalls.take();
    syn = new TestSynMessage();
    syn.connection = new TestTCConnection();
    syn.connectionID = client2ConnID;
    syn.flag = NetworkLayer.TYPE_TRANSPORT_LAYER;
    sink.putMessage(syn);
    synAckMessage = (SynAckMessage) serverTxForClietn2.sendToConnectionCalls.take();
    System.out.println("XXX Client 6 :" + synAckMessage.getConnectionId());
    Assert.eval(!synAckMessage.isMaxConnectionsExceeded());
    Assert.assertNull(notUsedTx.sendToConnectionCalls.peek());
    Assert.eval(synAckMessage.getConnectionId() != ConnectionID.NULL_ID);
    Assert.assertEquals(2, connectionPolicy.clientConnected);

    // client5 disconnected
    provider.notifyTransportDisconnected(serverTxForClietn5, false);
    Assert.assertEquals(1, connectionPolicy.clientConnected);

    // Client6 connected
    connectNewClient(this.provider, "jvm6", true);
    Assert.assertEquals(2, connectionPolicy.clientConnected);

  }

  public void testRebuildStack() throws Exception {
    ConnectionID connectionID1 = connectionIDProvider.nextConnectionId(JvmIDUtil.getJvmID());
    ConnectionID connectionID2 = connectionIDProvider.nextConnectionId(JvmIDUtil.getJvmID());
    Set rebuild = new HashSet();
    rebuild.add(connectionID1);

    provider = new ServerStackProvider(TCLogging.getLogger(ServerStackProvider.class), rebuild, this.harnessFactory,
                                       null, transportFactory, null, this.connectionIdFactory, connectionPolicy,
                                       new WireProtocolAdaptorFactoryImpl(), new ReentrantLock());

    MockTCConnection conn = new MockTCConnection();
    provider.attachNewConnection(connectionID1, conn);

    // trying to attach a stack that wasn't rebuilt at startup should fail.
    try {
      provider.attachNewConnection(connectionID2, new MockTCConnection());
      fail("Expected StackNotFoundException");
    } catch (StackNotFoundException e) {
      // expected.
    }
  }

  public void testNotifyTransportDisconnected() throws Exception {
    TestTCConnection conn = new TestTCConnection();

    MockMessageTransport transport = new MockMessageTransport();
    transportFactory.transport = transport;

    provider.attachNewConnection(new ConnectionID(JvmIDUtil.getJvmID(), ChannelID.NULL_ID.toLong()), conn);

    assertEquals(0, connectionPolicy.clientConnected);

    // send a transport disconnected event
    provider.notifyTransportDisconnected(transport, false);

    // transport disconnect event works only if the same client prev connected
    assertEquals(0, connectionPolicy.clientConnected);

    // send transport close event
    provider.notifyTransportClosed(transport);

    // transport close event works only if the same client prev connected
    assertEquals(0, connectionPolicy.clientConnected);

  }

  public void testNotifyTransportClose() throws Exception {
    TestTCConnection conn = new TestTCConnection();
    provider.attachNewConnection(new ConnectionID(JvmIDUtil.getJvmID(), ChannelID.NULL_ID.toLong()), conn);

    // try looking it up again. Make sure it found what it was looking for.
    provider.attachNewConnection(connId, conn);

    // send it a transport closed event.
    MockMessageTransport transport = new MockMessageTransport();
    transport.connectionId = this.connId;
    provider.notifyTransportClosed(transport);

    // make sure that a future lookup throws a StackNotFoundException
    try {
      provider.attachNewConnection(this.connId, conn);
      fail("Expected StackNotFoundException.");
    } catch (StackNotFoundException e) {
      // expected
    }
  }

  /**
   * Makes sure that removeNetworkStack(String) removes the the expected stack.
   */
  public void testRemoveNetworkStack() throws Exception {
    MockTCConnection conn = new MockTCConnection();
    provider.attachNewConnection(new ConnectionID(JvmIDUtil.getJvmID(), ChannelID.NULL_ID.toLong()), conn);

    assertEquals(harness, provider.removeNetworkStack(this.connId));
    assertTrue(provider.removeNetworkStack(this.connId) == null);

    try {
      // try looking it up again. Make sure it throws an exception
      provider.attachNewConnection(this.connId, conn);
      fail("Should have thrown an exception.");
    } catch (StackNotFoundException e) {
      // expected
    }

    // trying to remove it again should return null.
    assertTrue(provider.removeNetworkStack(this.connId) == null);
  }

  public void testAttachNewConnection() {
    assertFalse(harness.wasAttachNewConnectionCalled);
    assertFalse(harness.wasFinalizeStackCalled);

    MockTCConnection conn = new MockTCConnection();
    try {
      provider.attachNewConnection(new ConnectionID(JvmIDUtil.getJvmID(), ChannelID.NULL_ID.toLong()), conn);
    } catch (StackNotFoundException e) {
      fail("was virgin, should not throw exception");
    } catch (IllegalReconnectException e) {
      fail("was virgin, should not throw exception");
    }

    assertFalse(harness.wasAttachNewConnectionCalled);
    assertTrue(harness.wasFinalizeStackCalled);

    // test look up of existing connection ID
    harness.wasAttachNewConnectionCalled = false;
    harness.wasFinalizeStackCalled = false;

    try {
      provider.attachNewConnection(this.connId, conn);
    } catch (StackNotFoundException e) {
      fail("was virgin, should not throw exception");
    } catch (IllegalReconnectException e) {
      fail("was virgin, should not throw exception");
    }

    assertTrue(harness.wasAttachNewConnectionCalled);
    assertFalse(harness.wasFinalizeStackCalled);

    // cause lookup failure
    ConnectionID differentConnId = connectionIDProvider.nextConnectionId(JvmIDUtil.getJvmID());
    harness.wasAttachNewConnectionCalled = false;
    harness.wasFinalizeStackCalled = false;

    try {
      provider.attachNewConnection(differentConnId, conn);
      fail("was not virgin and had connId, but should not exist in provider");
    } catch (StackNotFoundException e) {
      // expected
    } catch (IllegalReconnectException e) {
      fail("unexpected exception: " + e);
    }
  }

  private class TestConnectionIDFactory extends DefaultConnectionIdFactory {

    @Override
    public synchronized ConnectionID nextConnectionId(String clientJvmID) {
      connId = super.nextConnectionId(clientJvmID);
      return connId;
    }

    @Override
    public ConnectionID makeConnectionId(String clientJvmID, long channelID) {
      return (super.makeConnectionId(clientJvmID, channelID));
    }
  }

}
