/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.net.ClientID;
import com.tc.net.StripeID;
import com.tc.net.core.TCConnection;
import com.tc.net.protocol.NetworkStackHarnessFactory;
import com.tc.net.protocol.RejectReconnectionException;
import com.tc.net.protocol.ServerNetworkStackHarness;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.MessageChannelInternal;
import com.tc.net.protocol.tcm.ServerMessageChannelFactory;
import com.tc.objectserver.impl.ConnectionIDFactoryImpl;
import com.tc.objectserver.persistence.ClientStatePersistor;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;
import com.tc.util.ProductID;
import com.tc.util.sequence.MutableSequence;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class ServerStackProviderTest extends TCTestCase {

  private ServerStackProvider            provider;
  private ConnectionPolicy           connectionPolicy;

  public ServerStackProviderTest() {
    super();
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    connectionPolicy = mock(ConnectionPolicy.class);
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  public void testAttachAlreadyConnectedTransport() throws Exception {
    NetworkStackHarnessFactory harnessFactory = when(mock(NetworkStackHarnessFactory.class).createServerHarness(
        any(ServerMessageChannelFactory.class), any(MessageTransport.class), any(MessageTransportListener[].class))).then(
            invoke->{
              Object[] args = invoke.getArguments();
              return new ServerNetworkStackHarness((ServerMessageChannelFactory)args[0], (MessageTransport)args[1]);
            }
        ).getMock();
    ServerMessageChannelFactory serverMessageChannelFactory = mock(ServerMessageChannelFactory.class);
    when(serverMessageChannelFactory.createNewChannel(any(ChannelID.class), any(ProductID.class))).then(invoke->{
      MessageChannelInternal channel = mock(MessageChannelInternal.class);
      ProductID product = (ProductID)invoke.getArguments()[1];
      when(channel.getProductId()).thenReturn(product);
      return channel;
    });
    MessageTransportFactory messageTransportFactory = mock(MessageTransportFactory.class);

    when(messageTransportFactory.createNewTransport(any(TCConnection.class), any(TransportHandshakeErrorHandler.class), 
        any(TransportHandshakeMessageFactory.class), any(List.class))).then(invoke->{
          Object[] args = invoke.getArguments();
          return new ServerMessageTransport((TCConnection)args[0], (TransportHandshakeErrorHandler)args[1], (TransportHandshakeMessageFactory)args[2]);
        });
    DefaultConnectionIdFactory connectionIdFactory = new DefaultConnectionIdFactory();
    ServerStackProvider serverStackProvider = new ServerStackProvider(Collections.emptySet(), harnessFactory, serverMessageChannelFactory, messageTransportFactory,
        spy(new TransportMessageFactoryImpl()), connectionIdFactory, new NullConnectionPolicy(), mock(WireProtocolAdaptorFactory.class), new ReentrantLock());

    // This is the next connection ID the connectionIDFactory is going to assign out, need to save it first before it
    // gets changed by the connectionIDFactory assigning it out.
    ConnectionID nextConnectionID = new ConnectionID("foo", connectionIdFactory.getCurrentConnectionID(), connectionIdFactory.getServerID());

    TCConnection connection = mock(TCConnection.class);
    // Attach once for the normal case.
    try {
      serverStackProvider.attachNewConnection(new ConnectionID("foo", -1), connection);
    } catch (Exception e) {
      e.printStackTrace();
      throw e;
    }

    // Now make the attach fail due to IllegalReconnectException, we should force a reconnection rejected exception now
//    when(harness.attachNewConnection(any(TCConnection.class))).thenThrow(new IllegalReconnectException());
    try {
      serverStackProvider.attachNewConnection(nextConnectionID, connection);
      fail("Should have gotten a reconnection rejected when attaching a new TCConnection results in an IllegalReconnectException");
    } catch (RejectReconnectionException e) {
      // expected
    }
  }
  
  public void testRebuildStack() throws Exception {
    NetworkStackHarnessFactory harnessFactory = when(mock(NetworkStackHarnessFactory.class).createServerHarness(
        any(ServerMessageChannelFactory.class), any(MessageTransport.class), any(MessageTransportListener[].class))).then(
            invoke->{
              Object[] args = invoke.getArguments();
              return new ServerNetworkStackHarness((ServerMessageChannelFactory)args[0], (MessageTransport)args[1]);
            }
        ).getMock();
    ServerMessageChannelFactory serverMessageChannelFactory = mock(ServerMessageChannelFactory.class);
    when(serverMessageChannelFactory.createNewChannel(any(ChannelID.class), any(ProductID.class))).then(invoke->{
      MessageChannelInternal channel = mock(MessageChannelInternal.class);
      ProductID product = (ProductID)invoke.getArguments()[1];
      when(channel.getProductId()).thenReturn(product);
      return channel;
    });
    MessageTransportFactory messageTransportFactory = mock(MessageTransportFactory.class);

    when(messageTransportFactory.createNewTransport(any(TCConnection.class), any(TransportHandshakeErrorHandler.class), 
        any(TransportHandshakeMessageFactory.class), any(List.class))).then(invoke->{
          Object[] args = invoke.getArguments();
          return new ServerMessageTransport((TCConnection)args[0], (TransportHandshakeErrorHandler)args[1], (TransportHandshakeMessageFactory)args[2]);
        });
    when(messageTransportFactory.createNewTransport(any(TransportHandshakeErrorHandler.class), 
        any(TransportHandshakeMessageFactory.class), any(List.class))).then(invoke->{
          Object[] args = invoke.getArguments();
          return new ServerMessageTransport((TransportHandshakeErrorHandler)args[0], (TransportHandshakeMessageFactory)args[1]);
        });
    
    ClientStatePersistor sequence = mock(ClientStatePersistor.class);
    when(sequence.getConnectionIDSequence()).thenReturn(new MutableSequence() {
      long current = 0;
      @Override
      public void setNext(long next) {
        current = next;
      }

      @Override
      public long next() {
        return ++current;
      }

      @Override
      public long current() {
        return current;
      }
    });
    ConnectionIDFactory factory = new ConnectionIDFactoryImpl(new NullConnectionIDFactoryImpl(), sequence, EnumSet.allOf(ProductID.class));
    factory.activate(new StripeID("server1"), 3);

    ConnectionID connectionID1 = new ConnectionID("JVM", 1, "server1");
    ConnectionID connectionID2 = new ConnectionID("JVM", 2, "server1");
    Set<ClientID> rebuild = new HashSet<>();
    rebuild.add(connectionID1.getClientID());
    

    provider = new ServerStackProvider(rebuild, harnessFactory,
                                       serverMessageChannelFactory, messageTransportFactory, null, factory, connectionPolicy,
                                       new WireProtocolAdaptorFactoryImpl(), new ReentrantLock());

    TCConnection conn = mock(TCConnection.class);
    provider.attachNewConnection(connectionID1, conn);

    // trying to attach a stack that wasn't rebuilt at startup should fail.
    try {
      provider.attachNewConnection(connectionID2, conn);
      fail("Expected StackNotFoundException");
    } catch (RejectReconnectionException e) {
      // expected.
    }
  }
  

  public void testRebuildStackWithProductNegtiation() throws Exception {
    NetworkStackHarnessFactory harnessFactory = when(mock(NetworkStackHarnessFactory.class).createServerHarness(
        any(ServerMessageChannelFactory.class), any(MessageTransport.class), any(MessageTransportListener[].class))).then(
            invoke->{
              Object[] args = invoke.getArguments();
              return new ServerNetworkStackHarness((ServerMessageChannelFactory)args[0], (MessageTransport)args[1]);
            }
        ).getMock();
    ServerMessageChannelFactory serverMessageChannelFactory = mock(ServerMessageChannelFactory.class);
    when(serverMessageChannelFactory.createNewChannel(any(ChannelID.class), any(ProductID.class))).then(invoke->{
      MessageChannelInternal channel = mock(MessageChannelInternal.class);
      ProductID product = (ProductID)invoke.getArguments()[1];
      when(channel.getProductId()).thenReturn(product);
      return channel;
    });
    MessageTransportFactory messageTransportFactory = mock(MessageTransportFactory.class);

    when(messageTransportFactory.createNewTransport(any(TCConnection.class), any(TransportHandshakeErrorHandler.class), 
        any(TransportHandshakeMessageFactory.class), any(List.class))).then(invoke->{
          Object[] args = invoke.getArguments();
          return new ServerMessageTransport((TCConnection)args[0], (TransportHandshakeErrorHandler)args[1], (TransportHandshakeMessageFactory)args[2]);
        });
    when(messageTransportFactory.createNewTransport(any(TransportHandshakeErrorHandler.class), 
        any(TransportHandshakeMessageFactory.class), any(List.class))).then(invoke->{
          Object[] args = invoke.getArguments();
          return new ServerMessageTransport((TransportHandshakeErrorHandler)args[0], (TransportHandshakeMessageFactory)args[1]);
        });

    ClientStatePersistor sequence = mock(ClientStatePersistor.class);
    when(sequence.getConnectionIDSequence()).thenReturn(new MutableSequence() {
      long current = 0;
      @Override
      public void setNext(long next) {
        current = next;
      }

      @Override
      public long next() {
        return ++current;
      }

      @Override
      public long current() {
        return current;
      }
    });
    ConnectionIDFactory factory = new ConnectionIDFactoryImpl(new NullConnectionIDFactoryImpl(), sequence, EnumSet.complementOf(EnumSet.of(ProductID.PERMANENT)));
    factory.activate(new StripeID("server1"), 3);

    ConnectionID connectionID1 = new ConnectionID("JVM", 1, "server1");
    ConnectionID connectionID2 = new ConnectionID("JVM", 2, "server1");

    Set<ClientID> rebuild = new HashSet<>();
    rebuild.add(connectionID1.getClientID());

    provider = new ServerStackProvider(rebuild, harnessFactory,
                                       serverMessageChannelFactory, messageTransportFactory, null, factory, connectionPolicy,
                                       new WireProtocolAdaptorFactoryImpl(), new ReentrantLock());

    TCConnection conn = mock(TCConnection.class);
    MessageTransport transport = provider.attachNewConnection(connectionID1, conn);
    Assert.assertEquals(ProductID.PERMANENT, connectionID1.getProductId());
    Assert.assertEquals(ProductID.PERMANENT, connectionID2.getProductId());
    Assert.assertEquals(1L, connectionID1.getChannelID());
    Assert.assertEquals(2L, connectionID2.getChannelID());
    Assert.assertEquals(ProductID.STRIPE, transport.getConnectionId().getProductId());

    // trying to attach a stack that wasn't rebuilt at startup should fail.
    try {
      provider.attachNewConnection(connectionID2, conn);
      fail("Expected StackNotFoundException");
    } catch (RejectReconnectionException e) {
      // expected.
    }
  }  
}
