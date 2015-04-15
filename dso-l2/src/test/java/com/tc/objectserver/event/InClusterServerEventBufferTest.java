/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.objectserver.event;

import static com.tc.server.ServerEventType.EVICT;
import static com.tc.server.ServerEventType.PUT;
import static com.tc.server.ServerEventType.REMOVE;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.tc.net.ClientID;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.server.BasicServerEvent;
import com.tc.server.ServerEvent;

/**
 * @author Eugene Shelestovich
 */
public class InClusterServerEventBufferTest {

  private InClusterServerEventBuffer buffer;
  
  private final ClientID clientId1 = new ClientID(1L);
  private final ClientID clientId2 = new ClientID(2L);
  private final ClientID clientId3 = new ClientID(3L);
  
  private final GlobalTransactionID gtxId1 = new GlobalTransactionID(1);
  private final GlobalTransactionID gtxId2 = new GlobalTransactionID(2);
  private final GlobalTransactionID gtxId3 = new GlobalTransactionID(3);
  
  private final ServerEvent event1 = new BasicServerEvent(PUT, 1, "cache1");
  private final ServerEvent event2 = new BasicServerEvent(EVICT, 2, "cache1");
  private final ServerEvent event3 = new BasicServerEvent(PUT, 3, "cache2");
  private final ServerEvent event33   = new BasicServerEvent(REMOVE, 5, "cache3");

  @Before
  public void setUp() {
    buffer = new InClusterServerEventBuffer();

    buffer.storeEvent(gtxId1, event1, Sets.newHashSet(clientId1));
    buffer.storeEvent(gtxId2, event2, Sets.newHashSet(clientId2, clientId3));
    buffer.storeEvent(gtxId3, event3, Sets.newHashSet(clientId1, clientId3));
    buffer.storeEvent(gtxId3, event33, Sets.newHashSet(clientId3));
  }

  @Test
  public void testStoreEvent() throws Exception {
    
    Multimap<ClientID, ServerEvent> eventsForGtxId1 = buffer.getServerEventsPerClient(gtxId1);
    Assert.assertTrue(eventsForGtxId1.size() == 1);
    Assert.assertTrue(eventsForGtxId1.get(clientId1).equals(Lists.newArrayList(event1)));
    
    Multimap<ClientID, ServerEvent> eventsForGtxId2 = buffer.getServerEventsPerClient(gtxId2);
    Assert.assertTrue(eventsForGtxId2.size() == 2);
    Assert.assertTrue(eventsForGtxId2.get(clientId2).equals(Lists.newArrayList(event2)));
    Assert.assertTrue(eventsForGtxId2.get(clientId3).equals(Lists.newArrayList(event2)));
    
    Multimap<ClientID, ServerEvent> eventsForGtxId3 = buffer.getServerEventsPerClient(gtxId3);
    Assert.assertTrue(eventsForGtxId3.size() == 3);
    Assert.assertTrue(eventsForGtxId3.get(clientId1).equals(Lists.newArrayList(event3)));
    Assert.assertTrue(eventsForGtxId3.get(clientId3).equals(Lists.newArrayList(event3, event33)));
  }

  @Test
  public void testRemoveEventsForTransaction() throws Exception {
    buffer.removeEventsForTransaction(gtxId3);

    Multimap<ClientID, ServerEvent> eventsForGtxId1 = buffer.getServerEventsPerClient(gtxId1);
    Assert.assertTrue(eventsForGtxId1.size() == 1);
    Assert.assertTrue(eventsForGtxId1.get(clientId1).equals(Lists.newArrayList(event1)));

    Multimap<ClientID, ServerEvent> eventsForGtxId2 = buffer.getServerEventsPerClient(gtxId2);
    Assert.assertTrue(eventsForGtxId2.size() == 2);
    Assert.assertTrue(eventsForGtxId2.get(clientId2).equals(Lists.newArrayList(event2)));
    Assert.assertTrue(eventsForGtxId2.get(clientId3).equals(Lists.newArrayList(event2)));

    Multimap<ClientID, ServerEvent> eventsForGtxId3 = buffer.getServerEventsPerClient(gtxId3);
    Assert.assertTrue(eventsForGtxId3.size() == 0);
  }

  @Test
  public void testClearEventBufferBelowLowWaterMark() throws Exception {
    buffer.clearEventBufferBelowLowWaterMark(gtxId3);

    Multimap<ClientID, ServerEvent> eventsForGtxId1 = buffer.getServerEventsPerClient(gtxId1);
    Assert.assertTrue(eventsForGtxId1.size() == 0);

    Multimap<ClientID, ServerEvent> eventsForGtxId2 = buffer.getServerEventsPerClient(gtxId2);
    Assert.assertTrue(eventsForGtxId2.size() == 0);

    Multimap<ClientID, ServerEvent> eventsForGtxId3 = buffer.getServerEventsPerClient(gtxId3);
    Assert.assertTrue(eventsForGtxId3.size() == 3);
    Assert.assertTrue(eventsForGtxId3.get(clientId1).equals(Lists.newArrayList(event3)));
    Assert.assertTrue(eventsForGtxId3.get(clientId3).equals(Lists.newArrayList(event3, event33)));
  }

}
