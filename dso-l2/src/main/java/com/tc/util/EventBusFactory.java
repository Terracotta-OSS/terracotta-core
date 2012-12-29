/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

import com.google.common.eventbus.EventBus;

/**
 * A signleton to hold application-wide {@link EventBus} instance.
 *
 * @author Eugene Shelestovich
 */
public final class EventBusFactory {

  private EventBusFactory() {}

  private static final class EventBusHolder {
    private static final EventBus eventBus = new EventBus("app-bus");
  }

  public static EventBus getEventBus() {
    return EventBusHolder.eventBus;
  }
}
