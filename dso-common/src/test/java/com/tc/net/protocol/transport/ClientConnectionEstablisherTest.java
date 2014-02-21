/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.transport;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.terracotta.test.categories.CheckShorts;

import com.tc.logging.ConnectionIdLogger;
import com.tc.net.CommStackMismatchException;
import com.tc.net.MaxConnectionsExceededException;
import com.tc.net.ReconnectionRejectedException;
import com.tc.net.TCSocketAddress;
import com.tc.net.core.ConnectionAddressIterator;
import com.tc.net.core.ConnectionAddressProvider;
import com.tc.net.core.ConnectionInfo;
import com.tc.net.core.TCConnection;
import com.tc.net.core.TCConnectionManager;
import com.tc.net.protocol.TCProtocolAdaptor;
import com.tc.net.protocol.delivery.OOOConnectionWatcher;
import com.tc.net.protocol.delivery.OnceAndOnlyOnceProtocolNetworkLayer;
import com.tc.net.protocol.transport.ClientConnectionEstablisher.AsyncReconnect;
import com.tc.util.TCAssertionError;
import com.tc.util.TCTimeoutException;

import java.io.IOException;

@Category(CheckShorts.class)
public class ClientConnectionEstablisherTest {
  private ClientConnectionEstablisher         connEstablisher;
  @Mock
  private TCConnectionManager                 connManager;
  @Mock
  private OnceAndOnlyOnceProtocolNetworkLayer layer;
  @Mock
  private ConnectionAddressProvider           connAddressProvider;
  @Mock
  private ReconnectionRejectedHandler         reconnectionRejectedHandler;
  @Mock
  private ClientMessageTransport              cmt;
  @Mock
  private AsyncReconnect                      mockedAsyncReconnect;
  private ClientConnectionEstablisher         spyConnEstablisher;
  @Mock
  private ConnectionAddressIterator           cai;
  @Mock
  private ConnectionInfo                      connInfo;
  @Mock
  private TCConnection                        tcConnection;
  @Mock
  private TCSocketAddress                     sa;

  @Mock
  private ConnectionIdLogger                  logger;
  @Mock
  private RestoreConnectionCallback           callback;
  @Mock
  private ConnectionAddressIterator           connAddresssItr;

  @Mock
  private TCConnection                        connection;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    connEstablisher = new ClientConnectionEstablisher(connManager, connAddressProvider, 1000, 10,
                                                      reconnectionRejectedHandler);
    spyConnEstablisher = Mockito.spy(connEstablisher);
  }

  @Test
  public void test_that_reset_creates_new_async_reconnect_thread() {
    AsyncReconnect asyncReconnectBeforeReset = connEstablisher.getAsyncReconnectThread();
    connEstablisher.reset();
    AsyncReconnect asyncReconnectAfterReset = connEstablisher.getAsyncReconnectThread();
    Assert.assertNotEquals(asyncReconnectBeforeReset, asyncReconnectAfterReset);
  }

  @Test
  public void test_reset_calls_quitReconnectAttempts() {
    spyConnEstablisher.reset();
    Mockito.verify(spyConnEstablisher, Mockito.times(1)).quitReconnectAttempts();
  }

  @Test
  public void test_quitReconnectAttempts_disallows_reconnects() {

    boolean allowReconnectsBefore = connEstablisher.getAllowReconnects();
    connEstablisher.setAllowReconnects(true);
    connEstablisher.quitReconnectAttempts();
    Assert.assertFalse(this.connEstablisher.getAllowReconnects());
    connEstablisher.setAllowReconnects(allowReconnectsBefore);
  }

  @Test
  public void test_open_fails_when_asyncReconnecting_is_true() throws TCTimeoutException, IOException,
      MaxConnectionsExceededException, CommStackMismatchException {
    connEstablisher.setAsyncReconnectingForTests(true);
    Mockito.doNothing().when(spyConnEstablisher).connectTryAllOnce(cmt);
    try {
      spyConnEstablisher.open(cmt);
      Assert.fail();
    } catch (TCAssertionError e) {
      // ignore
    }
  }

  @Test
  public void test_open_sets_allowReconnects_to_true() throws TCTimeoutException, IOException,
      MaxConnectionsExceededException, CommStackMismatchException {
    Mockito.doNothing().when(spyConnEstablisher).connectTryAllOnce(cmt);

    spyConnEstablisher.setAllowReconnects(false);
    spyConnEstablisher.open(cmt);
    Assert.assertTrue(spyConnEstablisher.getAllowReconnects());
  }

  @Test
  public void test_open_calls_connectTryAllOnce() throws TCTimeoutException, IOException,
      MaxConnectionsExceededException, CommStackMismatchException {
    Mockito.doNothing().when(spyConnEstablisher).connectTryAllOnce(cmt);
    spyConnEstablisher.open(cmt);
    Mockito.verify(spyConnEstablisher, Mockito.times(1)).connectTryAllOnce(cmt);
  }

  @Test
  public void test_connectTryAllOnce_tries_to_openConnection() throws TCTimeoutException, IOException,
      MaxConnectionsExceededException, CommStackMismatchException {
    // setup
    Mockito.doReturn(Boolean.TRUE).when(cai).hasNext();
    Mockito.doReturn(connInfo).when(cai).next();
    Mockito.doReturn(tcConnection).when(connManager).createConnection((TCProtocolAdaptor) Mockito.any());
    Mockito.doReturn(cai).when(connAddressProvider).getIterator();
    // test
    spyConnEstablisher.connectTryAllOnce(cmt);

    Mockito.verify(cmt).openConnection(tcConnection);
    Mockito.verify(spyConnEstablisher).connect((TCSocketAddress) Mockito.any(), (ClientMessageTransport) Mockito.any());
  }

  @Test
  public void test_connect_tries_to_make_new_connection_and_connect() throws TCTimeoutException, IOException {
    Mockito.doNothing().when(cmt).fireTransportConnectAttemptEvent();
    Mockito.doReturn(tcConnection).when(connManager).createConnection((TCProtocolAdaptor) Mockito.any());
    spyConnEstablisher.connect(sa, cmt);
    Mockito.verify(connManager).createConnection((TCProtocolAdaptor) Mockito.any());
    Mockito.verify(tcConnection).connect((TCSocketAddress) Mockito.any(), Mockito.anyInt());
  }

  @Test
  public void test_connect_fires_TransportConnectionAttempt_event() throws TCTimeoutException, IOException {
    Mockito.doNothing().when(cmt).fireTransportConnectAttemptEvent();
    Mockito.doReturn(tcConnection).when(connManager).createConnection((TCProtocolAdaptor) Mockito.any());
    spyConnEstablisher.connect(sa, cmt);
    Mockito.verify(cmt).fireTransportConnectAttemptEvent();
  }

  @Test
  public void test_reconnect_ignored_when_transport_already_connected() throws MaxConnectionsExceededException {
    // Mockito.doReturn(Boolean.TRUE).when(cmt).isConnected();
    Mockito.when(cmt.isConnected()).thenReturn(true);
    ClientConnectionEstablisher r = Mockito.doNothing().when(spyConnEstablisher);
    spyConnEstablisher.reconnect(cmt);
    Mockito.verify(spyConnEstablisher, Mockito.never()).isReconnectBetweenL2s();
  }

  @Test
  public void test_reconnect_calls_connect() throws MaxConnectionsExceededException, TCTimeoutException, IOException {
    Mockito.doReturn(cai).when(connAddressProvider).getIterator();
    Mockito.doReturn(logger).when(cmt).getLogger();
    Mockito.stub(cai.hasNext()).toReturn(true).toReturn(false);
    Mockito.doReturn(connInfo).when(cai).next();
    Mockito.doReturn(tcConnection).when(connManager).createConnection((TCProtocolAdaptor) Mockito.any());
    spyConnEstablisher.reconnect(cmt);
    Mockito.verify(spyConnEstablisher).connect((TCSocketAddress) Mockito.any(), (ClientMessageTransport) Mockito.any());
  }

  @Test
  public void test_reconnect_tries_to_reconnect_client_message_transport() throws Exception {

    Mockito.doReturn(cai).when(connAddressProvider).getIterator();
    Mockito.doReturn(logger).when(cmt).getLogger();
    Mockito.stub(cai.hasNext()).toReturn(true).toReturn(false);
    Mockito.doReturn(connInfo).when(cai).next();
    Mockito.doReturn(tcConnection).when(connManager).createConnection((TCProtocolAdaptor) Mockito.any());
    spyConnEstablisher.reconnect(cmt);
    Mockito.verify(cmt).reconnect((TCConnection) Mockito.any());
  }

  @Test
  public void test_restore_calls_connect() throws MaxConnectionsExceededException, TCTimeoutException, IOException {
    Mockito.doReturn(cai).when(connAddressProvider).getIterator();
    Mockito.doReturn(logger).when(cmt).getLogger();
    Mockito.stub(cai.hasNext()).toReturn(true).toReturn(false);
    Mockito.doReturn(connInfo).when(cai).next();
    Mockito.doReturn(tcConnection).when(connManager).createConnection((TCProtocolAdaptor) Mockito.any());
    spyConnEstablisher.restoreConnection(cmt, sa, 10 * 1000, callback);
    Mockito.verify(spyConnEstablisher).connect((TCSocketAddress) Mockito.any(), (ClientMessageTransport) Mockito.any());
  }

  @Test
  public void test_restore_tries_to_reconnect_client_message_transport() throws Exception {
    Mockito.doReturn(cai).when(connAddressProvider).getIterator();
    Mockito.doReturn(logger).when(cmt).getLogger();
    Mockito.stub(cai.hasNext()).toReturn(true).toReturn(false);
    Mockito.doReturn(connInfo).when(cai).next();
    Mockito.doReturn(tcConnection).when(connManager).createConnection((TCProtocolAdaptor) Mockito.any());
    spyConnEstablisher.restoreConnection(cmt, sa, 10 * 1000, callback);
    Mockito.verify(cmt).reconnect((TCConnection) Mockito.any());
  }

  @Test
  public void test_when_restoreConnection_gets_reconnectionRejected_then_reconnect_request_not_added()
      throws Exception {
    Mockito.doThrow(new ReconnectionRejectedException("Reconnection Rejected")).when(cmt).reconnect(connection);
    Mockito.doReturn(logger).when(cmt).getLogger();
    Mockito.doReturn(true).when(cmt).wasOpened();
    Mockito.doReturn(connection).when(connManager).createConnection((TCProtocolAdaptor) Mockito.any());
    OOOConnectionWatcher watcher = new OOOConnectionWatcher(cmt, connEstablisher, layer, 0);
    DummyAsyncReconnect asyncReconnectThread = new DummyAsyncReconnect(connEstablisher);
    connEstablisher.setAsyncReconnectThreadForTests(asyncReconnectThread);

    connEstablisher.restoreConnection(cmt, sa, 0, watcher);
    Assert.assertEquals(0, connEstablisher.connectionRequestQueueSize());
  }

  public static class DummyAsyncReconnect extends AsyncReconnect {

    public DummyAsyncReconnect(ClientConnectionEstablisher cce) {
      super(cce);
    }

    @Override
    public void startThreadIfNecessary() {
      // no op
    }

  }

}
