/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
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
