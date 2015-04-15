/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.object.config;

import com.tc.config.schema.CommonL1Config;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.L1ConfigurationSetupManager;
import com.tc.net.core.SecurityInfo;
import com.tc.object.Portability;
import com.tc.properties.ReconnectConfig;
import com.tc.security.PwProvider;

/**
 * Knows how to interpret the terracotta client config and tell you things like whether a class is portable. This
 * interface extends DSOApplicationConfig which is a much simpler interface suitable for manipulating the config from
 * the perspective of generating a configuration file.
 */
public interface DSOClientConfigHelper extends DSOMBeanConfig {
  String[] processArguments();

  String rawConfigText();

  Class getChangeApplicator(Class clazz);

  boolean addTunneledMBeanDomain(String tunneledMBeanDomain);

  TransparencyClassSpec getOrCreateSpec(String className, String applicator);

  TransparencyClassSpec getSpec(String className);

  int getFaultCount();

  void setFaultCount(int count);

  boolean isUseNonDefaultConstructor(Class clazz);

  CommonL1Config getNewCommonL1Config();

  Portability getPortability();

  void removeSpec(String className);

  public ReconnectConfig getL1ReconnectProperties(final PwProvider pwProvider) throws ConfigurationSetupException;

  public void validateGroupInfo(final PwProvider pwProvider) throws ConfigurationSetupException;

  public void validateClientServerCompatibility(final PwProvider pwProvider, SecurityInfo securityInfo)
      throws ConfigurationSetupException;

  L1ConfigurationSetupManager reloadServersConfiguration() throws ConfigurationSetupException;

  SecurityInfo getSecurityInfo();
}
