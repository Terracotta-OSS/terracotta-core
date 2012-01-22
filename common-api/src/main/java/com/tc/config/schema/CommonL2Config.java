/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema;

import com.terracottatech.config.BindPort;

import java.io.File;

/**
 * Contains methods exposing DSO L2 config.
 */
public interface CommonL2Config extends Config, StatisticsConfig {

  public static final short DEFAULT_JMXPORT_OFFSET_FROM_DSOPORT = 10;
  public static final int   MIN_PORTNUMBER                      = 0x0FFF;
  public static final int   MAX_PORTNUMBER                      = 0xFFFF;

  File dataPath();

  File logsPath();

  File serverDbBackupPath();

  File indexPath();

  BindPort jmxPort();

  String host();

  boolean authentication();

  String authenticationPasswordFile();

  String authenticationAccessFile();

  String authenticationLoginConfigName();

  boolean httpAuthentication();

  String httpAuthenticationUserRealmFile();
}
