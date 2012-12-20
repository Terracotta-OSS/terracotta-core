/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.config.schema;

import com.tc.config.schema.Config;
import com.terracottatech.config.BindPort;
import com.terracottatech.config.GarbageCollection;
import com.terracottatech.config.Offheap;
import com.terracottatech.config.Restartable;
import com.terracottatech.config.Security;

/**
 * Represents all configuration read by the DSO L2 and which is independent of application.
 */
public interface L2DSOConfig extends Config {

  public static final String OBJECTDB_DIRNAME                      = "objectdb";
  public static final String DIRTY_OBJECTDB_BACKUP_DIRNAME         = "dirty-objectdb-backup";
  public static final String DIRTY_OBJECTDB_BACKUP_PREFIX          = "dirty-objectdb-";

  GarbageCollection garbageCollection();

  BindPort tsaPort();

  BindPort tsaGroupPort();

  String host();

  String serverName();

  int clientReconnectWindow();

  String bind();

  Offheap offHeapConfig();

  Security securityConfig();

  Offheap getOffheap();

  Restartable getRestartable();
}
