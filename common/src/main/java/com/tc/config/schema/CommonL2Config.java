/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema;

import org.terracotta.config.BindPort;
import org.terracotta.config.TcConfiguration;

import java.io.File;

/**
 * Contains methods exposing DSO L2 config.
 */
public interface CommonL2Config extends Config<TcConfiguration> {

  File dataPath();

  File logsPath();

  File serverDbBackupPath();

  BindPort tsaPort();

  BindPort tsaGroupPort();

  BindPort managementPort();

  String host();

  boolean authentication();

  String authenticationPasswordFile();

  String authenticationAccessFile();

  String authenticationLoginConfigName();

  boolean httpAuthentication();

  String httpAuthenticationUserRealmFile();

  boolean isSecure();
}
