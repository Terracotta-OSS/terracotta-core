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
package com.tc.config.schema.setup;

import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.util.Assert;
import org.terracotta.config.Servers;
import org.terracotta.config.TcConfiguration;
import org.terracotta.config.TcProperties;

/**
 * A base class for all TVS configuration setup managers.
 */
public class BaseConfigurationSetupManager {
  private final String[] args;
  private final ConfigurationCreator configurationCreator;
  private TcConfiguration conf;

  public BaseConfigurationSetupManager(ConfigurationCreator configurationCreator) {
    this(null, configurationCreator);
  }

  public BaseConfigurationSetupManager(String[] args, ConfigurationCreator configurationCreator) {
    Assert.assertNotNull(configurationCreator);

    this.args = args;
    this.configurationCreator = configurationCreator;
  }

  public String[] processArguments() {
    return args;
  }

  public final Servers serversBeanRepository() {
    return this.conf.getPlatformConfiguration().getServers();
  }

  protected final TcProperties tcPropertiesRepository() {
    return this.conf.getPlatformConfiguration().getTcProperties();
  }

  protected final ConfigurationCreator configurationCreator() {
    return this.configurationCreator;
  }

  protected final TcConfiguration tcConfigurationRepository() {
    return this.conf;
  }

  protected final void runConfigurationCreator(ClassLoader loader) throws ConfigurationSetupException {
    try {
      this.configurationCreator.createConfiguration(loader);
    } catch (Throwable t) {
      throw new ConfigurationSetupException(t);
    }
    this.conf = configurationCreator.getParsedConfiguration();
  }

}
