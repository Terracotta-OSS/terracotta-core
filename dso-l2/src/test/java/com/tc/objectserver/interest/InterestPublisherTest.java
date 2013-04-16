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
    final InterestPublisher publisher = new InterestPublisher(new EventBus("testBus"));
    final InterestDumper consumer = new InterestDumper();
    publisher.register(consumer);

    final Interest event1 = new PutInterest(1, new byte[] { 101 });
    final Interest event2 = new RemoveInterest(1);
    final Interest event3 = new EvictionInterest(2);

    publisher.post(event1, event2, event3);

    assertEquals(3, consumer.events.size());
    assertTrue(consumer.events.get(0) instanceof PutInterest);
    assertTrue(consumer.events.get(1) instanceof RemoveInterest);
    assertTrue(consumer.events.get(2) instanceof EvictionInterest);
  }

  private static final class InterestDumper extends InterestListenerSupport {

    private final List<Interest> events = new ArrayList<Interest>();

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

    private void dump(final Interest interest) {
      System.out.println("A new message received: " + interest);
      events.add(interest);
    }
  }
}
