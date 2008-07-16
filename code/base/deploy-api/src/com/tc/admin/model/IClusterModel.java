/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.model;

import java.util.Map;

public interface IClusterModel extends IServer {
  static final String PROP_ACTIVE_SERVER = "activeServer";

  void setHost(String host);

  void setPort(int port);

  void addServerStateListener(ServerStateListener listener);

  void removeServerStateListener(ServerStateListener listener);

  IServer getActiveServer();

  Map<IServer, Map<String, Object>> getPrimaryServerStatistics();

  Map<IClient, Map<String, Object>> getPrimaryClientStatistics();
}
