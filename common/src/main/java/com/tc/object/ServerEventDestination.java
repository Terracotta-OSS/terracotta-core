package com.tc.object;

/**
 * @author Eugene Shelestovich
 */
public interface ServerEventDestination {
  String getDestinationName();
  void handleServerEvent(ServerEventType type, Object key);
}
