/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */

package com.tc.objectserver.event;

import org.junit.Before;
import org.junit.Test;

import com.google.common.eventbus.EventBus;
import com.tc.object.ObjectID;
import com.tc.objectserver.managedobject.CDSMValue;
import com.tc.server.BasicServerEvent;
import com.tc.server.CustomLifespanVersionedServerEvent;
import com.tc.server.ServerEvent;
import com.tc.server.ServerEventType;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * @author Eugene Shelestovich
 */
public class MutationEventPublisherTest {

  private static final String CACHE_NAME = "cache";
  private static final byte[] VALUE = new byte[] { 1 };
  private static final ObjectID OID = new ObjectID(1);

  private ServerEventPublisher serverEventPublisher;
  private EventBus eventBus;
  private MutationEventPublisher recorder;

  @Before
  public void setUp() throws Exception {
    eventBus = mock(EventBus.class);
    serverEventPublisher = new ServerEventPublisher(eventBus);
    recorder = new DefaultMutationEventPublisher(serverEventPublisher);
  }

  @Test
  public void testNoPublishWithoutValue() throws Exception {
    recorder.publishEvent(ServerEventType.PUT, 1, new CDSMValue(OID), CACHE_NAME);
    verify(eventBus, never()).post(any(ServerEvent.class));
  }

  @Test
  public void testPublishWhenBytesComeSecond() throws Exception {
    recorder.publishEvent(ServerEventType.PUT, 1, new CDSMValue(OID, 1, 2, 3, 4, 5), CACHE_NAME);
    recorder.setBytesForObjectID(OID, VALUE);
    verify(eventBus).post(new CustomLifespanVersionedServerEvent(new BasicServerEvent(ServerEventType.PUT, 1, VALUE, 5, CACHE_NAME), 1, 3, 4));
  }

  @Test
  public void testPublishWhenBytesComeFirst() throws Exception {
    recorder.setBytesForObjectID(OID, VALUE);
    recorder.publishEvent(ServerEventType.PUT, 1, new CDSMValue(OID, 1, 2, 3, 4, 5), CACHE_NAME);
    verify(eventBus).post(new CustomLifespanVersionedServerEvent(new BasicServerEvent(ServerEventType.PUT, 1, VALUE, 5, CACHE_NAME), 1, 3, 4));
  }

  @Test
  public void testPublishWhenNoValue() throws Exception {
    recorder.publishEvent(ServerEventType.REMOVE, "foo", new CDSMValue(ObjectID.NULL_ID), CACHE_NAME);
    verify(eventBus).post(new CustomLifespanVersionedServerEvent(new BasicServerEvent(ServerEventType.REMOVE, "foo", new byte[0], 0, CACHE_NAME), 0, 0, 0));
  }

  @Test
  public void testMultipleEventsOneObjectID() throws Exception {
    recorder.publishEvent(ServerEventType.PUT, 1, new CDSMValue(OID, 1, 2, 3, 4, 5), CACHE_NAME);
    recorder.setBytesForObjectID(OID, VALUE);
    verify(eventBus).post(new CustomLifespanVersionedServerEvent(new BasicServerEvent(ServerEventType.PUT, 1, VALUE, 5, CACHE_NAME), 1, 3, 4));
    recorder.publishEvent(ServerEventType.PUT_LOCAL, 1, new CDSMValue(OID, 5, 5, 3, 2, 1), CACHE_NAME);
    verify(eventBus).post(new CustomLifespanVersionedServerEvent(new BasicServerEvent(ServerEventType.PUT_LOCAL, 1, VALUE, 1, CACHE_NAME), 5, 3, 2));
  }
}
