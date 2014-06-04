/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.service.events;

import com.terracotta.management.resource.events.TopologyEventEntityV2;

/**
 * An interface for registering topology event listeners.

 * @author Ludovic Orban
 */
public interface TopologyEventServiceV2 {

  interface TopologyEventListener {
    void onEvent(TopologyEventEntityV2 eventEntity);
  }

  void registerTopologyEventListener(TopologyEventListener listener);

  void unregisterTopologyEventListener(TopologyEventListener listener);

}
