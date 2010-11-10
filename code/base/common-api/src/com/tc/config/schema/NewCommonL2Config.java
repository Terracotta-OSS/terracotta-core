/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.schema;

import com.tc.config.schema.dynamic.BindPortConfigItem;
import com.tc.config.schema.dynamic.FileConfigItem;
import com.tc.config.schema.dynamic.StringConfigItem;

/**
 * Contains methods exposing DSO L2 config.
 */
public interface NewCommonL2Config extends NewConfig, NewStatisticsConfig {

  public static final short DEFAULT_JMXPORT_OFFSET_FROM_DSOPORT = 10;
  public static final int   MIN_PORTNUMBER                      = 0x0FFF;
  public static final int   MAX_PORTNUMBER                      = 0xFFFF;

  FileConfigItem dataPath();

  FileConfigItem logsPath();

  FileConfigItem serverDbBackupPath();

  FileConfigItem indexPath();

  BindPortConfigItem jmxPort();

  StringConfigItem host();

  boolean authentication();

  String authenticationPasswordFile();

  String authenticationAccessFile();

  String authenticationLoginConfigName();

  boolean httpAuthentication();

  String httpAuthenticationUserRealmFile();
}
