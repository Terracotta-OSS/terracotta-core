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
package com.tc.objectserver.impl;

import com.tc.net.ClientID;
import com.tc.net.StripeID;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.transport.ConnectionID;
import com.tc.net.protocol.transport.ConnectionIDFactoryListener;
import com.tc.net.protocol.transport.JvmIDUtil;
import com.tc.net.protocol.transport.NullConnectionIDFactoryImpl;
import com.tc.objectserver.persistence.ClientStatePersistor;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;
import com.tc.net.core.ProductID;
import com.tc.util.sequence.MutableSequence;
import java.util.EnumSet;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author tim
 */
public class ConnectionIDFactoryImplTest extends TCTestCase {
  private ConnectionIDFactoryImpl connectionIDFactory;
  private ClientStatePersistor persistor;
  private MutableSequence sequence;
  private ConnectionIDFactoryListener listener;

  @Override
  public void setUp() throws Exception {
    sequence = createSequence();
    persistor = createPersistor(sequence);
    connectionIDFactory = new ConnectionIDFactoryImpl(new NullConnectionIDFactoryImpl(), persistor, EnumSet.allOf(ProductID.class));
    listener = mock(ConnectionIDFactoryListener.class);
    connectionIDFactory.activate(new StripeID("abc123"), 0);
    connectionIDFactory.registerForConnectionIDEvents(listener);
  }

  public void testNextID() throws Exception {
    nextChannelId(0);
    ConnectionID id = connectionIDFactory.populateConnectionID(idWith("Mr. Bogus", -1L));
    assertEquals("Mr. Bogus", id.getJvmID());
    assertEquals(0, id.getChannelID());
    assertEquals("abc123", id.getServerID());
  }

  public void testCreateExistingID() throws Exception {
//  implementation can no longer check existing
    connectionIDFactory.populateConnectionID(idWith("aaa", 0));
  }

  public void testRemoveId() throws Exception {
    connectionIDFactory.channelRemoved(channelWithId(0));
    verify(listener).connectionIDDestroyed(Mockito.<ConnectionID>any());
  }

  private void nextChannelId(long id) {
    when(sequence.next()).thenReturn(id);
  }
  
  public void testProductNegotiation() {
    ConnectionIDFactoryImpl factory = new ConnectionIDFactoryImpl(new NullConnectionIDFactoryImpl(), persistor, EnumSet.of(ProductID.DIAGNOSTIC, ProductID.SERVER));
    StripeID sid = new StripeID("server1");
    factory.activate(sid, 0);
    ConnectionID cid = factory.populateConnectionID(new ConnectionID("jvmid", 0, sid.getName(), ProductID.PERMANENT));
    
    Assert.assertEquals(ProductID.SERVER, cid.getProductId());
    cid = factory.populateConnectionID(new ConnectionID("jvmid", 0, sid.getName(), ProductID.STRIPE));
    
    Assert.assertEquals(ProductID.SERVER, cid.getProductId());
  }
  
    
  public void testMismatchServerRejection() {
    ConnectionIDFactoryImpl factory = new ConnectionIDFactoryImpl(new NullConnectionIDFactoryImpl(), persistor, EnumSet.of(ProductID.DIAGNOSTIC, ProductID.SERVER));
    StripeID sid = new StripeID("server1");
    factory.activate(sid, 0);
    ConnectionID cid = factory.populateConnectionID(new ConnectionID("jvmid", 0, new StripeID("server2").getName(), ProductID.PERMANENT));
    
    Assert.assertEquals(ConnectionID.NULL_ID, cid);
    cid = factory.populateConnectionID(new ConnectionID("jvmid", 0, sid.getName(), ProductID.STRIPE));
    
    Assert.assertEquals(new ClientID(0), cid.getClientID());
  }
  
  public void testProductRefusal() {
    ConnectionIDFactoryImpl factory = new ConnectionIDFactoryImpl(new NullConnectionIDFactoryImpl(), persistor, EnumSet.noneOf(ProductID.class));
    factory.activate(StripeID.NULL_ID, 0);
    ConnectionID cid = factory.populateConnectionID(new ConnectionID("jvmid", 0, "serverid", ProductID.PERMANENT));
    
    Assert.assertEquals(ConnectionID.NULL_ID, cid);
    cid = factory.populateConnectionID(new ConnectionID("jvmid", 0, "serverid", ProductID.STRIPE));
    
    Assert.assertEquals(ConnectionID.NULL_ID, cid);
  }
  
  public void testListenerGetsRightProductType() {
    MessageChannel channel = mock(MessageChannel.class);
    when(channel.getConnectionID()).thenReturn(new ConnectionID(JvmIDUtil.getJvmID(), 1L, ProductID.PERMANENT));
    when(channel.getProductID()).thenReturn(ProductID.PERMANENT);
    connectionIDFactory.channelRemoved(channel);
    ArgumentCaptor<ConnectionID> cap = ArgumentCaptor.forClass(ConnectionID.class);
    verify(listener).connectionIDDestroyed(cap.capture());
    Assert.assertEquals(ProductID.PERMANENT, cap.getValue().getProductId());
  }
  
  private static MutableSequence createSequence() {
    MutableSequence sequence = mock(MutableSequence.class);
    return sequence;
  }

  private static ClientStatePersistor createPersistor(MutableSequence sequence) {
    ClientStatePersistor clientStatePersistor = mock(ClientStatePersistor.class);
    when(clientStatePersistor.getConnectionIDSequence()).thenReturn(sequence);
    return clientStatePersistor;
  }

  private static MessageChannel channelWithId(long id) {
    MessageChannel channel = mock(MessageChannel.class);
    when(channel.getProductID()).thenReturn(ProductID.PERMANENT);
    return when(channel.getChannelID()).thenReturn(new ChannelID(id)).getMock();
  }

  private static ConnectionID idWith(String jvmId, long channelId) {
    return new ConnectionID(jvmId, channelId);
  }
}
