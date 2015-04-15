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

import com.tc.config.schema.beanfactory.ConfigBeanFactory;
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
    super(TCLogging.getLogger(TestConfigurationCreator.class), configurationSpec, beanFactory, null);
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
                                                  MutableBeanRepository tcPropertiesRepository, boolean isClient)
      throws ConfigurationSetupException {
    loadConfigAndSetIntoRepositories(l1BeanRepository, l2sBeanRepository, systemBeanRepository, tcPropertiesRepository,
                                     isClient);
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
