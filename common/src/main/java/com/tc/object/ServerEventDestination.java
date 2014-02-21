package com.tc.object;

import com.tc.server.ServerEvent;

/**
 * @author Eugene Shelestovich
 */
public interface ServerEventDestination {

  String getDestinationName();

  void handleServerEvent(ServerEvent event);

  void resendEventRegistrations();
}
