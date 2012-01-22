/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.transport;

import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedRef;

import com.tc.exception.TCRuntimeException;
import com.tc.net.CommStackMismatchException;
import com.tc.net.TCSocketAddress;
import com.tc.net.core.ConnectionAddressProvider;
import com.tc.net.core.ConnectionInfo;
import com.tc.net.core.MockConnectionManager;
import com.tc.net.core.MockTCConnection;
import com.tc.net.core.TCConnection;
import com.tc.net.core.event.TCConnectionEvent;
import com.tc.net.protocol.PlainNetworkStackHarnessFactory;
import com.tc.net.protocol.delivery.OOONetworkStackHarnessFactory;
import com.tc.net.protocol.delivery.OnceAndOnlyOnceProtocolNetworkLayerFactoryImpl;
import com.tc.net.protocol.tcm.ClientMessageChannel;
import com.tc.net.protocol.tcm.CommunicationsManager;
import com.tc.net.protocol.tcm.CommunicationsManagerImpl;
import com.tc.net.protocol.tcm.NetworkListener;
import com.tc.net.protocol.tcm.NullMessageMonitor;
import com.tc.object.session.NullSessionManager;
import com.tc.properties.L1ReconnectConfigImpl;
import com.tc.test.TCTestCase;
import com.tc.util.TCTimeoutException;

import java.util.Collections;
import java.util.List;

/**
 * x normal connect and handshake o reconnect and handshake
 */
public class ClientMessageTransportTest extends TCTestCase {
  private ConnectionID                       connectionId;
  private ClientMessageTransport             transport;
  private MockConnectionManager              connectionManager;
  private MockTCConnection                   connection;
  private TransportHandshakeMessageFactory   transportMessageFactory;
  private TestTransportHandshakeErrorHandler handshakeErrorHandler;
  private final int                          maxRetries = 10;
  private MessageTransportFactory            transportFactory;
  private final int                          timeout    = 3000;

  @Override
  public void setUp() {
    DefaultConnectionIdFactory connectionIDProvider = new DefaultConnectionIdFactory();
    this.connectionId = connectionIDProvider.nextConnectionId(JvmIDUtil.getJvmID());
    this.connectionManager = new MockConnectionManager();
    this.connection = new MockTCConnection();
    this.connectionManager.setConnection(connection);
    this.transportMessageFactory = new TransportMessageFactoryImpl();
    handshakeErrorHandler = new TestTransportHandshakeErrorHandler();

    final ConnectionInfo connectionInfo = new ConnectionInfo("", 0);
    ClientConnectionEstablisher cce = new ClientConnectionEstablisher(
                                                                      connectionManager,
                                                                      new ConnectionAddressProvider(
                                                                                                    new ConnectionInfo[] { connectionInfo }),
                                                                      maxRetries, 5000);
    transport = new ClientMessageTransport(cce, handshakeErrorHandler, this.transportMessageFactory,
                                           new WireProtocolAdaptorFactoryImpl(),
                                           TransportHandshakeMessage.NO_CALLBACK_PORT);
  }

  public void testRoundRobinReconnect() throws Exception {
    SynchronizedRef errorRef = new SynchronizedRef(null);
    ClientHandshakeMessageResponder tester = new ClientHandshakeMessageResponder(new LinkedQueue(), new LinkedQueue(),
                                                                                 this.transportMessageFactory,
                                                                                 this.connectionId, this.transport,
                                                                                 errorRef);
    this.connection.setMessageSink(tester);

    transport.open();
    while (!connection.connectCalls.isEmpty()) {
      connection.connectCalls.take();
    }

    connection.fail = true;
    transport.closeEvent(new TCConnectionEvent(connection));

    // FIXME 2005-12-14 -- We should restore this test.
    // assertNull(connection.connectCalls.poll(3000));

  }

  public void testConnectAndHandshake() throws Exception {
    SynchronizedRef errorRef = new SynchronizedRef(null);
    ClientHandshakeMessageResponder tester = new ClientHandshakeMessageResponder(new LinkedQueue(), new LinkedQueue(),
                                                                                 this.transportMessageFactory,
                                                                                 this.connectionId, this.transport,
                                                                                 errorRef);

    this.connection.setMessageSink(tester);

    transport.open();

    assertTrue(errorRef.get() == null);

    List sentMessages = connection.getSentMessages();

    assertEquals(2, sentMessages.size());
    assertEquals(this.connectionId, transport.getConnectionId());
    Thread.sleep(1000);
    assertTrue(tester.waitForAckToBeReceived(timeout));
  }

  /**
   * Test interaction with a real network listener.
   */
  public void testConnectAndHandshakeActuallyConnected() throws Exception {
    CommunicationsManager commsMgr = new CommunicationsManagerImpl("TestCommsMgr", new NullMessageMonitor(),
                                                                   new TransportNetworkStackHarnessFactory(),
                                                                   new NullConnectionPolicy(), 0);
    NetworkListener listener = commsMgr.createListener(new NullSessionManager(), new TCSocketAddress(0), true,
                                                       new DefaultConnectionIdFactory());
    listener.start(Collections.EMPTY_SET);
    int port = listener.getBindPort();

    final ConnectionInfo connInfo = new ConnectionInfo(TCSocketAddress.LOOPBACK_IP, port);
    ClientConnectionEstablisher cce = new ClientConnectionEstablisher(
                                                                      commsMgr.getConnectionManager(),
                                                                      new ConnectionAddressProvider(
                                                                                                    new ConnectionInfo[] { connInfo }),
                                                                      0, 1000);
    transport = new ClientMessageTransport(cce, this.handshakeErrorHandler, this.transportMessageFactory,
                                           new WireProtocolAdaptorFactoryImpl(),
                                           TransportHandshakeMessage.NO_CALLBACK_PORT);
    transport.open();
    assertTrue(transport.isConnected());
    listener.stop(5000);

  }

  /**
   * This test is for testing the communication stack layer mismatch between the server and L1s while handshaking
   */
  public void testStackLayerMismatch() throws Exception {
    // Case 1: Server has the OOO layer and client doesn't

    CommunicationsManager serverCommsMgr = new CommunicationsManagerImpl(
                                                                         "TestCommsMgr-Server",
                                                                         new NullMessageMonitor(),
                                                                         new OOONetworkStackHarnessFactory(
                                                                                                           new OnceAndOnlyOnceProtocolNetworkLayerFactoryImpl(),
                                                                                                           new L1ReconnectConfigImpl()),
                                                                         new NullConnectionPolicy(), 0);

    CommunicationsManager clientCommsMgr = new CommunicationsManagerImpl("TestCommsMgr-Client",
                                                                         new NullMessageMonitor(),
                                                                         new PlainNetworkStackHarnessFactory(),
                                                                         new NullConnectionPolicy(), 0);

    try {
      createStacksAndTest(serverCommsMgr, clientCommsMgr);
      throw new TCRuntimeException("Expect throwing CommStackMismatchException");
    } catch (CommStackMismatchException e) {
      // expected exception
    } finally {
      try {
        clientCommsMgr.shutdown();
      } finally {
        serverCommsMgr.shutdown();
      }
    }

    // Case 2: Client has the OOO layer and server doesn't
    serverCommsMgr = new CommunicationsManagerImpl("TestCommsMgr-Server", new NullMessageMonitor(),
                                                   new PlainNetworkStackHarnessFactory(), new NullConnectionPolicy(), 0);

    clientCommsMgr = new CommunicationsManagerImpl(
                                                   "TestCommsMgr-Client",
                                                   new NullMessageMonitor(),
                                                   new OOONetworkStackHarnessFactory(
                                                                                     new OnceAndOnlyOnceProtocolNetworkLayerFactoryImpl(),
                                                                                     new L1ReconnectConfigImpl()),
                                                   new NullConnectionPolicy(), 0);

    try {
      createStacksAndTest(serverCommsMgr, clientCommsMgr);
      throw new TCRuntimeException("Expect throwing CommStackMismatchException");
    } catch (CommStackMismatchException e) {
      // expected exception
    } finally {
      try {
        clientCommsMgr.shutdown();
      } finally {
        serverCommsMgr.shutdown();
      }
    }

  }

  private void createStacksAndTest(final CommunicationsManager serverCommsMgr,
                                   final CommunicationsManager clientCommsMgr) throws Exception {
    NetworkListener listener = serverCommsMgr.createListener(new NullSessionManager(),
                                                             new TCSocketAddress(TCSocketAddress.LOOPBACK_IP, 0), true,
                                                             new DefaultConnectionIdFactory());
    listener.start(Collections.EMPTY_SET);
    final int port = listener.getBindPort();

    // set up the transport factory
    transportFactory = new MessageTransportFactory() {
      public MessageTransport createNewTransport() {
        ClientConnectionEstablisher clientConnectionEstablisher = new ClientConnectionEstablisher(
                                                                                                  serverCommsMgr
                                                                                                      .getConnectionManager(),
                                                                                                  new ConnectionAddressProvider(
                                                                                                                                new ConnectionInfo[] { new ConnectionInfo(
                                                                                                                                                                          "localhost",
                                                                                                                                                                          port) }),
                                                                                                  maxRetries, timeout);
        ClientMessageTransport cmt = new ClientMessageTransport(clientConnectionEstablisher, handshakeErrorHandler,
                                                                transportMessageFactory,
                                                                new WireProtocolAdaptorFactoryImpl(),
                                                                TransportHandshakeMessage.NO_CALLBACK_PORT);
        return cmt;
      }

      public MessageTransport createNewTransport(ConnectionID connectionID, TransportHandshakeErrorHandler handler,
                                                 TransportHandshakeMessageFactory handshakeMessageFactory,
                                                 List transportListeners) {
        throw new AssertionError();
      }

      public MessageTransport createNewTransport(ConnectionID connectionID, TCConnection tcConnection,
                                                 TransportHandshakeErrorHandler handler,
                                                 TransportHandshakeMessageFactory handshakeMessageFactory,
                                                 List transportListeners) {
        throw new AssertionError();
      }
    };

    ClientMessageChannel channel;
    channel = clientCommsMgr
        .createClientChannel(new NullSessionManager(),
                             0,
                             TCSocketAddress.LOOPBACK_IP,
                             port,
                             timeout,
                             new ConnectionAddressProvider(
                                                           new ConnectionInfo[] { new ConnectionInfo("localhost", port) }),
                             transportFactory);
    try {
      channel.open();
    } catch (TCTimeoutException e) {
      // this is an expected timeout exception as the client will get an handshake error and it is not killing itself
      // do nothing
    }
    assertTrue(handshakeErrorHandler.getStackLayerMismatch());
    listener.stop(5000);
  }

  private static class TestTransportHandshakeErrorHandler implements TransportHandshakeErrorHandler {

    private boolean stackLayerMismatch = false;

    public void handleHandshakeError(TransportHandshakeErrorContext e) {
      if (e.getErrorType() == TransportHandshakeError.ERROR_STACK_MISMATCH) stackLayerMismatch = true;
    }

    public boolean getStackLayerMismatch() {
      return stackLayerMismatch;
    }
  }
}
