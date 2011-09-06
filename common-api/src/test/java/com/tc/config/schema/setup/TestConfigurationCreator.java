/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.schema.setup;

import com.tc.config.schema.beanfactory.ConfigBeanFactory;
import com.tc.config.schema.repository.ApplicationsRepository;
import com.tc.config.schema.repository.MutableBeanRepository;
import com.tc.config.schema.setup.sources.ConfigurationSource;
import com.tc.logging.TCLogging;

/**
 * A {@link ConfigurationCreator} that creates config appropriate for tests only.
 */
public class TestConfigurationCreator extends StandardXMLFileConfigurationCreator {

  private final boolean trustedSource;

  public TestConfigurationCreator(final ConfigurationSpec configurationSpec, final ConfigBeanFactory beanFactory,
                                  boolean trustedSource) {
    super(TCLogging.getLogger(TestConfigurationCreator.class), configurationSpec, beanFactory);
    this.trustedSource = trustedSource;
  }

  @Override
  protected ConfigurationSource[] getConfigurationSources(String configrationSpec) {
    ConfigurationSource[] out = new ConfigurationSource[1];
    out[0] = new TestConfigurationSource();
    return out;
  }

  @Override
  public void createConfigurationIntoRepositories(MutableBeanRepository l1BeanRepository,
                                                  MutableBeanRepository l2sBeanRepository,
                                                  MutableBeanRepository systemBeanRepository,
                                                  MutableBeanRepository tcPropertiesRepository,
                                                  ApplicationsRepository applicationsRepository, boolean isClient)
      throws ConfigurationSetupException {
    loadConfigAndSetIntoRepositories(l1BeanRepository, l2sBeanRepository, systemBeanRepository, tcPropertiesRepository,
                                     applicationsRepository, isClient);
  }

  @Override
  public String describeSources() {
    return "Dynamically-generated configuration for tests";
  }

  @Override
  public boolean loadedFromTrustedSource() {
    return this.trustedSource;
  }

  @Override
  public String reloadServersConfiguration(MutableBeanRepository l2sBeanRepository, boolean b, boolean reportToConsole) {
    throw new UnsupportedOperationException();
  }

}
