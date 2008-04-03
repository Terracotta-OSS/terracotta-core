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
import com.tc.config.schema.L2ConfigForL1.L2Data;
import com.tc.config.schema.defaults.DefaultValueProvider;
import com.tc.config.schema.dynamic.FileConfigItem;
import com.tc.config.schema.dynamic.ObjectArrayConfigItem;
import com.tc.config.schema.repository.ChildBeanFetcher;
import com.tc.config.schema.repository.ChildBeanRepository;
import com.tc.config.schema.utils.XmlObjectComparator;
import com.tc.logging.CustomerLogging;
import com.tc.logging.LogLevel;
import com.tc.logging.TCLogging;
import com.tc.object.config.schema.NewL1DSOConfig;
import com.tc.object.config.schema.NewL1DSOConfigObject;
import com.tc.util.Assert;
import com.terracottatech.config.Client;
import com.terracottatech.config.DsoClientData;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

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
    l2ConfigForL1 = new L2ConfigForL1Object(createContext(serversBeanRepository(), null),
                                            createContext(systemBeanRepository(), null));
    dsoL1Config = new NewL1DSOConfigObject(createContext(new ChildBeanRepository(clientBeanRepository(),
                                                                                 DsoClientData.class,
                                                                                 new ChildBeanFetcher() {
                                                                                   public XmlObject getChild(
                                                                                                             XmlObject parent) {
                                                                                     return ((Client) parent).getDso();
                                                                                   }
                                                                                 }), null));

  }

  public void setupLogging() {
    FileConfigItem logsPath = commonL1Config().logsPath();
    TCLogging.setLogDirectory(logsPath.getFile(), TCLogging.PROCESS_TYPE_L1);
    logsPath.addListener(new LogSettingConfigItemListener(TCLogging.PROCESS_TYPE_L1));
  }

  public String rawConfigText() {
    return configurationCreator.rawConfigText();
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

  public InputStream getL1PropertiesFromL2Stream() throws Exception{
    ObjectArrayConfigItem l2Data = this.l2ConfigForL1.l2Data();
    Object l2Objects[] = l2Data.getObjects();
    URLConnection connection = null;
    InputStream l1PropFromL2Stream = null;
    int numberOfL2Servers = l2Objects.length;
    URL theURL = null;
    L2Data l2data = null;
    for (int i = 0; i < numberOfL2Servers; i++) {
      try{
        l2data = (L2Data) l2Objects[i];
        theURL = new URL("http", l2data.host(), l2data.dsoPort(), "/l1reconnectproperties");
        CustomerLogging.getConsoleLogger().log(LogLevel.INFO,
                                               "Trying to get L1 Reconnect Properties from " + theURL.toString());
        connection = theURL.openConnection();
        l1PropFromL2Stream = connection.getInputStream();
        if (l1PropFromL2Stream != null) return l1PropFromL2Stream;
        
      }catch(IOException e){
        String text = "We couldn't load l1 reconnect properties from the " + theURL.toString();
        text += "; this error is permanent, so this source will not be retried.";
        
        if (i < numberOfL2Servers) text += " Skipping this source and going to the next one.";

        CustomerLogging.getConsoleLogger().warn(text);
        throw e;
      }
    }
    return null;
  }
}
