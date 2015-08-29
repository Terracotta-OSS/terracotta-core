/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema.setup;

import com.tc.config.schema.CommonL1Config;
import com.tc.config.schema.L2ConfigForL1;
import com.tc.net.core.SecurityInfo;

/**
 * Knows how to set up configuration for L1.
 */
public interface L1ConfigurationSetupManager {
  String[] processArguments();

  boolean loadedFromTrustedSource();

  String rawConfigText();
  
  String source();

  CommonL1Config commonL1Config();

  L2ConfigForL1 l2Config();

  SecurityInfo getSecurityInfo();

  void setupLogging();

  void reloadServersConfiguration() throws ConfigurationSetupException;
}
