/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.model;

public interface IServerGroup extends IClusterModelElement {
  static final String         PROP_ACTIVE_SERVER = "activeServer";
  static final String         PROP_CONNECTED     = "connected";

  static final IServerGroup[] NULL_SET           = {};

  IClusterModel getClusterModel();

  IServer[] getMembers();

  String getName();

  int getId();

  boolean isCoordinator();

  void setConnectionCredentials(String[] creds);

  void clearConnectionCredentials();

  IServer getActiveServer();

  void addServerStateListener(ServerStateListener listener);

  void removeServerStateListener(ServerStateListener listener);

  void connect();

  void disconnect();

  boolean isConnected();

  void tearDown();

  String dump();
}
