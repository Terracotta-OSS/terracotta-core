/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.net.CommStackMismatchException;
import com.tc.net.MaxConnectionsExceededException;
import com.tc.net.TCSocketAddress;
import com.tc.net.core.ConnectionAddressProvider;
import com.tc.net.core.ConnectionInfo;
import com.tc.net.core.TCConnection;
import com.tc.net.protocol.PlainNetworkStackHarnessFactory;
import com.tc.net.protocol.tcm.ChannelEvent;
import com.tc.net.protocol.tcm.ChannelEventListener;
import com.tc.net.protocol.tcm.ChannelEventType;
import com.tc.net.protocol.tcm.ClientMessageChannel;
import com.tc.net.protocol.tcm.CommunicationsManager;
import com.tc.net.protocol.tcm.CommunicationsManagerImpl;
import com.tc.net.protocol.tcm.NetworkListener;
import com.tc.net.protocol.tcm.NullMessageMonitor;
import com.tc.object.session.NullSessionManager;
import com.tc.test.TCTestCase;
import com.tc.test.proxyconnect.ProxyConnectManagerImpl;
import com.tc.util.Assert;
import com.tc.util.TCTimeoutException;
import com.tc.util.concurrent.ThreadUtil;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ReconnectionRejectedEventTest extends TCTestCase {
  private final int                  timeout = (int) ClientMessageTransport.TRANSPORT_HANDSHAKE_SYNACK_TIMEOUT;
  private ProxyConnectManagerImpl    proxyMgr;
  private CommunicationsManager      serverComms;
  private CommunicationsManager      clientComms;
  private NetworkListener            listener;
  private TestClientMessageTransport clientMessageTransport;

  @Override
  protected void setUp() throws Exception {
    this.proxyMgr = new ProxyConnectManagerImpl();
    this.proxyMgr.setupProxy();
    this.serverComms = new CommunicationsManagerImpl("ServerTestCommsMgr", new NullMessageMonitor(),
                                                     new PlainNetworkStackHarnessFactory(), new NullConnectionPolicy());
    this.clientComms = new CommunicationsManagerImpl("ClientTestCommsMgr", new NullMessageMonitor(),
                                                     new PlainNetworkStackHarnessFactory(), new NullConnectionPolicy());
    this.listener = serverComms.createListener(new NullSessionManager(),
                                               new TCSocketAddress("localhost", proxyMgr.getDsoPort()), true,
                                               new DefaultConnectionIdFactory());
    this.listener.start(Collections.EMPTY_SET);
  }

  private void setTestClientMessageTransport(final TestClientMessageTransport cmt) {
    this.clientMessageTransport = cmt;
  }

  private final TestClientMessageTransport getTestClientMessageTransport() {
    return this.clientMessageTransport;
  }

  private ClientMessageChannel createClientMessageChannel(final int connPort) {

    // set up the transport factory
    MessageTransportFactory transportFactory = new MessageTransportFactory() {
      public MessageTransport createNewTransport() {
        ClientConnectionEstablisher clientConnectionEstablisher = buildClientConnectionEstablisher(connPort);
        TestClientMessageTransport cmt = new TestClientMessageTransport(clientConnectionEstablisher,
                                                                        new TransportHandshakeErrorNullHandler(),
                                                                        new TransportMessageFactoryImpl(),
                                                                        new WireProtocolAdaptorFactoryImpl(),
                                                                        TransportHandshakeMessage.NO_CALLBACK_PORT);
        setTestClientMessageTransport(cmt);
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

    return clientComms.createClientChannel(new NullSessionManager(), 0, "localhost", connPort, timeout,
                                           getConnectionAddrProvider(connPort), transportFactory);

  }

  private ClientConnectionEstablisher buildClientConnectionEstablisher(final int connPort) {
    return new ClientConnectionEstablisher(clientComms.getConnectionManager(), getConnectionAddrProvider(connPort), -1,
                                           timeout);
  }

  private ConnectionAddressProvider getConnectionAddrProvider(final int connPort) {
    return new ConnectionAddressProvider(new ConnectionInfo[] { new ConnectionInfo("localhost", connPort) });
  }

  public void testReconnectionRejectedEventReceived() {
    RejectedEventListener eventListener = new RejectedEventListener();
    ClientMessageChannel channel = createClientMessageChannel(proxyMgr.getDsoPort());
    channel.addListener(eventListener); // fire transport ReconnectionRejectedEvent
    getTestClientMessageTransport().fireReconnectionRejectedEvent();
    Assert.assertEquals(1, eventListener.waitForEventCount(1, 10 * 1000));
  }

  public void testProxyOffForRejectedEvent() {
    RejectedEventListener eventListener = new RejectedEventListener();
    ClientMessageChannel channel = createClientMessageChannel(proxyMgr.getProxyPort());
    channel.addListener(eventListener);
    this.proxyMgr.proxyUp();

    // open connection
    try {
      channel.open();
    } catch (TCTimeoutException e) {
      Assert.fail(e.getMessage());
    } catch (UnknownHostException e) {
      Assert.fail(e.getMessage());
    } catch (MaxConnectionsExceededException e) {
      Assert.fail(e.getMessage());
    } catch (IOException e) {
      Assert.fail(e.getMessage());
    } catch (CommStackMismatchException e) {
      Assert.fail(e.getMessage());
    }

    // sleep a little to make sure server connection ready
    ThreadUtil.reallySleep(500);
    // proxy off
    this.proxyMgr.proxyDown();
    this.proxyMgr.closeClientConnections();
    ThreadUtil.reallySleep(10000);
    this.proxyMgr.proxyUp();

    Assert.assertEquals(1, eventListener.waitForEventCount(1, 10 * 1000));
  }

  private static class RejectedEventListener implements ChannelEventListener {
    private final AtomicInteger counter = new AtomicInteger(0);

    public void notifyChannelEvent(ChannelEvent event) {
      if (event.getType() == ChannelEventType.TRANSPORT_RECONNECTION_REJECTED_EVENT) {
        counter.incrementAndGet();
      }
    }

    public int getEventCount() {
      return counter.get();
    }

    public int waitForEventCount(int expectedCount, int timeout) {
      long start = System.nanoTime();
      while (TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) < timeout) {
        int count = getEventCount();
        if (count == expectedCount) { return count; }
        ThreadUtil.reallySleep(1 * 1000);
      }
      return getEventCount();
    }

  }

  private static class TestClientMessageTransport extends ClientMessageTransport {

    public TestClientMessageTransport(ClientConnectionEstablisher clientConnectionEstablisher,
                                      TransportHandshakeErrorHandler handshakeErrorHandler,
                                      TransportHandshakeMessageFactory messageFactory,
                                      WireProtocolAdaptorFactory wireProtocolAdaptorFactory, int callbackPort) {
      super(clientConnectionEstablisher, handshakeErrorHandler, messageFactory, wireProtocolAdaptorFactory,
            callbackPort);
    }

    public void fireReconnectionRejectedEvent() {
      fireTransportReconnectionRejectedEvent();
    }
  }

}
