/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.logging.TCLogging;
import com.tc.net.core.MockTCConnection;
import com.tc.net.core.TestTCConnection;
import com.tc.net.protocol.StackNotFoundException;
import com.tc.net.protocol.transport.MockTransportHandshakeMessageFactory.CallContext;
import com.tc.test.TCTestCase;

import java.util.HashSet;
import java.util.Set;

public class ServerStackProviderTest extends TCTestCase {

  private ServerStackProvider                  provider;
  private MockStackHarnessFactory              harnessFactory;
  private MockNetworkStackHarness              harness;
  private MockTransportHandshakeMessageFactory transportHandshakeMessageFactory;
  private ConnectionID                         connId;
  private ConnectionIdFactory                  connectionIdFactory;
  private TestConnectionPolicy                 connectionPolicy;
  private TestWireProtocolAdaptorFactory       wpaFactory;
  private MockMessageTransportFactory          transportFactory;
  private DefaultConnectionIdFactory           connectionIDProvider;

  public ServerStackProviderTest() {
    super();
  }

  protected void setUp() throws Exception {
    super.setUp();

    this.harness = new MockNetworkStackHarness();

    this.harnessFactory = new MockStackHarnessFactory();
    this.harnessFactory.harness = this.harness;
    this.connectionIdFactory = new ConnectionIdFactory() {
      public ConnectionID nextConnectionId() {
        return connId;
      }
    };

    transportFactory = new MockMessageTransportFactory();
    transportHandshakeMessageFactory = new MockTransportHandshakeMessageFactory();
    connectionPolicy = new TestConnectionPolicy();
    wpaFactory = new TestWireProtocolAdaptorFactory();
    this.provider = new ServerStackProvider(TCLogging.getLogger(ServerStackProvider.class), new HashSet(),
                                            this.harnessFactory, null, transportFactory,
                                            transportHandshakeMessageFactory, this.connectionIdFactory,
                                            connectionPolicy, wpaFactory);
    connectionIDProvider = new DefaultConnectionIdFactory();
    this.connId = connectionIDProvider.nextConnectionId();
  }

  protected void tearDown() throws Exception {
    super.tearDown();
  }

  /**
   * Test to make sure that the connection accounting is done properly.
   */
  public void testConnectionPolicyInteraction() throws Exception {

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
    connectionPolicy.maxConnectionsExceeded = true;
    // Send SYN message
    sink.putMessage(syn);
    // the client should have sent the SYN_ACK message
    assertSame(synAck, transport.sendToConnectionCalls.take());

    // The connected client count should have been incremented
    assertEquals(1, connectionPolicy.clientConnected);
    // make sure that the transport message factory was called with the proper arguments
    MockTransportHandshakeMessageFactory.CallContext args = (CallContext) transportHandshakeMessageFactory.createSynAckCalls
        .poll(0);
    assertEquals(new Boolean(connectionPolicy.maxConnectionsExceeded), args.getIsMaxConnectionsExceeded());
    assertEquals(new Integer(connectionPolicy.maxConnections), args.getMaxConnections());

    assertEquals(0, connectionPolicy.clientDisconnected);
    // XXX: This is yucky. THis is the connection id that the stack provider assigns to the transport (via the
    // connection id factory)
    transport.connectionId = this.connId;
    provider.notifyTransportClosed(transport);
    assertEquals(1, connectionPolicy.clientConnected);

  }

  public void testRebuildStack() throws Exception {
    ConnectionID connectionID1 = connectionIDProvider.nextConnectionId();
    ConnectionID connectionID2 = connectionIDProvider.nextConnectionId();
    Set rebuild = new HashSet();
    rebuild.add(connectionID1);

    provider = new ServerStackProvider(TCLogging.getLogger(ServerStackProvider.class), rebuild, this.harnessFactory,
                                       null, transportFactory, null, this.connectionIdFactory, connectionPolicy,
                                       new WireProtocolAdaptorFactoryImpl());

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
    provider.attachNewConnection(ConnectionID.NULL_ID, conn);

    // send a transport disconnected event
    MockMessageTransport transport = new MockMessageTransport();
    transport.connectionId = this.connId;
    assertEquals(0, connectionPolicy.clientDisconnected);
    provider.notifyTransportDisconnected(transport);

    // make sure that the connection policy is decremented
    assertEquals(1, connectionPolicy.clientDisconnected);

  }

  public void testNotifyTransportClose() throws Exception {
    TestTCConnection conn = new TestTCConnection();
    provider.attachNewConnection(ConnectionID.NULL_ID, conn);

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
    provider.attachNewConnection(ConnectionID.NULL_ID, conn);

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
      provider.attachNewConnection(ConnectionID.NULL_ID, conn);
    } catch (StackNotFoundException e) {
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
    }

    assertTrue(harness.wasAttachNewConnectionCalled);
    assertFalse(harness.wasFinalizeStackCalled);

    // cause lookup failure
    ConnectionID differentConnId = connectionIDProvider.nextConnectionId();
    harness.wasAttachNewConnectionCalled = false;
    harness.wasFinalizeStackCalled = false;

    try {
      provider.attachNewConnection(differentConnId, conn);
      fail("was not virgin and had connId, but should not exist in provider");
    } catch (StackNotFoundException e) {
      // expected
    }
  }

}
