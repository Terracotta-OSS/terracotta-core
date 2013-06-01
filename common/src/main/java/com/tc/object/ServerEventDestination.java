package com.tc.object;

import com.tc.server.ServerEventType;

/**
 * @author Eugene Shelestovich
 */
public interface ServerEventDestination {
  String getDestinationName();
  void handleServerEvent(ServerEventType type, Object key);
}
