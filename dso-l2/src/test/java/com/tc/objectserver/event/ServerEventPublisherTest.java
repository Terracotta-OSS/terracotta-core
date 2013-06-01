/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */

package com.tc.objectserver.event;

import org.junit.Test;

import com.google.common.eventbus.EventBus;
import com.tc.server.BasicServerEvent;
import com.tc.server.ServerEvent;
import com.tc.server.ServerEventType;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Eugene Shelestovich
 */
public class ServerEventPublisherTest {

  @Test
  public void testShouldPostEventsToListeners() {
    final String cacheName = "test-cache";
    final ServerEventPublisher publisher = new ServerEventPublisher(new EventBus("test-bus"));
    final ServerEventDumper consumer1 = new ServerEventDumper();
    final TypedServerEventDumper consumer2 = new TypedServerEventDumper();
    publisher.register(consumer1);
    publisher.register(consumer2);

    final ServerEvent event1 = new BasicServerEvent(ServerEventType.PUT, 1, new byte[] { 101 }, cacheName);
    final ServerEvent event2 = new BasicServerEvent(ServerEventType.REMOVE, 1, cacheName);
    final ServerEvent event3 = new BasicServerEvent(ServerEventType.EVICT, 2, cacheName);

    publisher.post(event1, event2, event3);

    assertConsumer(consumer1);
    assertConsumer(consumer2);
  }

  private void assertConsumer(final Consumer consumer) {
    assertEquals(3, consumer.getEvents().size());
    assertTrue(consumer.getEvents().get(0).getType() == ServerEventType.PUT);
    assertTrue(consumer.getEvents().get(1).getType() == ServerEventType.REMOVE);
    assertTrue(consumer.getEvents().get(2).getType() == ServerEventType.EVICT);
  }

  private static final class ServerEventDumper implements ServerEventListener, Consumer {
    private final List<ServerEvent> events = new ArrayList<ServerEvent>();

    @Override
    public void handleServerEvent(final ServerEvent event) {
      System.out.println(getClass().getSimpleName() + " has received a new message: " + event);
      events.add(event);
    }

    @Override
    public List<ServerEvent> getEvents() {
      return events;
    }
  }

  private static final class TypedServerEventDumper implements ServerEventListener, Consumer {
    private final List<ServerEvent> events = new ArrayList<ServerEvent>();

    @Override
    public List<ServerEvent> getEvents() {
      return events;
    }

    @Override
    public void handleServerEvent(final ServerEvent event) {
      System.out.println(getClass().getSimpleName() + " has received a new message: " + event);
      events.add(event);
    }
  }

  private interface Consumer {
    List<ServerEvent> getEvents();
  }
}
