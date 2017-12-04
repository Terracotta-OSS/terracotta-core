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


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tc.config.schema.CommonL1Config;
import com.tc.config.schema.setup.L1ConfigurationSetupManager;
import com.tc.properties.ReconnectConfig;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;

public class ClientConfigImpl implements ClientConfig {

  private static final Logger logger = LoggerFactory.getLogger(ClientConfigImpl.class);

  private final L1ConfigurationSetupManager                      configSetupManager;
  private final ReconnectConfig                                        l1ReconnectConfig           = null;
  private final long                                      CONFIGURATION_TOTAL_TIMEOUT;
  
  public ClientConfigImpl(boolean initializedModulesOnlyOnce, L1ConfigurationSetupManager configSetupManager) {
    this(configSetupManager);
  }

  public ClientConfigImpl(L1ConfigurationSetupManager configSetupManager) {
    this.configSetupManager = configSetupManager;
    TCPropertiesImpl.getProperties().overwriteTcPropertiesFromConfig(configSetupManager.getOverrideTCProperties());
    CONFIGURATION_TOTAL_TIMEOUT = TCPropertiesImpl
                                         .getProperties()
                                         .getLong(TCPropertiesConsts.TC_CONFIG_TOTAL_TIMEOUT);
  }

  @Override
  public String rawConfigText() {
    return configSetupManager.rawConfigText();
  }

  @Override
  public CommonL1Config getCommonL1Config() {
    return configSetupManager.commonL1Config();
  }

  @Override
  public String toString() {
    return "<ClientConfigImpl: " + configSetupManager + ">";
  }

}
