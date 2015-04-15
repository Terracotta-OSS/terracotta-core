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
package com.tc.net.protocol.transport;

import com.tc.net.core.MockTCConnection;
import com.tc.net.core.TCConnection;
import com.tc.net.core.TestTCConnection;
import com.tc.net.protocol.IllegalReconnectException;
import com.tc.net.protocol.NetworkLayer;
import com.tc.net.protocol.NetworkStackHarness;
import com.tc.net.protocol.NetworkStackHarnessFactory;
import com.tc.net.protocol.RejectReconnectionException;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.MessageChannelInternal;
import com.tc.net.protocol.tcm.MockMessageChannel;
import com.tc.net.protocol.tcm.MockMessageChannelFactory;
import com.tc.net.protocol.tcm.ServerMessageChannelFactory;
import com.tc.net.protocol.transport.MockTransportMessageFactory.CallContext;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

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
    this.provider = new ServerStackProvider(new HashSet(),
                                            this.harnessFactory, null, transportFactory,
                                            transportHandshakeMessageFactory, this.connectionIdFactory,
                                            connectionPolicy, wpaFactory, new ReentrantLock());
    connectionIDProvider = new DefaultConnectionIdFactory();
    this.connId = nextConnectionID();
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  public void testAttachAlreadyConnectedTransport() throws Exception {
    NetworkStackHarness harness = mock(NetworkStackHarness.class);
    NetworkStackHarnessFactory harnessFactory = when(mock(NetworkStackHarnessFactory.class).createServerHarness(
        any(ServerMessageChannelFactory.class), any(MessageTransport.class), any(MessageTransportListener[].class))).thenReturn(harness).getMock();
    ServerMessageChannelFactory serverMessageChannelFactory = mock(ServerMessageChannelFactory.class);
    MessageTransportFactory messageTransportFactory = mock(MessageTransportFactory.class);
    DefaultConnectionIdFactory connectionIdFactory = new DefaultConnectionIdFactory();
    ServerStackProvider serverStackProvider = new ServerStackProvider(Collections.emptySet(), harnessFactory, serverMessageChannelFactory, messageTransportFactory,
        spy(new TransportMessageFactoryImpl()), connectionIdFactory, new NullConnectionPolicy(), mock(WireProtocolAdaptorFactory.class), new ReentrantLock());

    // This is the next connection ID the connectionIDFactory is going to assign out, need to save it first before it
    // gets changed by the connectionIDFactory assigning it out.
    ConnectionID nextConnectionID = new ConnectionID("foo", connectionIdFactory.getCurrentConnectionID(), connectionIdFactory.getServerID());

    // Attach once for the normal case.
    serverStackProvider.attachNewConnection(new ConnectionID("foo", -1), new TestTCConnection());

    // Now make the attach fail due to IllegalReconnectException, we should force a reconnection rejected exception now
    when(harness.attachNewConnection(any(TCConnection.class))).thenThrow(new IllegalReconnectException());
    try {
      serverStackProvider.attachNewConnection(nextConnectionID, new TestTCConnection());
      fail("Should have gotten a reconnection rejected when attaching a new TCConnection results in an IllegalReconnectException");
    } catch (RejectReconnectionException e) {
      // expected
    }
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
    this.provider = new ServerStackProvider(new HashSet(),
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

    // But Client1 still can connect (even though max connections are reached)
    connectNewClient(this.provider, "jvm1", true);

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
    ConnectionID connectionID1 = nextConnectionID();
    ConnectionID connectionID2 = nextConnectionID();
    Set rebuild = new HashSet();
    rebuild.add(connectionID1);

    provider = new ServerStackProvider(rebuild, this.harnessFactory,
                                       null, transportFactory, null, this.connectionIdFactory, connectionPolicy,
                                       new WireProtocolAdaptorFactoryImpl(), new ReentrantLock());

    MockTCConnection conn = new MockTCConnection();
    provider.attachNewConnection(connectionID1, conn);

    // trying to attach a stack that wasn't rebuilt at startup should fail.
    try {
      provider.attachNewConnection(connectionID2, new MockTCConnection());
      fail("Expected StackNotFoundException");
    } catch (RejectReconnectionException e) {
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
    } catch (RejectReconnectionException e) {
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
    } catch (RejectReconnectionException e) {
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
    } catch (RejectReconnectionException e) {
      fail("was virgin, should not throw exception");
    }

    assertFalse(harness.wasAttachNewConnectionCalled);
    assertTrue(harness.wasFinalizeStackCalled);

    // test look up of existing connection ID
    harness.wasAttachNewConnectionCalled = false;
    harness.wasFinalizeStackCalled = false;

    try {
      provider.attachNewConnection(this.connId, conn);
    } catch (RejectReconnectionException e) {
      fail("was virgin, should not throw exception");
    }

    assertTrue(harness.wasAttachNewConnectionCalled);
    assertFalse(harness.wasFinalizeStackCalled);

    // cause lookup failure
    ConnectionID differentConnId = nextConnectionID();
    harness.wasAttachNewConnectionCalled = false;
    harness.wasFinalizeStackCalled = false;

    try {
      provider.attachNewConnection(differentConnId, conn);
      fail("was not virgin and had connId, but should not exist in provider");
    } catch (RejectReconnectionException e) {
      // expected
    }
  }

  private ConnectionID nextConnectionID() {
    return connectionIDProvider.populateConnectionID(new ConnectionID(JvmIDUtil.getJvmID(), -1L));
  }

  private class TestConnectionIDFactory extends DefaultConnectionIdFactory {
    @Override
    public ConnectionID populateConnectionID(final ConnectionID connectionID) {
      connId = super.populateConnectionID(connectionID);
      return connId;
    }
  }

}
