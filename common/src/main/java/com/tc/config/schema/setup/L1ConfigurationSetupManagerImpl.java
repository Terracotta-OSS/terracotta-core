/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema.setup;

import org.apache.xmlbeans.XmlObject;

import com.tc.config.TcProperty;
import com.tc.config.schema.CommonL1Config;
import com.tc.config.schema.CommonL1ConfigObject;
import com.tc.config.schema.ConfigTCProperties;
import com.tc.config.schema.ConfigTCPropertiesFromObject;
import com.tc.config.schema.IllegalConfigurationChangeHandler;
import com.tc.config.schema.L2ConfigForL1;
import com.tc.config.schema.L2ConfigForL1Object;
import com.tc.config.schema.defaults.DefaultValueProvider;
import com.tc.config.schema.repository.ChildBeanFetcher;
import com.tc.config.schema.repository.ChildBeanRepository;
import com.tc.config.schema.utils.XmlObjectComparator;
import com.tc.logging.TCLogging;
import com.tc.object.config.schema.L1DSOConfig;
import com.tc.object.config.schema.L1DSOConfigObject;
import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.Assert;
import com.terracottatech.config.Client;
import com.terracottatech.config.DsoClientData;
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
  private final ConfigTCProperties configTCProperties;
  private final boolean            loadedFromTrustedSource;

  public L1ConfigurationSetupManagerImpl(ConfigurationCreator configurationCreator,
                                         DefaultValueProvider defaultValueProvider,
                                         XmlObjectComparator xmlObjectComparator,
                                         IllegalConfigurationChangeHandler illegalConfigChangeHandler)
      throws ConfigurationSetupException {
    super(configurationCreator, defaultValueProvider, xmlObjectComparator, illegalConfigChangeHandler);

    Assert.assertNotNull(configurationCreator);

    runConfigurationCreator(true);
    loadedFromTrustedSource = configurationCreator().loadedFromTrustedSource();

    commonL1Config = new CommonL1ConfigObject(createContext(clientBeanRepository(), null));
    configTCProperties = new ConfigTCPropertiesFromObject((TcProperties) tcPropertiesRepository().bean());
    dsoL1Config = new L1DSOConfigObject(createContext(new ChildBeanRepository(clientBeanRepository(),
                                                                              DsoClientData.class,
                                                                              new ChildBeanFetcher() {
                                                                                public XmlObject getChild(
                                                                                                          XmlObject parent) {
                                                                                  return ((Client) parent).getDso();
                                                                                }
                                                                              }), null));

    overwriteTcPropertiesFromConfig();
  }

  public void setupLogging() {
    File logsPath = commonL1Config().logsPath();
    TCLogging.setLogDirectory(logsPath, TCLogging.PROCESS_TYPE_L1);
  }

  public String rawConfigText() {
    return configurationCreator().rawConfigText();
  }

  public boolean loadedFromTrustedSource() {
    return this.loadedFromTrustedSource;
  }

  public L2ConfigForL1 l2Config() {
    return new L2ConfigForL1Object(createContext(serversBeanRepository(), null), createContext(systemBeanRepository(),
                                                                                               null));
  }

  public CommonL1Config commonL1Config() {
    return this.commonL1Config;
  }

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

  public void reloadServersConfiguration() throws ConfigurationSetupException {
    configurationCreator().reloadServersConfiguration(serversBeanRepository(), true, false);
  }
}
