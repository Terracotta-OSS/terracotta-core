/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */

package com.tc.objectserver.interest;

import com.google.common.eventbus.EventBus;

import java.util.Arrays;
import java.util.Collection;

/**
 * A dispatcher component responsible for subscription, posting and rounting events.
 *
 * @author Eugene Shelestovich
 */
public final class InterestPublisher {

  private final EventBus bus;

  public InterestPublisher(final EventBus bus) {
    this.bus = bus;
  }

  public void post(final Interest interest) {
    bus.post(interest);
  }

  public void post(final Interest... interests) {
    post(Arrays.asList(interests));
  }

  public void post(final Collection<Interest> interests) {
    for (Interest interest : interests) {
      post(interest);
    }
  }

  public void register(final InterestListener listener) {
    bus.register(listener);
  }

  public void unregister(final InterestListener listener) {
    bus.unregister(listener);
  }
}
