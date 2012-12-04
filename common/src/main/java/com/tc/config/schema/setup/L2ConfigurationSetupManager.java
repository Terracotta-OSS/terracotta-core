/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema.setup;

import com.tc.config.schema.ActiveServerGroupConfig;
import com.tc.config.schema.ActiveServerGroupsConfig;
import com.tc.config.schema.CommonL2Config;
import com.tc.config.schema.SecurityConfig;
import com.tc.config.schema.UpdateCheckConfig;
import com.tc.object.config.schema.L2DSOConfig;
import com.tc.operatorevent.TerracottaOperatorEventLogger;
import com.tc.server.ServerConnectionValidator;

import java.io.InputStream;

/**
 * Knows how to set up configuration for L2.
 */
public interface L2ConfigurationSetupManager {
  String[] processArguments();

  CommonL2Config commonl2Config();

  L2DSOConfig dsoL2Config();

  UpdateCheckConfig updateCheckConfig();

  ActiveServerGroupsConfig activeServerGroupsConfig();

  ActiveServerGroupConfig getActiveServerGroupForThisL2();

  String describeSources();

  InputStream rawConfigFile();

  InputStream effectiveConfigFile();

  String[] allCurrentlyKnownServers();

  String getL2Identifier();

  SecurityConfig getSecurity();

  CommonL2Config commonL2ConfigFor(String name) throws ConfigurationSetupException;

  L2DSOConfig dsoL2ConfigFor(String name) throws ConfigurationSetupException;

  TopologyReloadStatus reloadConfiguration(ServerConnectionValidator serverConnectionValidator,
                                           TerracottaOperatorEventLogger opeventlogger)
      throws ConfigurationSetupException;

  boolean isSecure();
}
