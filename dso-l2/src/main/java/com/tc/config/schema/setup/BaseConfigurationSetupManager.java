/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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

  protected final void runConfigurationCreator() throws ConfigurationSetupException {
    try {
      this.configurationCreator.createConfiguration();
    } catch (Throwable t) {
      throw new ConfigurationSetupException(t);
    }
    this.conf = configurationCreator.getParsedConfiguration();
  }

}
