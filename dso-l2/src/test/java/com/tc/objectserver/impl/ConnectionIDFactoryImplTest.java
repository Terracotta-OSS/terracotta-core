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

package com.tc.objectserver.impl;

import com.tc.net.StripeID;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.transport.ConnectionID;
import com.tc.net.protocol.transport.ConnectionIDFactoryListener;
import com.tc.objectserver.persistence.ClientStatePersistor;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;
import com.tc.util.ProductID;
import com.tc.util.sequence.MutableSequence;
import java.util.EnumSet;
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
    connectionIDFactory = new ConnectionIDFactoryImpl(persistor, EnumSet.allOf(ProductID.class));
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
    connectionIDFactory.channelRemoved(channelWithId(0), true);
    verify(listener).connectionIDDestroyed(Mockito.any(ConnectionID.class));
  }

  private void nextChannelId(long id) {
    when(sequence.next()).thenReturn(id);
  }
  
  public void testProductNegotiation() {
    ConnectionIDFactoryImpl factory = new ConnectionIDFactoryImpl(persistor, EnumSet.of(ProductID.DIAGNOSTIC, ProductID.SERVER));
    factory.activate(StripeID.NULL_ID, 0);
    ConnectionID cid = factory.populateConnectionID(new ConnectionID("jvmid", 0, "serverid", null, null, ProductID.PERMANENT));
    
    Assert.assertEquals(ProductID.SERVER, cid.getProductId());
    cid = factory.populateConnectionID(new ConnectionID("jvmid", 0, "serverid", null, null, ProductID.STRIPE));
    
    Assert.assertEquals(ProductID.SERVER, cid.getProductId());
  }
  public void testProductRefusal() {
    ConnectionIDFactoryImpl factory = new ConnectionIDFactoryImpl(persistor, EnumSet.noneOf(ProductID.class));
    factory.activate(StripeID.NULL_ID, 0);
    ConnectionID cid = factory.populateConnectionID(new ConnectionID("jvmid", 0, "serverid", null, null, ProductID.PERMANENT));
    
    Assert.assertEquals(ConnectionID.NULL_ID, cid);
    cid = factory.populateConnectionID(new ConnectionID("jvmid", 0, "serverid", null, null, ProductID.STRIPE));
    
    Assert.assertEquals(ConnectionID.NULL_ID, cid);
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
    return when(mock(MessageChannel.class).getChannelID()).thenReturn(new ChannelID(id)).getMock();
  }

  private static ConnectionID idWith(String jvmId, long channelId) {
    return new ConnectionID(jvmId, channelId);
  }
}
