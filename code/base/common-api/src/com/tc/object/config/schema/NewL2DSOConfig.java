/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.config.schema;

import com.tc.config.schema.NewConfig;
import com.tc.config.schema.dynamic.BindPortConfigItem;
import com.tc.config.schema.dynamic.BooleanConfigItem;
import com.tc.config.schema.dynamic.ConfigItem;
import com.tc.config.schema.dynamic.IntConfigItem;
import com.tc.config.schema.dynamic.StringConfigItem;

/**
 * Represents all configuration read by the DSO L2 and which is independent of application.
 */
public interface NewL2DSOConfig extends NewConfig {

  public static final String OBJECTDB_DIRNAME                      = "objectdb";
  public static final String DIRTY_OBJECTDB_BACKUP_DIRNAME         = "dirty-objectdb-backup";
  public static final String DIRTY_OBJECTDB_BACKUP_PREFIX          = "dirty-objectdb-";
  public static final short  DEFAULT_GROUPPORT_OFFSET_FROM_DSOPORT = 20;

  ConfigItem persistenceMode();

  BooleanConfigItem garbageCollectionEnabled();

  BooleanConfigItem garbageCollectionVerbose();

  IntConfigItem garbageCollectionInterval();

  BindPortConfigItem dsoPort();
  
  BindPortConfigItem l2GroupPort();
  
  StringConfigItem host();
  
  StringConfigItem serverName();

  IntConfigItem clientReconnectWindow();

  StringConfigItem bind();
}
