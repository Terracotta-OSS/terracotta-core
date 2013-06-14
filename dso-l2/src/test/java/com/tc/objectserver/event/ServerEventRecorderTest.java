/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */

package com.tc.objectserver.event;

import org.junit.Test;

import com.tc.object.ObjectID;
import com.tc.server.ServerEvent;
import com.tc.server.ServerEventType;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Eugene Shelestovich
 */
public class ServerEventRecorderTest {

  @Test
  public void testShouldResolveKeyToValuePairsOnGet() {
    final String cacheName = "test-cache";
    final ServerEventRecorder recorder = new DefaultServerEventRecorder();
    final ObjectID objectId1 = new ObjectID(1001);
    recorder.recordEvent(ServerEventType.PUT, 1, objectId1, cacheName);
    final ObjectID objectId2 = new ObjectID(1002);
    recorder.recordEvent(ServerEventType.REMOVE, 1, objectId2, cacheName);
    recorder.recordEventValue(objectId2, new byte[] { 12 });
    recorder.recordEventValue(objectId1, new byte[] { 11 });

    final ObjectID objectId3 = new ObjectID(1003);
    recorder.recordEvent(ServerEventType.PUT, 2, objectId3, cacheName);
    recorder.recordEventValue(objectId3, new byte[] { 13 });

    final List<ServerEvent> events = recorder.getEvents();
    assertEquals(3, events.size());

    assertEquals(ServerEventType.PUT, events.get(0).getType());
    assertEquals(11, events.get(0).getValue()[0]);

    assertEquals(ServerEventType.REMOVE, events.get(1).getType());
    assertEquals(12, events.get(1).getValue()[0]);

    assertEquals(ServerEventType.PUT, events.get(2).getType());
    assertEquals(13, events.get(2).getValue()[0]);
  }

  @Test
  public void testShouldResolveKeyToValuePairsWhenValueGoesFirst() {
    final String cacheName = "test-cache";
    final ServerEventRecorder recorder = new DefaultServerEventRecorder();

    final ObjectID objectId1 = new ObjectID(1001);
    final ObjectID objectId2 = new ObjectID(1002);

    recorder.recordEventValue(objectId2, new byte[] { 12 });
    recorder.recordEventValue(objectId1, new byte[] { 11 });

    recorder.recordEvent(ServerEventType.PUT, 1, objectId1, cacheName);
    recorder.recordEvent(ServerEventType.REMOVE, 1, objectId2, cacheName);

    final ObjectID objectId3 = new ObjectID(1003);
    recorder.recordEvent(ServerEventType.PUT, 2, objectId3, cacheName);
    recorder.recordEventValue(objectId3, new byte[] { 13 });

    final List<ServerEvent> events = recorder.getEvents();
    assertEquals(3, events.size());

    assertEquals(ServerEventType.PUT, events.get(0).getType());
    assertEquals(11, events.get(0).getValue()[0]);

    assertEquals(ServerEventType.REMOVE, events.get(1).getType());
    assertEquals(12, events.get(1).getValue()[0]);

    assertEquals(ServerEventType.PUT, events.get(2).getType());
    assertEquals(13, events.get(2).getValue()[0]);
  }

  @Test
  public void testShouldResolveKeyToValuePairsMultipleEventsPerObjectId() {
    final String cacheName = "test-cache";
    final ServerEventRecorder recorder = new DefaultServerEventRecorder();

    final ObjectID objectId1 = new ObjectID(1001);

    recorder.recordEvent(ServerEventType.PUT, 1, objectId1, cacheName);
    recorder.recordEvent(ServerEventType.PUT_LOCAL, 1, objectId1, cacheName);

    recorder.recordEventValue(objectId1, new byte[] { 11 });

    final List<ServerEvent> events = recorder.getEvents();
    assertEquals(2, events.size());

    assertEquals(ServerEventType.PUT, events.get(0).getType());
    assertEquals(11, events.get(0).getValue()[0]);

    assertEquals(ServerEventType.PUT_LOCAL, events.get(1).getType());
    assertEquals(11, events.get(1).getValue()[0]);
  }

}
