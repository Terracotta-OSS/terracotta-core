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
package com.tc.config.schema.setup;

import com.tc.net.core.SecurityInfo;

/**
 * An object that knows how to make TVS configuration setup managers.
 */
public interface ConfigurationSetupManagerFactory {

  public static final String DEFAULT_APPLICATION_NAME         = "default";
  public static final String CONFIG_FILE_PROPERTY_NAME        = "tc.config";
  public static final String SERVER_CONFIG_FILE_PROPERTY_NAME = "tc.server.topology";

  L1ConfigurationSetupManager getL1TVSConfigurationSetupManager(SecurityInfo securityInfo) throws ConfigurationSetupException;

  L1ConfigurationSetupManager getL1TVSConfigurationSetupManager() throws ConfigurationSetupException;

  /**
   * @param l2Name The name of the L2 we should create configuration for. Normally you should pass <code>null</code>,
   *        which lets the configuration system work it out itself (usually from a system property), but, especially for
   *        tests, sometimes you need to specifically control this. (Because system properties are global, if you're
   *        starting more than one L2 in a single VM, it's hard or impossible to accurately set which L2 is being used
   *        that way.)
   */
  L2ConfigurationSetupManager createL2TVSConfigurationSetupManager(String l2Name, boolean setupLogging)
      throws ConfigurationSetupException;

}
