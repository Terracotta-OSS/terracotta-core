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

import com.tc.config.TcProperty;
import com.tc.config.schema.CommonL1Config;
import com.tc.config.schema.CommonL1ConfigObject;
import com.tc.config.schema.ConfigTCProperties;
import com.tc.config.schema.ConfigTCPropertiesFromObject;
import com.tc.config.schema.IllegalConfigurationChangeHandler;
import com.tc.config.schema.L2ConfigForL1;
import com.tc.config.schema.L2ConfigForL1Object;
import com.tc.config.schema.defaults.DefaultValueProvider;
import com.tc.config.schema.utils.XmlObjectComparator;
import com.tc.logging.TCLogging;
import com.tc.net.core.SecurityInfo;
import com.tc.object.config.schema.L1DSOConfig;
import com.tc.object.config.schema.L1DSOConfigObject;
import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.Assert;
import com.terracottatech.config.TcProperties;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * The standard implementation of {@link com.tc.config.schema.setup.L1ConfigurationSetupManager}.
 */
public class L1ConfigurationSetupManagerImpl extends BaseConfigurationSetupManager implements
    L1ConfigurationSetupManager {
  private final CommonL1Config     commonL1Config;
  private final L1DSOConfig        dsoL1Config;
  private final SecurityInfo       securityInfo;
  private final ConfigTCProperties configTCProperties;
  private final boolean            loadedFromTrustedSource;

  public L1ConfigurationSetupManagerImpl(ConfigurationCreator configurationCreator,
                                         DefaultValueProvider defaultValueProvider,
                                         XmlObjectComparator xmlObjectComparator,
                                         IllegalConfigurationChangeHandler illegalConfigChangeHandler, final SecurityInfo securityInfo)
      throws ConfigurationSetupException {
    super(configurationCreator, defaultValueProvider, xmlObjectComparator, illegalConfigChangeHandler);

    Assert.assertNotNull(configurationCreator);
    this.securityInfo = securityInfo;

    runConfigurationCreator(true);
    loadedFromTrustedSource = configurationCreator().loadedFromTrustedSource();

    commonL1Config = new CommonL1ConfigObject(createContext(clientBeanRepository(), null));
    configTCProperties = new ConfigTCPropertiesFromObject((TcProperties) tcPropertiesRepository().bean());
    dsoL1Config = new L1DSOConfigObject(createContext(clientBeanRepository(), null));

    overwriteTcPropertiesFromConfig();
  }

  @Override
  public void setupLogging() {
    File logsPath = commonL1Config().logsPath();
    TCLogging.setLogDirectory(logsPath, TCLogging.PROCESS_TYPE_L1);
  }

  @Override
  public String rawConfigText() {
    return configurationCreator().rawConfigText();
  }

  @Override
  public boolean loadedFromTrustedSource() {
    return this.loadedFromTrustedSource;
  }

  @Override
  public L2ConfigForL1 l2Config() {
    return new L2ConfigForL1Object(createContext(serversBeanRepository(), null), createContext(systemBeanRepository(),
                                                                                               null));
  }

  @Override
  public CommonL1Config commonL1Config() {
    return this.commonL1Config;
  }

  @Override
  public L1DSOConfig dsoL1Config() {
    return this.dsoL1Config;
  }

  private void overwriteTcPropertiesFromConfig() {
    TCProperties tcProps = TCPropertiesImpl.getProperties();

    Map<String, String> propMap = new HashMap<String, String>();
    for (TcProperty tcp : this.configTCProperties.getTcPropertiesArray()) {
      propMap.put(tcp.getPropertyName().trim(), tcp.getPropertyValue().trim());
    }

    tcProps.overwriteTcPropertiesFromConfig(propMap);
  }

  @Override
  public void reloadServersConfiguration() throws ConfigurationSetupException {
    configurationCreator().reloadServersConfiguration(serversBeanRepository(), true, false);
  }

  @Override
  public SecurityInfo getSecurityInfo() {
    return this.securityInfo;
  }
}
