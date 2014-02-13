/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */

package com.tc.objectserver.event;

import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.google.common.collect.Sets;
import com.tc.net.ClientID;
import com.tc.object.ObjectID;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.objectserver.managedobject.CDSMValue;
import com.tc.server.BasicServerEvent;
import com.tc.server.CustomLifespanVersionedServerEvent;
import com.tc.server.ServerEventType;

import java.util.Set;

/**
 * @author Eugene Shelestovich
 */
public class MutationEventPublisherTest {

  private static final String CACHE_NAME = "cache";
  private static final byte[] VALUE = new byte[] { 1 };
  private static final ObjectID OID = new ObjectID(1);

  private MutationEventPublisher publisher;
  private GlobalTransactionID    gtxId;
  private Set<ClientID>          clientIds;
  @Mock private ServerEventBuffer serverEventBuffer;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    gtxId = new GlobalTransactionID(1);
    publisher = new DefaultMutationEventPublisher(gtxId, serverEventBuffer);
    clientIds = Sets.newHashSet();
    clientIds.add(new ClientID(1));
    clientIds.add(new ClientID(2));
  }

  @Test
  public void testNoPublishWithoutValue() throws Exception {
    publisher.publishEvent(clientIds, ServerEventType.PUT, 1, new CDSMValue(OID), CACHE_NAME);
    Mockito.verifyZeroInteractions(serverEventBuffer);
  }

  @Test
  public void testPublishWhenBytesComeSecond() throws Exception {
    publisher.publishEvent(clientIds, ServerEventType.PUT, 1, new CDSMValue(OID, 1, 2, 3, 4, 5), CACHE_NAME);
    publisher.setBytesForObjectID(OID, VALUE);
    verify(serverEventBuffer).storeEvent(gtxId, new CustomLifespanVersionedServerEvent(
              new BasicServerEvent(ServerEventType.PUT, 1, VALUE, 5, CACHE_NAME), 1, 3, 4), clientIds);
  }

  @Test
  public void testPublishWhenBytesComeFirst() throws Exception {
    publisher.setBytesForObjectID(OID, VALUE);
    publisher.publishEvent(clientIds, ServerEventType.PUT, 1, new CDSMValue(OID, 1, 2, 3, 4, 5), CACHE_NAME);
    verify(serverEventBuffer).storeEvent(gtxId, new CustomLifespanVersionedServerEvent(
              new BasicServerEvent(ServerEventType.PUT, 1, VALUE, 5, CACHE_NAME), 1, 3, 4), clientIds);
  }

  @Test
  public void testPublishWhenNoValue() throws Exception {
    publisher.publishEvent(clientIds, ServerEventType.REMOVE, "foo", new CDSMValue(ObjectID.NULL_ID), CACHE_NAME);
    verify(serverEventBuffer).storeEvent(gtxId, new CustomLifespanVersionedServerEvent
              (new BasicServerEvent(ServerEventType.REMOVE, "foo", new byte[0], 0, CACHE_NAME), 0, 0, 0), clientIds);
  }

  @Test
  public void testMultipleEventsOneObjectID() throws Exception {
    publisher.publishEvent(clientIds, ServerEventType.PUT, 1, new CDSMValue(OID, 1, 2, 3, 4, 5), CACHE_NAME);
    publisher.setBytesForObjectID(OID, VALUE);
    verify(serverEventBuffer).storeEvent(gtxId, new CustomLifespanVersionedServerEvent(
              new BasicServerEvent(ServerEventType.PUT, 1, VALUE, 5, CACHE_NAME), 1, 3, 4), clientIds);
    publisher.publishEvent(clientIds, ServerEventType.PUT_LOCAL, 1, new CDSMValue(OID, 5, 5, 3, 2, 1), CACHE_NAME);
    verify(serverEventBuffer).storeEvent(gtxId, new CustomLifespanVersionedServerEvent(
              new BasicServerEvent(ServerEventType.PUT_LOCAL, 1, VALUE, 1, CACHE_NAME), 5, 3, 2), clientIds);
  }
}
