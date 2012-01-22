/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema.setup;

import com.tc.config.schema.CommonL1Config;
import com.tc.config.schema.L2ConfigForL1;
import com.tc.object.config.schema.DSOApplicationConfig;
import com.tc.object.config.schema.L1DSOConfig;

/**
 * Knows how to set up configuration for L1.
 */
public interface L1ConfigurationSetupManager {
  String[] processArguments();

  boolean loadedFromTrustedSource();

  String rawConfigText();

  CommonL1Config commonL1Config();

  L2ConfigForL1 l2Config();

  L1DSOConfig dsoL1Config();

  void setupLogging();

  String[] applicationNames();

  DSOApplicationConfig dsoApplicationConfigFor(String applicationName);

  void reloadServersConfiguration() throws ConfigurationSetupException;
}
