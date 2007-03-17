/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.schema.setup;

import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;

import com.tc.capabilities.AbstractCapabilitiesFactory;
import com.tc.capabilities.Capabilities;
import com.tc.config.schema.IllegalConfigurationChangeHandler;
import com.tc.config.schema.NewCommonL2Config;
import com.tc.config.schema.NewCommonL2ConfigObject;
import com.tc.config.schema.NewSystemConfig;
import com.tc.config.schema.NewSystemConfigObject;
import com.tc.config.schema.defaults.DefaultValueProvider;
import com.tc.config.schema.repository.ChildBeanFetcher;
import com.tc.config.schema.repository.ChildBeanRepository;
import com.tc.config.schema.utils.XmlObjectComparator;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.config.schema.NewL2DSOConfig;
import com.tc.object.config.schema.NewL2DSOConfigObject;
import com.tc.object.config.schema.PersistenceMode;
import com.tc.util.Assert;
import com.terracottatech.config.Application;
import com.terracottatech.config.Client;
import com.terracottatech.config.Server;
import com.terracottatech.config.Servers;
import com.terracottatech.config.System;
import com.terracottatech.config.TcConfigDocument;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The standard implementation of {@link com.tc.config.schema.setup.L2TVSConfigurationSetupManager}.
 */
public class StandardL2TVSConfigurationSetupManager extends BaseTVSConfigurationSetupManager implements
    L2TVSConfigurationSetupManager {

  private static TCLogger            logger = TCLogging.getLogger(StandardL2TVSConfigurationSetupManager.class);

  private final ConfigurationCreator configurationCreator;

  private NewSystemConfig            systemConfig;
  private final Map                  l2ConfigData;

  private final String               thisL2Identifier;
  private L2ConfigData               myConfigData;

  public StandardL2TVSConfigurationSetupManager(ConfigurationCreator configurationCreator, String thisL2Identifier,
                                                DefaultValueProvider defaultValueProvider,
                                                XmlObjectComparator xmlObjectComparator,
                                                IllegalConfigurationChangeHandler illegalConfigChangeHandler)
      throws ConfigurationSetupException {
    super(defaultValueProvider, xmlObjectComparator, illegalConfigChangeHandler);

    Assert.assertNotNull(configurationCreator);
    Assert.assertNotNull(defaultValueProvider);
    Assert.assertNotNull(xmlObjectComparator);

    this.configurationCreator = configurationCreator;

    this.systemConfig = null;
    this.l2ConfigData = new HashMap();

    this.thisL2Identifier = thisL2Identifier;
    this.myConfigData = null;

    runConfigurationCreator(this.configurationCreator);

    selectL2((Servers) serversBeanRepository().bean(), "the set of L2s known to us");
    validateRestrictions();
  }

  private class L2ConfigData {
    private final String              name;
    private final ChildBeanRepository beanRepository;

    private final NewCommonL2Config   commonL2Config;
    private final NewL2DSOConfig      dsoL2Config;

    public L2ConfigData(String name) throws ConfigurationSetupException {
      this.name = name;
      findMyL2Bean(); // To get the exception in case things are screwed up

      this.beanRepository = new ChildBeanRepository(serversBeanRepository(), Server.class, new BeanFetcher());

      this.commonL2Config = new NewCommonL2ConfigObject(createContext(this.beanRepository, configurationCreator
          .directoryConfigurationLoadedFrom()));
      this.dsoL2Config = new NewL2DSOConfigObject(createContext(this.beanRepository, configurationCreator
          .directoryConfigurationLoadedFrom()));
    }

    public String name() {
      return this.name;
    }

    public NewCommonL2Config commonL2Config() {
      return this.commonL2Config;
    }

    public NewL2DSOConfig dsoL2Config() {
      return this.dsoL2Config;
    }

    public boolean explicitlySpecifiedInConfigFile() throws ConfigurationSetupException {
      return findMyL2Bean() != null;
    }

    private Server findMyL2Bean() throws ConfigurationSetupException {
      Servers servers = (Servers) serversBeanRepository().bean();
      Server[] l2Array = servers == null ? null : servers.getServerArray();

      if (l2Array == null || l2Array.length == 0) return null;
      else if (this.name == null) {
        if (l2Array.length > 1) {
          // formatting
          throw new ConfigurationSetupException("You have not specified a name for your L2, and there are "
              + l2Array.length + " L2s defined in the configuration file. " + "You must indicate which L2 this is.");
        } else {
          return l2Array[0];
        }
      } else {
        for (int i = 0; i < l2Array.length; ++i) {
          if (this.name.trim().equalsIgnoreCase(l2Array[i].getName().trim())) { return l2Array[i]; }
        }
      }

      return null;
    }

    private class BeanFetcher implements ChildBeanFetcher {
      public XmlObject getChild(XmlObject parent) {
        try {
          return findMyL2Bean();
        } catch (ConfigurationSetupException cse) {
          logger.warn("Unable to find L2 bean for L2 '" + name + "'", cse);
          return null;
        }
      }
    }
  }

  public String describeSources() {
    return this.configurationCreator.describeSources();
  }

  private synchronized L2ConfigData configDataFor(String name) throws ConfigurationSetupException {
    L2ConfigData out = (L2ConfigData) this.l2ConfigData.get(name);

    if (out == null) {
      out = new L2ConfigData(name);

      Servers servers = (Servers) this.serversBeanRepository().bean();
      String list = "[data unavailable]";

      if (servers != null) {
        Server[] serverList = servers.getServerArray();

        if (serverList != null) {
          list = "";

          for (int i = 0; i < serverList.length; ++i) {
            if (i > 0) list += ", ";
            if (i == serverList.length - 1) list += "and ";
            list += "'" + serverList[i].getName() + "'";
          }
        }
      }

      if ((!out.explicitlySpecifiedInConfigFile()) && name != null) {
        // formatting
        throw new ConfigurationSetupException("Multiple <server> elements are defined in the configuration file. "
            + "As such, each server that you start needs to know which configuration " + "it should use.\n\n"
            + "However, this server couldn't figure out which one it is -- it thinks it's " + "called '" + name
            + "' (which, by default, is the host name of this machine), but you've only "
            + "created <server> elements in the config file called " + list
            + ".\n\nPlease re-start the server with a '-n <name>' argument on the command line to tell this "
            + "server which one it is, or change the 'name' attributes of the <server> "
            + "elements in the config file as appropriate.");
      }

      this.l2ConfigData.put(name, out);
    }

    return out;
  }

  private void selectL2(Servers servers, final String description) throws ConfigurationSetupException {
    this.systemConfig = new NewSystemConfigObject(createContext(systemBeanRepository(), configurationCreator
        .directoryConfigurationLoadedFrom()));

    if (this.allCurrentlyKnownServers().length == 1) {
      if (servers != null && servers.getServerArray() != null && servers.getServerArray()[0] != null) {
        this.myConfigData = configDataFor(servers.getServerArray()[0].getName());
      } else {
        this.myConfigData = configDataFor(null);
      }
    } else this.myConfigData = configDataFor(this.thisL2Identifier);

    LogSettingConfigItemListener listener = new LogSettingConfigItemListener(TCLogging.PROCESS_TYPE_L2);
    this.myConfigData.commonL2Config().logsPath().addListener(listener);
    listener.valueChanged(null, this.myConfigData.commonL2Config().logsPath().getObject());
  }

  private void validateRestrictions() throws ConfigurationSetupException {
    validateLicenseModuleRestrictions();
    validateDSOClusterPersistenceMode();
  }

  private void validateDSOClusterPersistenceMode() throws ConfigurationSetupException {
    if (super.serversBeanRepository().bean() != null) {
      Server[] servers = ((Servers) super.serversBeanRepository().bean()).getServerArray();
      Set badServers = new HashSet();

      if (servers != null && servers.length > 1) {
        Capabilities capabilities = AbstractCapabilitiesFactory.getCapabilitiesManager();

        if (!capabilities.hasHA() && capabilities.canClusterPOJOs()) { throw new ConfigurationSetupException(
            "Attempting to run multiple servers without license " + "authorization of DSO High Availability."); }

        // We have clustered DSO; they must all be in permanent-store mode
        for (int i = 0; i < servers.length; ++i) {
          String name = servers[i].getName();
          L2ConfigData data = configDataFor(name);

          Assert.assertNotNull(data);
          if (!capabilities.hasHAOverNetwork()
              && !(data.dsoL2Config().persistenceMode().getObject().equals(PersistenceMode.PERMANENT_STORE))) {
            badServers.add(name);
          }
        }
      }

      if (badServers.size() > 0) {
        // formatting
        throw new ConfigurationSetupException("Your Terracotta system has a clustered DSO configuration -- i.e., "
            + "DSO is enabled, and more than one server is defined in the configuration file -- but "
            + "at least one server is in the '" + PersistenceMode.TEMPORARY_SWAP_ONLY
            + "' persistence mode. (Servers in this mode: " + badServers + ".) In a "
            + "clustered DSO configuration, all servers must be in the '" + PersistenceMode.PERMANENT_STORE
            + "' mode. Please adjust the " + "persistence/mode element (inside the 'server' element) in your "
            + "configuration file; see the Terracotta documentation for more details.");
      }
    }
  }

  private void validateLicenseModuleRestrictions() throws ConfigurationSetupException {
    Capabilities capabilities = AbstractCapabilitiesFactory.getCapabilitiesManager();

    if (!capabilities.canClusterPOJOs()) {
      Object result = this.dsoApplicationConfigFor(TVSConfigurationSetupManagerFactory.DEFAULT_APPLICATION_NAME)
          .roots().getObject();
      if (result != null && Array.getLength(result) > 0) {
        // formatting
        throw new ConfigurationSetupException("Your Terracotta license, " + capabilities.describe()
            + ", does not allow you to define DSO roots in your configuration file. Please remove them and try again.");
      }
    }

  }

  public NewCommonL2Config commonL2ConfigFor(String name) throws ConfigurationSetupException {
    return configDataFor(name).commonL2Config();
  }

  public NewCommonL2Config commonl2Config() {
    return this.myConfigData.commonL2Config();
  }

  public NewSystemConfig systemConfig() {
    return this.systemConfig;
  }

  public NewL2DSOConfig dsoL2ConfigFor(String name) throws ConfigurationSetupException {
    return configDataFor(name).dsoL2Config();
  }

  public NewL2DSOConfig dsoL2Config() {
    return this.myConfigData.dsoL2Config();
  }

  public String[] allCurrentlyKnownServers() {
    Servers serversBean = (Servers) serversBeanRepository().bean();
    Server[] l2s = serversBean == null ? null : serversBean.getServerArray();
    if (l2s == null || l2s.length == 0) return new String[] { null };
    else {
      String[] out = new String[l2s.length];
      for (int i = 0; i < l2s.length; ++i)
        out[i] = l2s[i].getName();
      return out;
    }
  }

  public InputStream rawConfigFile() {
    // This MUST piece together the configuration from our currently-active bean repositories. If we just read the
    // actual config file we got on startup, we'd be sending out, well, the config we got on startup -- which might be
    // quite different from our current config, if an L1 came in and overrode our config.

    TcConfigDocument doc = TcConfigDocument.Factory.newInstance();
    TcConfigDocument.TcConfig config = doc.addNewTcConfig();

    System system = (System) this.systemBeanRepository().bean();
    Client client = (Client) this.clientBeanRepository().bean();
    Servers servers = (Servers) this.serversBeanRepository().bean();
    Application application = (Application) this.applicationsRepository().repositoryFor(
        TVSConfigurationSetupManagerFactory.DEFAULT_APPLICATION_NAME).bean();

    if (system != null) config.setSystem(system);
    if (client != null) config.setClients(client);
    if (servers != null) config.setServers(servers);
    if (application != null) config.setApplication(application);

    StringWriter sw = new StringWriter();
    XmlOptions options = new XmlOptions().setSavePrettyPrint().setSavePrettyPrintIndent(4);

    try {
      doc.save(sw, options);
    } catch (IOException ioe) {
      throw Assert.failure("Unexpected failure writing to in-memory streams", ioe);
    }

    String text = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n\n" + sw.toString();

    try {
      return new ByteArrayInputStream(text.getBytes("UTF-8"));
    } catch (UnsupportedEncodingException uee) {
      throw Assert.failure("This shouldn't be possible", uee);
    }
  }

}
