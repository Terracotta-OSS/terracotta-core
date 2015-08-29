/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.config.schema;

import org.terracotta.config.BindPort;

import com.tc.config.schema.Config;

/**
 * Represents all configuration read by the DSO L2 and which is independent of application.
 */
public interface L2Config extends Config {

  public static final String OBJECTDB_DIRNAME                      = "objectdb";
  public static final String DIRTY_OBJECTDB_BACKUP_DIRNAME         = "dirty-objectdb-backup";
  public static final String DIRTY_OBJECTDB_BACKUP_PREFIX          = "dirty-objectdb-";

  BindPort tsaPort();

  BindPort tsaGroupPort();

  BindPort managementPort();

  String host();

  String serverName();

  int clientReconnectWindow();

  String bind();

  boolean getRestartable();

  boolean isJmxEnabled();

  void setJmxEnabled(boolean b);
}
