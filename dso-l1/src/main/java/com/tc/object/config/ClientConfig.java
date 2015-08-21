/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.config;

import com.tc.config.schema.CommonL1Config;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.L1ConfigurationSetupManager;
import com.tc.net.core.SecurityInfo;
import com.tc.properties.ReconnectConfig;
import com.tc.security.PwProvider;

public interface ClientConfig extends DSOMBeanConfig {
  
  String rawConfigText();

  boolean addTunneledMBeanDomain(String tunneledMBeanDomain);

  CommonL1Config getCommonL1Config();

  public ReconnectConfig getL1ReconnectProperties(PwProvider pwProvider) throws ConfigurationSetupException;

  public void validateClientServerCompatibility(PwProvider pwProvider, SecurityInfo securityInfo)
      throws ConfigurationSetupException;

  L1ConfigurationSetupManager reloadServersConfiguration() throws ConfigurationSetupException;

  SecurityInfo getSecurityInfo();
}
