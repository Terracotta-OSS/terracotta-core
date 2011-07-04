/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.schema.setup;

import com.tc.config.schema.ActiveServerGroupConfig;
import com.tc.config.schema.ActiveServerGroupsConfig;
import com.tc.config.schema.CommonL2Config;
import com.tc.config.schema.HaConfigSchema;
import com.tc.config.schema.SystemConfig;
import com.tc.config.schema.UpdateCheckConfig;
import com.tc.object.config.schema.DSOApplicationConfig;
import com.tc.object.config.schema.L2DSOConfig;
import com.tc.server.ServerConnectionValidator;

import java.io.InputStream;

/**
 * Knows how to set up configuration for L2.
 */
public interface L2ConfigurationSetupManager {
  String[] processArguments();

  CommonL2Config commonl2Config();

  SystemConfig systemConfig();

  L2DSOConfig dsoL2Config();

  HaConfigSchema haConfig();

  UpdateCheckConfig updateCheckConfig();

  ActiveServerGroupsConfig activeServerGroupsConfig();

  ActiveServerGroupConfig getActiveServerGroupForThisL2();

  String[] applicationNames();

  DSOApplicationConfig dsoApplicationConfigFor(String applicationName);

  String describeSources();

  InputStream rawConfigFile();

  InputStream effectiveConfigFile();

  String[] allCurrentlyKnownServers();

  String getL2Identifier();

  CommonL2Config commonL2ConfigFor(String name) throws ConfigurationSetupException;

  L2DSOConfig dsoL2ConfigFor(String name) throws ConfigurationSetupException;

  TopologyReloadStatus reloadConfiguration(ServerConnectionValidator serverConnectionValidator)
      throws ConfigurationSetupException;
}
