/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema.setup;

import org.apache.xmlbeans.XmlBoolean;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlInteger;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;

import com.tc.config.TcProperty;
import com.tc.config.schema.ActiveServerGroupConfig;
import com.tc.config.schema.ActiveServerGroupsConfig;
import com.tc.config.schema.ActiveServerGroupsConfigObject;
import com.tc.config.schema.CommonL2Config;
import com.tc.config.schema.CommonL2ConfigObject;
import com.tc.config.schema.ConfigTCProperties;
import com.tc.config.schema.ConfigTCPropertiesFromObject;
import com.tc.config.schema.IllegalConfigurationChangeHandler;
import com.tc.config.schema.SecurityConfig;
import com.tc.config.schema.SecurityConfigObject;
import com.tc.config.schema.UpdateCheckConfig;
import com.tc.config.schema.UpdateCheckConfigObject;
import com.tc.config.schema.defaults.DefaultValueProvider;
import com.tc.config.schema.repository.ChildBeanFetcher;
import com.tc.config.schema.repository.ChildBeanRepository;
import com.tc.config.schema.repository.MutableBeanRepository;
import com.tc.config.schema.repository.StandardBeanRepository;
import com.tc.config.schema.utils.XmlObjectComparator;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.management.TSAManagementEventPayload;
import com.tc.management.TerracottaRemoteManagement;
import com.tc.object.config.schema.L2DSOConfig;
import com.tc.object.config.schema.L2DSOConfigObject;
import com.tc.operatorevent.TerracottaOperatorEventFactory;
import com.tc.operatorevent.TerracottaOperatorEventLogger;
import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesImpl;
import com.tc.server.ServerConnectionValidator;
import com.tc.util.Assert;
import com.terracottatech.config.Client;
import com.terracottatech.config.Security;
import com.terracottatech.config.Server;
import com.terracottatech.config.Servers;
import com.terracottatech.config.TcConfigDocument;
import com.terracottatech.config.TcProperties;
import com.terracottatech.config.UpdateCheck;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The standard implementation of {@link com.tc.config.schema.setup.L2ConfigurationSetupManager}.
 */
public class L2ConfigurationSetupManagerImpl extends BaseConfigurationSetupManager implements
    L2ConfigurationSetupManager {

  private static final TCLogger             logger = TCLogging.getLogger(L2ConfigurationSetupManagerImpl.class);

  private final Map<String, L2ConfigData>   l2ConfigData;
  private final UpdateCheckConfig           updateCheckConfig;
  private final String                      thisL2Identifier;
  private final L2ConfigData                myConfigData;
  private final ConfigTCProperties          configTCProperties;
  private final Set<InetAddress>            localInetAddresses;

  private volatile ActiveServerGroupsConfig activeServerGroupsConfig;
  private volatile SecurityConfig           securityConfig;
  private volatile boolean                  secure;

  private final Servers                     serversBean;

  public L2ConfigurationSetupManagerImpl(ConfigurationCreator configurationCreator, String thisL2Identifier,
                                         DefaultValueProvider defaultValueProvider,
                                         XmlObjectComparator xmlObjectComparator,
                                         IllegalConfigurationChangeHandler illegalConfigChangeHandler,
                                         boolean setupLogging)
      throws ConfigurationSetupException {
    this(null, configurationCreator, thisL2Identifier, defaultValueProvider, xmlObjectComparator,
         illegalConfigChangeHandler, setupLogging);
  }

  public L2ConfigurationSetupManagerImpl(String[] args, ConfigurationCreator configurationCreator,
                                         String thisL2Identifier, DefaultValueProvider defaultValueProvider,
                                         XmlObjectComparator xmlObjectComparator,
                                         IllegalConfigurationChangeHandler illegalConfigChangeHandler,
                                         boolean setupLogging)
      throws ConfigurationSetupException {
    super(args, configurationCreator, defaultValueProvider, xmlObjectComparator, illegalConfigChangeHandler);

    Assert.assertNotNull(defaultValueProvider);
    Assert.assertNotNull(xmlObjectComparator);

    this.l2ConfigData = new HashMap<String, L2ConfigData>();

    this.localInetAddresses = getAllLocalInetAddresses();

    // this sets the beans in each repository
    runConfigurationCreator(false);
    this.configTCProperties = new ConfigTCPropertiesFromObject((TcProperties) tcPropertiesRepository().bean());
    overwriteTcPropertiesFromConfig();

    // do this after runConfigurationCreator method call, after serversBeanRepository is set
    try {
      this.updateCheckConfig = getUpdateCheckConfig();
    } catch (XmlException e2) {
      throw new ConfigurationSetupException(e2);
    }

    this.activeServerGroupsConfig = new ActiveServerGroupsConfigObject(
                                                                       createContext(serversBeanRepository(),
                                                                                     configurationCreator()
                                                                                         .directoryConfigurationLoadedFrom()),
                                                                       this);

    serversBean = (Servers) serversBeanRepository().bean();
    this.secure = serversBean != null && serversBean.getSecure();
    Server[] servers = serversBean != null ? L2DSOConfigObject.getServers(serversBean) : null;
    Server server = null;

    if (thisL2Identifier != null) {
      this.thisL2Identifier = thisL2Identifier;
      if (servers != null) {
        for (Server s : servers) {
          if (s.getName().equals(thisL2Identifier)) {
            server = s;
            break;
          }
        }
      }
    } else {
      server = autoChooseThisL2(servers);
      this.thisL2Identifier = (server != null ? server.getName() : null);
    }

    if (secure && server != null) {
      final Server s = server;
      ChildBeanRepository beanRepository = new ChildBeanRepository(serversBeanRepository(), Security.class,
                                                                   new ChildBeanFetcher() {
                                                                     @Override
                                                                     public XmlObject getChild(XmlObject parent) {
                                                                       return s.getSecurity();
                                                                     }
                                                                   });
      securityConfig = new SecurityConfigObject(createContext(beanRepository, getConfigFilePath()));
    } else {
      securityConfig = null;
    }

    verifyL2Identifier(servers, this.thisL2Identifier);
    this.myConfigData = setupConfigDataForL2(this.thisL2Identifier, setupLogging);

    // do this after servers and groups have been processed
    validateGroups();
    validateSecurityConfiguration();
  }

  @Override
  public TopologyReloadStatus reloadConfiguration(ServerConnectionValidator serverConnectionValidator,
                                                  TerracottaOperatorEventLogger opEventLogger)
      throws ConfigurationSetupException {
    MutableBeanRepository changedL2sBeanRepository = new StandardBeanRepository(Servers.class);

    String reloadSource = configurationCreator().reloadServersConfiguration(changedL2sBeanRepository, false, false);
    opEventLogger.fireOperatorEvent(TerracottaOperatorEventFactory.createConfigReloadedEvent(reloadSource));

    TopologyVerifier topologyVerifier = new TopologyVerifier(serversBeanRepository(), changedL2sBeanRepository,
                                                             this.activeServerGroupsConfig, serverConnectionValidator);
    TopologyReloadStatus status = topologyVerifier.checkAndValidateConfig();
    if (TopologyReloadStatus.TOPOLOGY_CHANGE_ACCEPTABLE != status) { return status; }

    configurationCreator().reloadServersConfiguration(serversBeanRepository(), true, true);
    this.l2ConfigData.clear();

    this.activeServerGroupsConfig = new ActiveServerGroupsConfigObject(
                                                                       createContext(serversBeanRepository(),
                                                                                     configurationCreator()
                                                                                         .directoryConfigurationLoadedFrom()),
                                                                       this);

    TSAManagementEventPayload tsaManagementEventPayload = new TSAManagementEventPayload("TSA.TOPOLOGY.CONFIG_RELOADED");
    TerracottaRemoteManagement.getRemoteManagementInstance().sendEvent(tsaManagementEventPayload.toManagementEvent());

    return TopologyReloadStatus.TOPOLOGY_CHANGE_ACCEPTABLE;
  }

  @Override
  public boolean isSecure() {
    return secure;
  }

  @Override
  public String getL2Identifier() {
    return this.thisL2Identifier;
  }

  @Override
  public SecurityConfig getSecurity() {
    return this.securityConfig;
  }

  private void verifyPortUsed(Set<String> serverPorts, String hostname, int port) throws ConfigurationSetupException {
    String hostport = hostname + ":" + port;
    if (port != 0 && !serverPorts.add(hostport)) { throw new ConfigurationSetupException(
                                                                                         "The server "
                                                                                             + hostport
                                                                                             + " is specified more than once in tc-config."
                                                                                             + "\nPlease provide different server name or port numbers(tsa-port, jmx-port, "
                                                                                             + "\ntsa-group-port) in tc-config."); }
  }

  private void verifyL2Identifier(final Server[] servers, final String l2Identifier) throws ConfigurationSetupException {
    if (servers == null) return;
    boolean found = false;
    for (Server server : servers) {
      if (server.getName().equals(l2Identifier)) {
        found = true;
        break;
      }
    }

    if ((servers.length > 0) && !found) { throw new ConfigurationSetupException(
                                                                                "You have specified server name '"
                                                                                    + l2Identifier
                                                                                    + "' which does not "
                                                                                    + "exist in the specified tc-config file. \n\n"
                                                                                    + "Please check your settings and try again.");

    }
  }

  private void verifyServerPortUsed(Set<String> serverPorts, Server server) throws ConfigurationSetupException {
    String hostname = server.getHost();
    if (server.isSetTsaPort()) verifyPortUsed(serverPorts, hostname, server.getTsaPort().getIntValue());
    if (server.isSetJmxPort()) verifyPortUsed(serverPorts, hostname, server.getJmxPort().getIntValue());
    if (server.isSetTsaGroupPort()) verifyPortUsed(serverPorts, hostname, server.getTsaGroupPort().getIntValue());
  }

  private void validateGroups() throws ConfigurationSetupException {
    Server[] serverArray = L2DSOConfigObject.getServers(((Servers) serversBeanRepository().bean()));
    List<ActiveServerGroupConfig> groups = this.activeServerGroupsConfig.getActiveServerGroups();
    Set<String> serverPorts = new HashSet<String>();

    validateGroupNames(groups);

    for (Server server : serverArray) {
      verifyServerPortUsed(serverPorts, server);
      String serverName = server.getName();
      boolean found = false;
      int gid = -1;
      for (ActiveServerGroupConfig groupConfig : groups) {
        if (groupConfig.isMember(serverName)) {
          if (found) { throw new ConfigurationSetupException("Server{" + serverName
                                                             + "} is part of more than 1 mirror-group:  groups{" + gid
                                                             + "," + groupConfig.getGroupId() + "}"); }
          gid = groupConfig.getGroupId().toInt();
          found = true;
        }
      }
      if (!found) { throw new ConfigurationSetupException("Server{" + serverName + "} is not part of any mirror-group."); }
    }
  }

  private void validateGroupNames(Collection<ActiveServerGroupConfig> groups) throws ConfigurationSetupException {
    HashSet<String> groupNames = new HashSet<String>();
    for (ActiveServerGroupConfig element : groups) {
      String grpName = element.getGroupName();
      if (grpName != null) {
        if (groupNames.contains(grpName)) { throw new ConfigurationSetupException(
                                                                                  "Group Name {"
                                                                                      + grpName
                                                                                      + "} is part of more than 1 mirror-group groups"); }
        groupNames.add(grpName);
      }
    }
  }

  private UpdateCheckConfig getUpdateCheckConfig() throws XmlException {
    final UpdateCheck defaultUpdateCheck = getDefaultUpdateCheck();

    ChildBeanRepository beanRepository = new ChildBeanRepository(serversBeanRepository(), UpdateCheck.class,
                                                                 new ChildBeanFetcher() {
                                                                   @Override
                                                                   public XmlObject getChild(XmlObject parent) {
                                                                     UpdateCheck updateCheck = ((Servers) parent)
                                                                         .getUpdateCheck();

                                                                     if (updateCheck == null) {
                                                                       updateCheck = defaultUpdateCheck;
                                                                       ((Servers) parent).setUpdateCheck(updateCheck);
                                                                     }
                                                                     return updateCheck;
                                                                   }
                                                                 });

    return new UpdateCheckConfigObject(createContext(beanRepository, configurationCreator()
        .directoryConfigurationLoadedFrom()));
  }

  private UpdateCheck getDefaultUpdateCheck() throws XmlException {
    final int defaultPeriodDays = ((XmlInteger) defaultValueProvider.defaultFor(serversBeanRepository()
        .rootBeanSchemaType(), "update-check/period-days")).getBigIntegerValue().intValue();
    final boolean defaultEnabled = ((XmlBoolean) defaultValueProvider.defaultFor(serversBeanRepository()
        .rootBeanSchemaType(), "update-check/enabled")).getBooleanValue();
    UpdateCheck uc = UpdateCheck.Factory.newInstance();
    uc.setEnabled(defaultEnabled);
    uc.setPeriodDays(defaultPeriodDays);
    return uc;
  }

  // called by configObjects that need to create their own context
  public File getConfigFilePath() {
    return configurationCreator().directoryConfigurationLoadedFrom();
  }

  public void setSecurityConfig(final SecurityConfig securityConfig) {
    this.secure = securityConfig != null;
    this.securityConfig = securityConfig;
  }

  private class L2ConfigData {
    private final String              name;
    private final ChildBeanRepository beanRepository;

    private final CommonL2Config      commonL2Config;
    private final L2DSOConfig         dsoL2Config;

    public L2ConfigData(String name, Servers serversBean) throws ConfigurationSetupException {
      this.name = name;
      findMyL2Bean(); // To get the exception in case things are screwed up
      this.beanRepository = new ChildBeanRepository(serversBeanRepository(), Server.class, new BeanFetcher());
      this.commonL2Config = new CommonL2ConfigObject(createContext(this.beanRepository, configurationCreator()
          .directoryConfigurationLoadedFrom()), secure);
      this.dsoL2Config = new L2DSOConfigObject(createContext(this.beanRepository, configurationCreator()
          .directoryConfigurationLoadedFrom()), serversBean.getGarbageCollection(),
                                               serversBean.getClientReconnectWindow(), serversBean.getRestartable());
    }

    public CommonL2Config commonL2Config() {
      return this.commonL2Config;
    }

    public L2DSOConfig dsoL2Config() {
      return this.dsoL2Config;
    }

    public boolean explicitlySpecifiedInConfigFile() throws ConfigurationSetupException {
      return findMyL2Bean() != null;
    }

    private Server findMyL2Bean() throws ConfigurationSetupException {
      Servers servers = (Servers) serversBeanRepository().bean();
      Server[] l2Array = servers == null ? null : L2DSOConfigObject.getServers(servers);

      if (l2Array == null || l2Array.length == 0) {
        return null;
      } else if (this.name == null) {
        if (l2Array.length > 1) {
          Server rv = autoChooseThisL2(l2Array);
          if (rv == null) {
            throw new ConfigurationSetupException("You have not specified a name for your Terracotta server, and"
                                                  + " there are " + l2Array.length
                                                  + " servers defined in the Terracotta configuration file. "
                                                  + " Pass the desired server name to the script using "
                                                  + "the -n flag.");

          } else {
            return rv;
          }
        } else {
          return l2Array[0];
        }
      } else {
        for (final Server aL2Array : l2Array) {
          if (this.name.trim().equals(aL2Array.getName().trim())) { return aL2Array; }
        }
      }

      return null;
    }

    private class BeanFetcher implements ChildBeanFetcher {
      @Override
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

  private Server autoChooseThisL2(Server[] servers) throws ConfigurationSetupException {
    Server myL2 = null;

    if (servers == null) {
      // no server elements. return nothing
    } else if (servers.length == 1) {
      myL2 = servers[0];
    } else {
      try {
        for (Server server : servers) {
          if (localInetAddresses.contains(InetAddress.getByName(server.getHost()))) {
            if (myL2 == null) {
              myL2 = server;
            } else {
              throw new ConfigurationSetupException("You have not specified a name for your Terracotta server, and"
                                                    + " there are " + servers.length
                                                    + " servers defined in the Terracotta configuration file. "
                                                    + "The script can not automatically choose between "
                                                    + "the following server names: " + myL2.getName() + ", "
                                                    + server.getName()
                                                    + ". Pass the desired server name to the script using "
                                                    + "the -n flag.");

            }
          }
        }
      } catch (UnknownHostException uhe) {
        throw new ConfigurationSetupException("Exception when trying to choose this L2 instance : " + uhe);
      }
    }
    return myL2;
  }

  private Set<InetAddress> getAllLocalInetAddresses() {
    Set<InetAddress> localAddresses = new HashSet<InetAddress>();
    Enumeration<NetworkInterface> networkInterfaces;
    try {
      networkInterfaces = NetworkInterface.getNetworkInterfaces();
    } catch (SocketException e) {
      throw new RuntimeException(e);
    }
    while (networkInterfaces.hasMoreElements()) {
      Enumeration<InetAddress> inetAddresses = networkInterfaces.nextElement().getInetAddresses();
      while (inetAddresses.hasMoreElements()) {
        localAddresses.add(inetAddresses.nextElement());
      }
    }
    return localAddresses;
  }

  @Override
  public String describeSources() {
    return this.configurationCreator().describeSources();
  }

  private synchronized L2ConfigData configDataFor(String name) throws ConfigurationSetupException {
    L2ConfigData out = this.l2ConfigData.get(name);

    if (out == null) {
      out = new L2ConfigData(name, serversBean);

      if ((!out.explicitlySpecifiedInConfigFile()) && name != null) {
        Servers servers = (Servers) this.serversBeanRepository().bean();
        String list;
        if (servers == null) {
          list = "[data unavailable]";
        } else {
          Server[] serverList = L2DSOConfigObject.getServers(servers);
          if (serverList == null) {
            list = "[data unavailable]";
          } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < serverList.length; ++i) {
              if (i > 0) sb.append(", ");
              if (i == serverList.length - 1) sb.append("and ");
              sb.append("'").append(serverList[i].getName()).append("'");
            }
            list = sb.toString();
          }
        }

        // formatting
        throw new ConfigurationSetupException(
                                              "Multiple <server> elements are defined in the configuration file. "
                                                  + "As such, each server that you start needs to know which configuration "
                                                  + "it should use.\n\n"
                                                  + "However, this server couldn't figure out which one it is -- it thinks it's "
                                                  + "called '"
                                                  + name
                                                  + "', but you've only created <server> elements in the config file called "
                                                  + list
                                                  + ".\n\nPlease re-start the server with a '-n <name>' argument on the command line to tell this "
                                                  + "server which one it is, or change the 'name' attributes of the <server> "
                                                  + "elements in the config file as appropriate.");
      }
      this.l2ConfigData.put(name, out);
    }

    return out;
  }

  private L2ConfigData setupConfigDataForL2(final String l2Identifier, boolean setupLogging)
      throws ConfigurationSetupException {
    L2ConfigData serverConfigData = configDataFor(l2Identifier);

    if (setupLogging) {
      LogSettingConfigItemListener listener = new LogSettingConfigItemListener(TCLogging.PROCESS_TYPE_L2);
      listener.valueChanged(null, serverConfigData.commonL2Config().logsPath());
    }
    return serverConfigData;
  }

  private void validateSecurityConfiguration() throws ConfigurationSetupException {
    Servers servers = (Servers) serversBeanRepository().bean();
    if (servers.getSecure() && securityConfig.getSslCertificateUri() == null) { throw new ConfigurationSetupException(
                                                                                                                      "Security is enabled but server "
                                                                                                                          + thisL2Identifier
                                                                                                                          + " has no configured SSL certificate."); }
  }

  @Override
  public CommonL2Config commonL2ConfigFor(String name) throws ConfigurationSetupException {
    return configDataFor(name).commonL2Config();
  }

  @Override
  public CommonL2Config commonl2Config() {
    return this.myConfigData.commonL2Config();
  }

  @Override
  public L2DSOConfig dsoL2ConfigFor(String name) throws ConfigurationSetupException {
    return configDataFor(name).dsoL2Config();
  }

  @Override
  public L2DSOConfig dsoL2Config() {
    return this.myConfigData.dsoL2Config();
  }

  @Override
  public UpdateCheckConfig updateCheckConfig() {
    return updateCheckConfig;
  }

  @Override
  public ActiveServerGroupsConfig activeServerGroupsConfig() {
    return activeServerGroupsConfig;
  }

  @Override
  public String[] allCurrentlyKnownServers() {
    List<String> servers = new ArrayList<String>();
    for (ActiveServerGroupConfig group : activeServerGroupsConfig.getActiveServerGroups()) {
      for (String member : group.getMembers()) {
        servers.add(member);
      }
    }

    return servers.toArray(new String[servers.size()]);
  }

  @Override
  public InputStream rawConfigFile() {
    String text = configurationCreator().rawConfigText();
    try {
      return new ByteArrayInputStream(text.getBytes("UTF-8"));
    } catch (UnsupportedEncodingException uee) {
      throw Assert.failure("This shouldn't be possible", uee);
    }
  }

  @Override
  public InputStream effectiveConfigFile() {
    // This MUST piece together the configuration from our currently-active
    // bean repositories. If we just read the
    // actual config file we got on startup, we'd be sending out, well, the
    // config we got on startup -- which might be
    // quite different from our current config, if an L1 came in and
    // overrode our config. This effective config will
    // also contain the effects of parameter substitution for server
    // elements

    TcConfigDocument doc = TcConfigDocument.Factory.newInstance();
    TcConfigDocument.TcConfig config = doc.addNewTcConfig();

    TcProperties tcProperties = (TcProperties) this.tcPropertiesRepository().bean();
    Client client = (Client) this.clientBeanRepository().bean();
    Servers servers = (Servers) this.serversBeanRepository().bean();

    if (client != null) config.setClients(client);
    if (servers != null) config.setServers(servers);
    if (tcProperties != null) config.setTcProperties(tcProperties);

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

  private void overwriteTcPropertiesFromConfig() {
    TCProperties tcProps = TCPropertiesImpl.getProperties();

    Map<String, String> propMap = new HashMap<String, String>();
    for (TcProperty tcp : this.configTCProperties.getTcPropertiesArray()) {
      propMap.put(tcp.getPropertyName().trim(), tcp.getPropertyValue().trim());
    }

    tcProps.overwriteTcPropertiesFromConfig(propMap);
  }

  @Override
  public ActiveServerGroupConfig getActiveServerGroupForThisL2() {
    return this.activeServerGroupsConfig.getActiveServerGroupForL2(this.thisL2Identifier);
  }

}
