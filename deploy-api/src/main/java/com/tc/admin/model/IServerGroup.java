/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
