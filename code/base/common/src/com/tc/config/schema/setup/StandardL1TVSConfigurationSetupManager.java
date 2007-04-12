/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.schema.setup;

import org.apache.xmlbeans.XmlObject;

import com.tc.config.schema.IllegalConfigurationChangeHandler;
import com.tc.config.schema.L2ConfigForL1;
import com.tc.config.schema.L2ConfigForL1Object;
import com.tc.config.schema.NewCommonL1Config;
import com.tc.config.schema.NewCommonL1ConfigObject;
import com.tc.config.schema.defaults.DefaultValueProvider;
import com.tc.config.schema.dynamic.FileConfigItem;
import com.tc.config.schema.repository.ChildBeanFetcher;
import com.tc.config.schema.repository.ChildBeanRepository;
import com.tc.config.schema.utils.XmlObjectComparator;
import com.tc.logging.TCLogging;
import com.tc.object.config.schema.NewL1DSOConfig;
import com.tc.object.config.schema.NewL1DSOConfigObject;
import com.tc.util.Assert;
import com.terracottatech.config.Client;
import com.terracottatech.config.DsoClientData;

/**
 * The standard implementation of {@link com.tc.config.schema.setup.L1TVSConfigurationSetupManager}.
 */
public class StandardL1TVSConfigurationSetupManager extends BaseTVSConfigurationSetupManager implements
    L1TVSConfigurationSetupManager {
  private final ConfigurationCreator configurationCreator;
  private final NewCommonL1Config    commonL1Config;
  private final L2ConfigForL1        l2ConfigForL1;
  private final NewL1DSOConfig       dsoL1Config;
  private boolean                    loadedFromTrustedSource;

  public StandardL1TVSConfigurationSetupManager(ConfigurationCreator configurationCreator,
                                                DefaultValueProvider defaultValueProvider,
                                                XmlObjectComparator xmlObjectComparator,
                                                IllegalConfigurationChangeHandler illegalConfigChangeHandler)
      throws ConfigurationSetupException {
    super(defaultValueProvider, xmlObjectComparator, illegalConfigChangeHandler);

    Assert.assertNotNull(configurationCreator);

    this.configurationCreator = configurationCreator;
    runConfigurationCreator(this.configurationCreator);
    loadedFromTrustedSource = this.configurationCreator.loadedFromTrustedSource();

    commonL1Config = new NewCommonL1ConfigObject(createContext(clientBeanRepository(), null));
    l2ConfigForL1 = new L2ConfigForL1Object(createContext(serversBeanRepository(), null), createContext(
        systemBeanRepository(), null));
    dsoL1Config = new NewL1DSOConfigObject(createContext(new ChildBeanRepository(clientBeanRepository(),
        DsoClientData.class, new ChildBeanFetcher() {
          public XmlObject getChild(XmlObject parent) {
            return ((Client) parent).getDso();
          }
        }), null));

  }

  public void setupLogging() {
    FileConfigItem logsPath = commonL1Config().logsPath();
    TCLogging.setLogDirectory(logsPath.getFile(), TCLogging.PROCESS_TYPE_L1);
    logsPath.addListener(new LogSettingConfigItemListener(TCLogging.PROCESS_TYPE_L1));
  }

  public boolean loadedFromTrustedSource() {
    return this.loadedFromTrustedSource;
  }

  public L2ConfigForL1 l2Config() {
    return this.l2ConfigForL1;
  }

  public NewCommonL1Config commonL1Config() {
    return this.commonL1Config;
  }

  public NewL1DSOConfig dsoL1Config() {
    return this.dsoL1Config;
  }
}
