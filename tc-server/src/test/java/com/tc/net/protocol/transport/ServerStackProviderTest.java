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
import com.tc.net.core.ProductID;
import com.tc.util.sequence.MutableSequence;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import static org.mockito.ArgumentMatchers.any;
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
    NetworkStackHarnessFactory harnessFactory = mock(NetworkStackHarnessFactory.class);
    when(harnessFactory
        .createServerHarness(
            any(ServerMessageChannelFactory.class), any(MessageTransport.class), any(MessageTransportListener[].class)))
        .then(
            invoke -> {
              Object[] args = invoke.getArguments();
              return new ServerNetworkStackHarness((ServerMessageChannelFactory)args[0], (MessageTransport)args[1]);
            }
        );
    ServerMessageChannelFactory serverMessageChannelFactory = mock(ServerMessageChannelFactory.class);
    when(serverMessageChannelFactory.createNewChannel(any(ChannelID.class))).then(invoke->{
      MessageChannelInternal channel = mock(MessageChannelInternal.class);
      ProductID product = ProductID.PERMANENT;
      when(channel.getProductID()).thenReturn(product);
      when(channel.getChannelID()).thenReturn((ChannelID)invoke.getArguments()[0]);
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
    try {
      serverStackProvider.attachNewConnection(nextConnectionID, connection);
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
    when(serverMessageChannelFactory.createNewChannel(any(ChannelID.class))).then(invoke->{
      MessageChannelInternal channel = mock(MessageChannelInternal.class);
      ProductID product = ProductID.PERMANENT;
      when(channel.getProductID()).thenReturn(product);
      when(channel.getChannelID()).thenReturn((ChannelID)invoke.getArguments()[0]);
      return channel;
    });
    MessageTransportFactory messageTransportFactory = mock(MessageTransportFactory.class);

    when(messageTransportFactory.createNewTransport(any(TCConnection.class), any(TransportHandshakeErrorHandler.class),
        Mockito.<TransportHandshakeMessageFactory>any(), any(List.class))).then(invoke->{
          Object[] args = invoke.getArguments();
          return new ServerMessageTransport((TCConnection)args[0], (TransportHandshakeErrorHandler)args[1], (TransportHandshakeMessageFactory)args[2]);
        });
    when(messageTransportFactory.createNewTransport(any(TransportHandshakeErrorHandler.class),
            Mockito.<TransportHandshakeMessageFactory>any(), any(List.class))).then(invoke->{
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
    Set<ConnectionID> rebuild = new HashSet<>();
    rebuild.add(connectionID1);


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
    when(serverMessageChannelFactory.createNewChannel(any(ChannelID.class))).then(invoke->{
      MessageChannelInternal channel = mock(MessageChannelInternal.class);
      ProductID product = ProductID.PERMANENT;
      when(channel.getProductID()).thenReturn(product);
      when(channel.getChannelID()).thenReturn((ChannelID)invoke.getArguments()[0]);
      return channel;
    });
    MessageTransportFactory messageTransportFactory = mock(MessageTransportFactory.class);

    when(messageTransportFactory.createNewTransport(any(TCConnection.class), any(TransportHandshakeErrorHandler.class),
            Mockito.<TransportHandshakeMessageFactory>any(), any(List.class))).then(invoke->{
          Object[] args = invoke.getArguments();
          return new ServerMessageTransport((TCConnection)args[0], (TransportHandshakeErrorHandler)args[1], (TransportHandshakeMessageFactory)args[2]);
        });
    when(messageTransportFactory.createNewTransport(any(TransportHandshakeErrorHandler.class),
            Mockito.<TransportHandshakeMessageFactory>any(), any(List.class))).then(invoke->{
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

    Set<ConnectionID> rebuild = new HashSet<>();
    rebuild.add(connectionID1);

    provider = new ServerStackProvider(rebuild, harnessFactory,
                                       serverMessageChannelFactory, messageTransportFactory, null, factory, connectionPolicy,
                                       new WireProtocolAdaptorFactoryImpl(), new ReentrantLock());

    TCConnection conn = mock(TCConnection.class);
    MessageTransport transport = provider.attachNewConnection(connectionID1, conn);
    Assert.assertEquals(ProductID.PERMANENT, connectionID1.getProductId());
    Assert.assertEquals(ProductID.PERMANENT, connectionID2.getProductId());
    Assert.assertEquals(1L, connectionID1.getChannelID());
    Assert.assertEquals(2L, connectionID2.getChannelID());
    Assert.assertEquals(ProductID.STRIPE, transport.getConnectionID().getProductId());

    // trying to attach a stack that wasn't rebuilt at startup should fail.
    try {
      provider.attachNewConnection(connectionID2, conn);
      fail("Expected StackNotFoundException");
    } catch (RejectReconnectionException e) {
      // expected.
    }
  }
}
