/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.service.events;

import org.terracotta.management.resource.events.EventEntityV2;

/**
 * An interface for registering topology event listeners.

 * @author Ludovic Orban
 */
public interface EventServiceV2 {

  interface EventListener {
    void onEvent(EventEntityV2 eventEntity);
  }

  void registerTopologyEventListener(EventListener listener);

  void unregisterTopologyEventListener(EventListener listener);

}
