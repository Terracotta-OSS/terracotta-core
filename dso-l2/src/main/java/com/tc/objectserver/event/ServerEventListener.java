/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */

package com.tc.objectserver.event;

import com.google.common.eventbus.Subscribe;

/**
 * Generic listener interface for server events.
 *
 * @author Eugene Shelestovich
 * @see ServerEvent
 */
public interface ServerEventListener {

  @Subscribe
  void handleServerEvent(ServerEvent event);
}
