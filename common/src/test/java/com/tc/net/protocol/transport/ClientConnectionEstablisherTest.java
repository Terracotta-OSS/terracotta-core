/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.net.protocol.transport;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.tc.logging.ConnectionIdLogger;
import com.tc.net.CommStackMismatchException;
import com.tc.net.MaxConnectionsExceededException;
import com.tc.net.core.TCConnection;
import com.tc.net.core.TCConnectionManager;
import com.tc.net.protocol.NetworkStackID;
import com.tc.net.protocol.TCNetworkMessage;
import com.tc.util.TCTimeoutException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import com.tc.net.protocol.TCProtocolAdaptor;
import java.net.Socket;
import java.nio.channels.ClosedByInterruptException;
import java.util.concurrent.CountDownLatch;
import static org.mockito.ArgumentMatchers.anyBoolean;

public class ClientConnectionEstablisherTest {
  //private ClientConnectionEstablisher         connEstablisher;
  @Mock
  private TCConnectionManager                 connManager;

  private ClientMessageTransport              cmt;

  private ClientConnectionEstablisher         spyConnEstablisher;

  private final InetSocketAddress                   serverAddress = InetSocketAddress.createUnresolved("localhost", 9510);

  @Mock
  private TCConnection                        tcConnection;
  @Mock
  private ClientConnectionErrorListener       errorListener;
  @Mock
  private ConnectionIdLogger                  logger;

  @Before
  public void setup() throws Exception {
    MockitoAnnotations.initMocks(this);
    try {
      cmt = spy(new ClientMessageTransport(connManager, mock(TransportHandshakeErrorHandler.class), mock(TransportHandshakeMessageFactory.class), mock(WireProtocolAdaptorFactory.class), 0));
      doNothing().when(cmt).sendToConnection(any(TCNetworkMessage.class));
      doNothing().when(cmt).reconnect(any(InetSocketAddress.class));
      doReturn(new NetworkStackID(0)).when(cmt).open(any(InetSocketAddress.class));
      doNothing().when(cmt).openConnection(any(TCConnection.class));
      ConnectionID cid = new ConnectionID(JvmIDUtil.getJvmID(), 0);
      cmt.initConnectionID(cid);
      ClientConnectionEstablisher connEstablisher = new ClientConnectionEstablisher(cmt);
      spyConnEstablisher = Mockito.spy(connEstablisher);
    } catch (Exception e) {
      e.printStackTrace();
      throw e;
    }
  }

  @Test
  public void test_quitReconnectAttempts_disallows_reconnects() {
    spyConnEstablisher.shutdown();
    Assert.assertFalse(this.spyConnEstablisher.isReconnectEnabled());
  }

  @Test
  public void test_interrupt_kills_reconnect() throws Exception {
    Mockito.doReturn(mock(NetworkStackID.class)).when(cmt).open(any());
    Mockito.when(cmt.wasOpened()).thenReturn(Boolean.FALSE);
    spyConnEstablisher.open(Collections.singleton(serverAddress), errorListener);
    Mockito.when(cmt.wasOpened()).thenReturn(Boolean.TRUE);
    CountDownLatch callOnce = new CountDownLatch(1);
    Mockito.doAnswer(a->{
      if (Thread.interrupted()) {
        Thread.currentThread().interrupt();
        throw new ClosedByInterruptException();
      } else {
        callOnce.countDown();
        throw new IOException("host not found");
      }
    }).when(cmt).reopen(any(InetSocketAddress.class));
    Assert.assertTrue(spyConnEstablisher.asyncReconnect(()->false));
    Assert.assertTrue(spyConnEstablisher.isReconnecting());
    callOnce.await();
    spyConnEstablisher.interruptReconnect();
    spyConnEstablisher.waitForTermination();
    Assert.assertFalse(cmt.isConnected());
    Assert.assertFalse(spyConnEstablisher.isReconnecting());
  }
  
  @Test
  public void test_multiple_open_without_reset_fails() throws TCTimeoutException, IOException,
      MaxConnectionsExceededException, CommStackMismatchException {
    Mockito.doReturn(mock(NetworkStackID.class)).when(cmt).open(any());
    Mockito.when(cmt.wasOpened()).thenReturn(Boolean.FALSE);
    spyConnEstablisher.open(Collections.singleton(serverAddress), errorListener);
    try {
      spyConnEstablisher.open(Collections.singleton(serverAddress), errorListener);
      Assert.fail();
    } catch (IOException e) {
      // ignore
    }
  }

  @Test
  public void test_open_sets_allowReconnects_to_true() throws TCTimeoutException, IOException,
      MaxConnectionsExceededException, CommStackMismatchException {
    Mockito.doReturn(null).when(spyConnEstablisher).connectTryAllOnce(errorListener);
    Mockito.doReturn(tcConnection).when(connManager).createConnection((TCProtocolAdaptor) any());

    Assert.assertFalse(spyConnEstablisher.isReconnectEnabled());
    spyConnEstablisher.open(Collections.singleton(serverAddress), errorListener);
    Assert.assertTrue(spyConnEstablisher.isReconnectEnabled());
  }

  @Test
  public void test_open_calls_connectTryAllOnce() throws TCTimeoutException, IOException,
      MaxConnectionsExceededException, CommStackMismatchException {
    Mockito.doReturn(null).when(spyConnEstablisher).connectTryAllOnce(errorListener);
    spyConnEstablisher.open(Collections.singleton(serverAddress), errorListener);
    Mockito.verify(spyConnEstablisher, Mockito.times(1)).connectTryAllOnce(errorListener);
  }

  @Test
  public void test_connectTryAllOnce_tries_to_openConnection() throws Exception, TCTimeoutException, IOException,
      MaxConnectionsExceededException, CommStackMismatchException {
    Mockito.doReturn(tcConnection).when(connManager).createConnection((TCProtocolAdaptor) any());
    // test
    try {
      spyConnEstablisher.open(Collections.singleton(serverAddress), errorListener);
    } catch (Exception e) {
      e.printStackTrace();
      throw e;
    }

    Mockito.verify(cmt).open(any(InetSocketAddress.class));
  }

  @Test
  public void test_connect_tries_to_make_new_connection_and_connect() throws Exception {
    Mockito.doNothing().when(cmt).fireTransportConnectAttemptEvent();
    Mockito.doReturn(tcConnection).when(connManager).createConnection((TCProtocolAdaptor) any());
    spyConnEstablisher.open(Collections.singletonList(serverAddress), errorListener);
    Mockito.verify(cmt).open(any(InetSocketAddress.class));
  }

  @Test
  public void test_reconnect_ignored_when_transport_already_connected() throws MaxConnectionsExceededException, InterruptedException {
    // Mockito.doReturn(Boolean.TRUE).when(cmt).isConnected();
    Mockito.when(cmt.isConnected()).thenReturn(true);
    Mockito.doNothing().when(spyConnEstablisher);
    spyConnEstablisher.reconnect(()->false);
    Mockito.verify(cmt, Mockito.never()).isRetryOnReconnectionRejected();
  }

  @Test
  public void test_reconnect_calls_connect() throws Exception {
    Mockito.doReturn(logger).when(cmt).getLogger();
    Mockito.doReturn(tcConnection).when(connManager).createConnection((TCProtocolAdaptor) any());
    Mockito.doReturn(null).when(spyConnEstablisher).connectTryAllOnce(any(ClientConnectionErrorListener.class));
    spyConnEstablisher.open(Collections.singletonList(serverAddress), errorListener);
    Mockito.doReturn(true).when(cmt).wasOpened();
    spyConnEstablisher.reconnect(()-> {
      Mockito.doReturn(true).when(cmt).isConnected();
      return false;
    });
    Mockito.verify(cmt).reopen(any(InetSocketAddress.class));
  }

  @Test
  public void test_client_keeps_trying_for_reconnect_after_unknownHostException() throws Exception {
    spyConnEstablisher.open(Collections.singleton(serverAddress), errorListener);
    Mockito.doThrow(new UnknownHostException("Host can not be resolved!")).when(spyConnEstablisher)
        .getHostByName(serverAddress);
    Mockito.doReturn(tcConnection).when(connManager).createConnection((TCProtocolAdaptor) any());
    Mockito.doReturn(true).when(cmt).wasOpened();
    try {
      spyConnEstablisher.reconnect(()-> {
        Mockito.doReturn(true).when(cmt).isConnected();
        return false;
      });
    } catch (RuntimeException re) {
      String msg = "failed due to:" + re.getMessage();
      if (re.getCause() instanceof UnknownHostException) {
        msg = "Got UnknownHostException,it should be ignored and we should keep trying reconnect";
      }
      Assert.fail(msg);
    }
    Mockito.verify(spyConnEstablisher).handleConnectException(any(UnknownHostException.class), anyBoolean(), any());
  }

  @Test
  public void test_client_continues_on_handshake_timeout() throws Exception {
    /* simulate the reconnection thread causing a second reconnect attempt due to
       handshake timeout and connection close.  ConnectionEstablisher should just ignore
       the async connection request
    */
    spyConnEstablisher.open(Collections.singleton(serverAddress), errorListener);
    Mockito.doReturn(tcConnection).when(connManager).createConnection((TCProtocolAdaptor) any());
    Mockito.doAnswer((iom) -> {
      Assert.assertFalse(spyConnEstablisher.asyncReconnect(()->true));
      Mockito.when(cmt.isConnected()).thenReturn(Boolean.TRUE);
      return null;
    }).when(cmt).reopen(eq(serverAddress));
    Mockito.doReturn(true).when(cmt).wasOpened();
    Assert.assertTrue(spyConnEstablisher.asyncReconnect(()->false));
    spyConnEstablisher.waitForTermination();
    Mockito.verify(cmt).reopen(any());
  }

  @Test
  public void test_client_tries_next_after_noActive() throws Exception {
    List<InetSocketAddress> serverAddresses = new ArrayList<>();
    serverAddresses.add(serverAddress);
    serverAddresses.add(InetSocketAddress.createUnresolved("localhost", 9610));
    doThrow(new NoActiveException()).when(cmt).open(any(InetSocketAddress.class));
    try {
      spyConnEstablisher.open(serverAddresses, errorListener);
    } catch (IOException ioe) {
      assertTrue(ioe.getCause() instanceof NoActiveException);
    }
    verify(cmt).open(eq(serverAddresses.get(0)));
    verify(cmt).open(eq(serverAddresses.get(1)));
  }

  @Test
  public void test_client_tries_next_after_unknown_host_from_redirect() throws Exception {
    List<InetSocketAddress> serverAddresses = new ArrayList<>();
    serverAddresses.add(serverAddress);
    serverAddresses.add(InetSocketAddress.createUnresolved("localhost", 9610));
    Mockito.doAnswer((iom) -> {
      InetSocketAddress add = (InetSocketAddress)iom.getArguments()[0];
      if (add.equals(serverAddress)) {
        throw new TransportRedirect("unknown:9410");
      } else if (add.getHostString().equals("unknown")) {
        Socket s = new Socket(add.getHostString(), add.getPort());
        boolean connected = s.isConnected();
        throw new UnknownHostException("connected:" + connected);
      } else {
        throw new IOException("checked");
      }
    }).when(cmt).open(any(InetSocketAddress.class));
    try {
      spyConnEstablisher.open(serverAddresses, errorListener);
    } catch (IOException ioe) {
      assertTrue(ioe.getMessage().startsWith("checked"));
    }
  }
}
