/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */

package com.tc.objectserver.interest;

import org.junit.Test;

import com.google.common.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Eugene Shelestovich
 */
public class InterestPublisherTest {

  @Test
  public void testShouldPostEventsToListeners() {
    final String cacheName = "test-cache";
    final InterestPublisher publisher = new InterestPublisher(new EventBus("test-bus"));
    final InterestDumper consumer1 = new InterestDumper();
    final TypedInterestDumper consumer2 = new TypedInterestDumper();
    publisher.register(consumer1);
    publisher.register(consumer2);

    final Interest event1 = new PutInterest(1, new byte[] { 101 }, cacheName);
    final Interest event2 = new RemoveInterest(1, cacheName);
    final Interest event3 = new EvictionInterest(2, cacheName);

    publisher.post(event1, event2, event3);

    assertConsumer(consumer1);
    assertConsumer(consumer2);
  }

  private void assertConsumer(final Consumer consumer) {
    assertEquals(3, consumer.getEvents().size());
    assertTrue(consumer.getEvents().get(0) instanceof PutInterest);
    assertTrue(consumer.getEvents().get(1) instanceof RemoveInterest);
    assertTrue(consumer.getEvents().get(2) instanceof EvictionInterest);
  }

  private static final class InterestDumper implements InterestListener, Consumer {
    private final List<Interest> events = new ArrayList<Interest>();

    @Override
    public void onInterest(final Interest interest) {
      System.out.println(getClass().getSimpleName() + " has received a new message: " + interest);
      events.add(interest);
    }

    @Override
    public List<Interest> getEvents() {
      return events;
    }
  }

  private static final class TypedInterestDumper extends TypedInterestListenerSupport implements Consumer {
    private final List<Interest> events = new ArrayList<Interest>();

    private void dump(final Interest interest) {
      System.out.println(getClass().getSimpleName() + " has received a new message: " + interest);
      events.add(interest);
    }

    @Override
    public void onPut(final PutInterest interest) {
      dump(interest);
    }

    @Override
    public void onRemove(final RemoveInterest interest) {
      dump(interest);
    }

    @Override
    public void onEviction(final EvictionInterest interest) {
      dump(interest);
    }

    @Override
    public List<Interest> getEvents() {
      return events;
    }
  }

  private interface Consumer {
    List<Interest> getEvents();
  }
}
