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
package com.tc.net.protocol.transport;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.tc.logging.ConnectionIdLogger;
import com.tc.net.CommStackMismatchException;
import com.tc.net.MaxConnectionsExceededException;
import com.tc.net.ReconnectionRejectedException;
import com.tc.net.TCSocketAddress;
import com.tc.net.core.ConnectionInfo;
import com.tc.net.core.TCConnection;
import com.tc.net.core.TCConnectionManager;
import com.tc.net.protocol.NetworkStackID;
import com.tc.net.protocol.TCNetworkMessage;
import com.tc.net.protocol.TCProtocolAdaptor;
import com.tc.net.protocol.delivery.OOOConnectionWatcher;
import com.tc.net.protocol.delivery.OnceAndOnlyOnceProtocolNetworkLayer;
import com.tc.net.protocol.transport.ClientConnectionEstablisher.AsyncReconnect;
import com.tc.util.TCAssertionError;
import com.tc.util.TCTimeoutException;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

public class ClientConnectionEstablisherTest {
  private ClientConnectionEstablisher         connEstablisher;
  @Mock
  private TCConnectionManager                 connManager;
  @Mock
  private OnceAndOnlyOnceProtocolNetworkLayer layer;
  @Mock
  private ReconnectionRejectedHandler         reconnectionRejectedHandler;

  private ClientMessageTransport              cmt;

  private ClientConnectionEstablisher         spyConnEstablisher;
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
  private TCConnection                        connection;

  @Before
  public void setup() throws Exception {
    MockitoAnnotations.initMocks(this);
    try {
      cmt = spy(new ClientMessageTransport(connManager, mock(TransportHandshakeErrorHandler.class), mock(TransportHandshakeMessageFactory.class), mock(WireProtocolAdaptorFactory.class), 0, 0));
      doNothing().when(cmt).sendToConnection(any(TCNetworkMessage.class));
      doNothing().when(cmt).reconnect(any(TCSocketAddress.class));
      doReturn(new NetworkStackID(0)).when(cmt).open(any(ConnectionInfo.class));
      doNothing().when(cmt).openConnection(any(TCConnection.class));
      ConnectionID cid = new ConnectionID(JvmIDUtil.getJvmID(), 0);
      cmt.initConnectionID(cid);
      connEstablisher = new ClientConnectionEstablisher(reconnectionRejectedHandler);
    } catch (Exception e) {
      e.printStackTrace();
      throw e;
    }
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
    Mockito.doReturn(null).when(spyConnEstablisher).connectTryAllOnce(cmt);
    try {
      spyConnEstablisher.open(Collections.singleton(connInfo), cmt);
      Assert.fail();
    } catch (TCAssertionError e) {
      // ignore
    }
  }

  @Test
  public void test_open_sets_allowReconnects_to_true() throws TCTimeoutException, IOException,
      MaxConnectionsExceededException, CommStackMismatchException {
    Mockito.doReturn(null).when(spyConnEstablisher).connectTryAllOnce(cmt);
    Mockito.doReturn(tcConnection).when(connManager).createConnection((TCProtocolAdaptor) Matchers.any());

    spyConnEstablisher.setAllowReconnects(false);
    spyConnEstablisher.open(Collections.singleton(connInfo), cmt);
    Assert.assertTrue(spyConnEstablisher.getAllowReconnects());
  }

  @Test
  public void test_open_calls_connectTryAllOnce() throws TCTimeoutException, IOException,
      MaxConnectionsExceededException, CommStackMismatchException {
    Mockito.doReturn(null).when(spyConnEstablisher).connectTryAllOnce(cmt);
    spyConnEstablisher.open(Collections.singleton(connInfo), cmt);
    Mockito.verify(spyConnEstablisher, Mockito.times(1)).connectTryAllOnce(cmt);
  }

  @Test
  public void test_connectTryAllOnce_tries_to_openConnection() throws Exception, TCTimeoutException, IOException,
      MaxConnectionsExceededException, CommStackMismatchException {
    Mockito.doReturn(tcConnection).when(connManager).createConnection((TCProtocolAdaptor) Matchers.any());
    // test
    try {
      spyConnEstablisher.open(Collections.singleton(connInfo), cmt);
    } catch (Exception e) {
      e.printStackTrace();
      throw e;
    }

    Mockito.verify(cmt).open(Matchers.any(ConnectionInfo.class));
  }

  @Test
  public void test_connect_tries_to_make_new_connection_and_connect() throws Exception {
    Mockito.doNothing().when(cmt).fireTransportConnectAttemptEvent();
    Mockito.doReturn(tcConnection).when(connManager).createConnection((TCProtocolAdaptor) Matchers.any());
    spyConnEstablisher.open(Arrays.asList(connInfo), cmt);
    Mockito.verify(cmt).open(Matchers.any(ConnectionInfo.class));
  }

  @Test
  public void test_reconnect_ignored_when_transport_already_connected() throws MaxConnectionsExceededException {
    // Mockito.doReturn(Boolean.TRUE).when(cmt).isConnected();
    Mockito.when(cmt.isConnected()).thenReturn(true);
    Mockito.doNothing().when(spyConnEstablisher);
    spyConnEstablisher.reconnect(cmt);
    Mockito.verify(spyConnEstablisher, Mockito.never()).isReconnectBetweenL2s();
  }

  @Test
  public void test_reconnect_calls_connect() throws Exception {
    Mockito.doReturn(logger).when(cmt).getLogger();
    Mockito.doReturn(tcConnection).when(connManager).createConnection((TCProtocolAdaptor) Matchers.any());
    Mockito.doReturn(null).when(spyConnEstablisher).connectTryAllOnce(Matchers.any(ClientMessageTransport.class));
    spyConnEstablisher.open(Arrays.asList(connInfo), cmt);
    spyConnEstablisher.reconnect(cmt);
    Mockito.verify(cmt).reopen(Matchers.any(ConnectionInfo.class));
  }

  @Test
  public void test_restore_calls_connect() throws Exception {
    Mockito.doReturn(tcConnection).when(connManager).createConnection((TCProtocolAdaptor) Matchers.any());
    Mockito.doReturn(null).when(spyConnEstablisher).connectTryAllOnce(Matchers.any(ClientMessageTransport.class));
    spyConnEstablisher.open(Arrays.asList(connInfo), cmt);
    spyConnEstablisher.restoreConnection(cmt, sa, 10 * 1000, callback);
    Mockito.verify(cmt).reconnect(Matchers.any(TCSocketAddress.class));
  }

  @Test
  public void test_when_restoreConnection_gets_reconnectionRejected_then_reconnect_request_not_added()
      throws Exception {
    Mockito.doThrow(new ReconnectionRejectedException("Reconnection Rejected")).when(cmt).reopen(Matchers.any(ConnectionInfo.class));
    Mockito.doReturn(logger).when(cmt).getLogger();
    Mockito.doReturn(true).when(cmt).wasOpened();
    Mockito.doReturn(connection).when(connManager).createConnection((TCProtocolAdaptor) Matchers.any());
    OOOConnectionWatcher watcher = new OOOConnectionWatcher(cmt, connEstablisher, layer, 0);
    connEstablisher.disableReconnectThreadSpawn();

    connEstablisher.restoreConnection(cmt, sa, 0, watcher);
    Assert.assertEquals(0, connEstablisher.connectionRequestQueueSize());
  }

  @Test
  public void test_client_keeps_trying_for_reconnect_after_unknownHostException() throws Exception {
    spyConnEstablisher.open(Collections.singleton(connInfo), cmt);
    Mockito.doThrow(new UnknownHostException("Host can not be resolved!")).when(spyConnEstablisher)
        .getHostByName(connInfo);
    Mockito.doReturn(tcConnection).when(connManager).createConnection((TCProtocolAdaptor) Matchers.any());
    try {
      spyConnEstablisher.reconnect(cmt);
    } catch (RuntimeException re) {
      String msg = "failed due to:" + re.getMessage();
      if (re.getCause() instanceof UnknownHostException) {
        msg = "Got UnknownHostException,it should be ignored and we should keep trying reconnect";
      }
      Assert.fail(msg);
    }
  }
}
