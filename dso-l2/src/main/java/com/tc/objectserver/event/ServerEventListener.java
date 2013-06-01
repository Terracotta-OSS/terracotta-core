/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */

package com.tc.objectserver.event;

import com.google.common.eventbus.Subscribe;
import com.tc.server.ServerEvent;

/**
 * Generic listener interface for server events.
 *
 * @author Eugene Shelestovich
 * @see com.tc.server.ServerEvent
 */
public interface ServerEventListener {

  @Subscribe
  void handleServerEvent(ServerEvent event);
}
