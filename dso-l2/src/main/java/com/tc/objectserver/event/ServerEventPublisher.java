/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */

package com.tc.objectserver.event;

import com.google.common.eventbus.EventBus;
import com.tc.server.ServerEvent;

import java.util.Arrays;
import java.util.Collection;

/**
 * Dispatcher component responsible for subscription, posting and rounting events.
 *
 * @author Eugene Shelestovich
 */
public final class ServerEventPublisher {

  private final EventBus bus;

  public ServerEventPublisher(final EventBus bus) {
    this.bus = bus;
  }

  public void post(final ServerEvent event) {
    bus.post(event);
  }

  public void post(final ServerEvent... events) {
    post(Arrays.asList(events));
  }

  public void post(final Collection<ServerEvent> events) {
    for (ServerEvent event : events) {
      post(event);
    }
  }

  public void register(final ServerEventListener listener) {
    bus.register(listener);
  }

  public void unregister(final ServerEventListener listener) {
    bus.unregister(listener);
  }
}
