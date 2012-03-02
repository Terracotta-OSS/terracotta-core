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
import com.tc.config.schema.HaConfigSchema;
import com.tc.config.schema.IllegalConfigurationChangeHandler;
import com.tc.config.schema.SystemConfig;
import com.tc.config.schema.SystemConfigObject;
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
import com.tc.object.config.schema.L2DSOConfig;
import com.tc.object.config.schema.L2DSOConfigObject;
import com.tc.operatorevent.TerracottaOperatorEventFactory;
import com.tc.operatorevent.TerracottaOperatorEventLogger;
import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesImpl;
import com.tc.server.ServerConnectionValidator;
import com.tc.util.Assert;
import com.terracottatech.config.Client;
import com.terracottatech.config.MirrorGroups;
import com.terracottatech.config.PersistenceMode;
import com.terracottatech.config.Server;
import com.terracottatech.config.Servers;
import com.terracottatech.config.System;
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

  private final Map                         l2ConfigData;
  private final HaConfigSchema              haConfig;
  private final UpdateCheckConfig           updateCheckConfig;
  private final String                      thisL2Identifier;
  private final L2ConfigData                myConfigData;
  private final ConfigTCProperties          configTCProperties;
  private final Set<InetAddress>            localInetAddresses;

  private SystemConfig                      systemConfig;
  private volatile ActiveServerGroupsConfig activeServerGroupsConfig;

  public L2ConfigurationSetupManagerImpl(ConfigurationCreator configurationCreator, String thisL2Identifier,
                                         DefaultValueProvider defaultValueProvider,
                                         XmlObjectComparator xmlObjectComparator,
                                         IllegalConfigurationChangeHandler illegalConfigChangeHandler)
      throws ConfigurationSetupException {
    this((String[]) null, configurationCreator, thisL2Identifier, defaultValueProvider, xmlObjectComparator,
         illegalConfigChangeHandler);
  }

  public L2ConfigurationSetupManagerImpl(String[] args, ConfigurationCreator configurationCreator,
                                         String thisL2Identifier, DefaultValueProvider defaultValueProvider,
                                         XmlObjectComparator xmlObjectComparator,
                                         IllegalConfigurationChangeHandler illegalConfigChangeHandler)
      throws ConfigurationSetupException {
    super(args, configurationCreator, defaultValueProvider, xmlObjectComparator, illegalConfigChangeHandler);

    Assert.assertNotNull(defaultValueProvider);
    Assert.assertNotNull(xmlObjectComparator);

    this.systemConfig = null;
    this.l2ConfigData = new HashMap();

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

    ChildBeanRepository mirrorGroupsRepository = new ChildBeanRepository(serversBeanRepository(), MirrorGroups.class,
                                                                         new ChildBeanFetcher() {
                                                                           public XmlObject getChild(XmlObject parent) {
                                                                             return ((Servers) serversBeanRepository()
                                                                                 .bean()).getMirrorGroups();
                                                                           }
                                                                         });
    this.activeServerGroupsConfig = new ActiveServerGroupsConfigObject(
                                                                       createContext(mirrorGroupsRepository,
                                                                                     configurationCreator()
                                                                                         .directoryConfigurationLoadedFrom()),
                                                                       this);

    Servers serversBean = (Servers) serversBeanRepository().bean();
    Server[] servers = serversBean != null ? serversBean.getServerArray() : null;

    if (thisL2Identifier != null) {
      this.thisL2Identifier = thisL2Identifier;
    } else {
      Server s = autoChooseThisL2(servers);
      this.thisL2Identifier = (s != null ? s.getName() : null);
    }
    verifyL2Identifier(servers, this.thisL2Identifier);
    this.myConfigData = setupConfigDataForL2(this.thisL2Identifier);

    this.haConfig = getHaConfig();

    // do this after servers and groups have been processed
    validateGroups();
    validateDSOClusterPersistenceMode();
    validateHaConfiguration();
  }

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

    ChildBeanRepository mirrorGroupsRepository = new ChildBeanRepository(serversBeanRepository(), MirrorGroups.class,
                                                                         new ChildBeanFetcher() {
                                                                           public XmlObject getChild(XmlObject parent) {
                                                                             return ((Servers) serversBeanRepository()
                                                                                 .bean()).getMirrorGroups();
                                                                           }
                                                                         });

    this.activeServerGroupsConfig = new ActiveServerGroupsConfigObject(
                                                                       createContext(mirrorGroupsRepository,
                                                                                     configurationCreator()
                                                                                         .directoryConfigurationLoadedFrom()),
                                                                       this);

    return TopologyReloadStatus.TOPOLOGY_CHANGE_ACCEPTABLE;
  }

  public String getL2Identifier() {
    return this.thisL2Identifier;
  }

  private void verifyPortUsed(Set<String> serverPorts, String hostname, int port) throws ConfigurationSetupException {
    String hostport = hostname + ":" + port;
    if (port != 0 && !serverPorts.add(hostport)) { throw new ConfigurationSetupException(
                                                                                         "The server "
                                                                                             + hostport
                                                                                             + " is specified more than once in tc-config."
                                                                                             + "\nPlease provide different server name or port numbers(dso-port, jmx-port, "
                                                                                             + "\nl2-group-port) in tc-config."); }
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
    if (server.isSetDsoPort()) verifyPortUsed(serverPorts, hostname, server.getDsoPort().getIntValue());
    if (server.isSetJmxPort()) verifyPortUsed(serverPorts, hostname, server.getJmxPort().getIntValue());
    if (server.isSetL2GroupPort()) verifyPortUsed(serverPorts, hostname, server.getL2GroupPort().getIntValue());
  }

  private void validateGroups() throws ConfigurationSetupException {
    Server[] serverArray = ((Servers) serversBeanRepository().bean()).getServerArray();
    List<ActiveServerGroupConfig> groups = this.activeServerGroupsConfig.getActiveServerGroups();
    Set<String> serverPorts = new HashSet<String>();

    validateGroupNames(groups);

    for (Server element : serverArray) {
      verifyServerPortUsed(serverPorts, element);
      String serverName = element.getName();
      boolean found = false;
      int gid = -1;
      for (ActiveServerGroupConfig element2 : groups) {
        if (element2.isMember(serverName)) {
          if (found) { throw new ConfigurationSetupException("Server{" + serverName
                                                             + "} is part of more than 1 mirror-group:  groups{" + gid
                                                             + "," + element2.getGroupId() + "}"); }
          gid = element2.getGroupId().toInt();
          found = true;
        }
      }
      if (!found) { throw new ConfigurationSetupException("Server{" + serverName + "} is not part of any mirror-group."); }
    }

    Set<String> allServers = new HashSet<String>();
    for (Server server : serverArray) {
      allServers.add(server.getName());
    }

    for (ActiveServerGroupConfig element : groups) {
      for (String member : element.getMembers().getMemberArray()) {
        if (!allServers.contains(member)) { throw new ConfigurationSetupException(
                                                                                  "Server{"
                                                                                      + member
                                                                                      + "} is not defined but has been added as a member in the group "
                                                                                      + element.getGroupName()); }
      }
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

  // make sure there is at most one of these
  private HaConfigSchema getHaConfig() {
    HaConfigSchema newHaConfig = null;
    if (this.activeServerGroupsConfig.getActiveServerGroupCount() != 0) {
      ActiveServerGroupConfig groupConfig = getActiveServerGroupForThisL2();
      if (groupConfig != null) {
        newHaConfig = groupConfig.getHaHolder();
      }
    }
    return newHaConfig;
  }

  private UpdateCheckConfig getUpdateCheckConfig() throws XmlException {
    final UpdateCheck defaultUpdateCheck = getDefaultUpdateCheck();

    ChildBeanRepository beanRepository = new ChildBeanRepository(serversBeanRepository(), UpdateCheck.class,
                                                                 new ChildBeanFetcher() {
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

  private class L2ConfigData {
    private final String              name;
    private final ChildBeanRepository beanRepository;

    private final CommonL2Config      commonL2Config;
    private final L2DSOConfig         dsoL2Config;

    public L2ConfigData(String name) throws ConfigurationSetupException {
      this.name = name;
      findMyL2Bean(); // To get the exception in case things are screwed up
      this.beanRepository = new ChildBeanRepository(serversBeanRepository(), Server.class, new BeanFetcher());
      this.commonL2Config = new CommonL2ConfigObject(createContext(this.beanRepository, configurationCreator()
          .directoryConfigurationLoadedFrom()));
      this.dsoL2Config = new L2DSOConfigObject(createContext(this.beanRepository, configurationCreator()
          .directoryConfigurationLoadedFrom()));
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
      Server[] l2Array = servers == null ? null : servers.getServerArray();

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
        for (int i = 0; i < l2Array.length; ++i) {
          if (this.name.trim().equals(l2Array[i].getName().trim())) { return l2Array[i]; }
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

  public String describeSources() {
    return this.configurationCreator().describeSources();
  }

  private synchronized L2ConfigData configDataFor(String name) throws ConfigurationSetupException {
    L2ConfigData out = (L2ConfigData) this.l2ConfigData.get(name);

    if (out == null) {
      out = new L2ConfigData(name);

      if ((!out.explicitlySpecifiedInConfigFile()) && name != null) {
        Servers servers = (Servers) this.serversBeanRepository().bean();
        String list;
        if (servers == null) {
          list = "[data unavailable]";
        } else {
          Server[] serverList = servers.getServerArray();
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

  private L2ConfigData setupConfigDataForL2(final String l2Identifier) throws ConfigurationSetupException {
    this.systemConfig = new SystemConfigObject(createContext(systemBeanRepository(), configurationCreator()
        .directoryConfigurationLoadedFrom()));
    L2ConfigData serverConfigData = configDataFor(this.thisL2Identifier);
    LogSettingConfigItemListener listener = new LogSettingConfigItemListener(TCLogging.PROCESS_TYPE_L2);
    listener.valueChanged(null, serverConfigData.commonL2Config().logsPath());
    return serverConfigData;
  }

  private void validateDSOClusterPersistenceMode() throws ConfigurationSetupException {
    validatePersistenceModeInGroups();
    List<ActiveServerGroupConfig> groups = this.activeServerGroupsConfig.getActiveServerGroups();

    Map<String, Boolean> serversToMode = new HashMap<String, Boolean>();
    for (ActiveServerGroupConfig element : groups) {
      boolean isNwAP = element.getHaHolder().isNetworkedActivePassive();
      String[] members = element.getMembers().getMemberArray();
      for (String member : members) {
        serversToMode.put(member, isNwAP);
      }
    }

    if (super.serversBeanRepository().bean() != null) {
      Server[] servers = ((Servers) super.serversBeanRepository().bean()).getServerArray();
      Set badServers = new HashSet();

      if (servers != null && servers.length > 1) {
        // We have clustered DSO; they must all be in permanent-store
        // mode
        for (Server server : servers) {
          String name = server.getName();
          L2ConfigData data = configDataFor(name);

          Assert.assertNotNull(data);
          boolean isNwAP = serversToMode.get(name);
          if (!isNwAP && (data.dsoL2Config().getPersistence().getMode() != PersistenceMode.PERMANENT_STORE)) {
            badServers.add(name);
          }
        }
      }

      if (badServers.size() > 0) {
        // formatting
        throw new ConfigurationSetupException(
                                              "At least one server defined in the Terracotta configuration file is in \n'"
                                                  + PersistenceMode.TEMPORARY_SWAP_ONLY
                                                  + "' persistence mode. (Servers in this mode: \n"
                                                  + badServers
                                                  + ".) \n\n"
                                                  + "If even one server has persistence mode set to  "
                                                  + PersistenceMode.TEMPORARY_SWAP_ONLY
                                                  + ", \nthen High Availability mode must be set to 'networked-active-passive'"
                                                  + "\n\nFor servers in a mirror group, High Availability mode can be set per"
                                                  + "\nmirror group. A mirror-group High Availability setting overrides the main"
                                                  + "\nHigh Availability for that mirror group.\n\n"
                                                  + "See the Terracotta documentation for more details.");
      }
    }
  }

  private void validatePersistenceModeInGroups() throws ConfigurationSetupException {
    List<ActiveServerGroupConfig> groupArray = this.activeServerGroupsConfig.getActiveServerGroups();

    for (ActiveServerGroupConfig group : groupArray) {
      String[] members = group.getMembers().getMemberArray();
      if (members.length > 1) {
        PersistenceMode.Enum baseMode = configDataFor(members[0]).dsoL2Config.getPersistence().getMode();
        for (int i = 1; i < members.length; i++) {
          L2ConfigData memberData = configDataFor(members[i]);
          if (memberData.dsoL2Config.getPersistence().getMode() != baseMode) {
            StringBuilder msg = new StringBuilder();
            msg.append("The persistence mode of the servers in the group ").append(group.getGroupName())
                .append(" with servers {");
            for (String member : members) {
              msg.append(member).append(" ");
            }
            msg.append("} are not equal. To maintain consitency all the servers in a group need to have same persistence mode");
            throw new ConfigurationSetupException(msg.toString());
          }
        }
      }
    }

  }

  public void validateHaConfiguration() throws ConfigurationSetupException {
    int networkedHa = 0;
    int diskbasedHa = 0;
    List<ActiveServerGroupConfig> asgcArray = activeServerGroupsConfig.getActiveServerGroups();
    for (ActiveServerGroupConfig asgc : asgcArray) {
      if (asgc.getHaHolder().isNetworkedActivePassive()) {
        ++networkedHa;
      } else {
        ++diskbasedHa;
      }
    }
    if (networkedHa > 0 && diskbasedHa > 0) { throw new ConfigurationSetupException(
                                                                                    "All mirror-groups must be set to the same High Availability mode. Your tc-config.xml has "
                                                                                        + networkedHa
                                                                                        + " group(s) set to networked HA and "
                                                                                        + diskbasedHa
                                                                                        + " group(s) set to disk-based HA."); }
  }

  public CommonL2Config commonL2ConfigFor(String name) throws ConfigurationSetupException {
    return configDataFor(name).commonL2Config();
  }

  public CommonL2Config commonl2Config() {
    return this.myConfigData.commonL2Config();
  }

  public SystemConfig systemConfig() {
    return this.systemConfig;
  }

  public L2DSOConfig dsoL2ConfigFor(String name) throws ConfigurationSetupException {
    return configDataFor(name).dsoL2Config();
  }

  public L2DSOConfig dsoL2Config() {
    return this.myConfigData.dsoL2Config();
  }

  public HaConfigSchema haConfig() {
    return haConfig;
  }

  public UpdateCheckConfig updateCheckConfig() {
    return updateCheckConfig;
  }

  public ActiveServerGroupsConfig activeServerGroupsConfig() {
    return activeServerGroupsConfig;
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
    String text = configurationCreator().rawConfigText();
    try {
      return new ByteArrayInputStream(text.getBytes("UTF-8"));
    } catch (UnsupportedEncodingException uee) {
      throw Assert.failure("This shouldn't be possible", uee);
    }
  }

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
    System system = (System) this.systemBeanRepository().bean();
    Client client = (Client) this.clientBeanRepository().bean();
    Servers servers = (Servers) this.serversBeanRepository().bean();

    if (system != null) config.setSystem(system);
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

  public ActiveServerGroupConfig getActiveServerGroupForThisL2() {
    return this.activeServerGroupsConfig.getActiveServerGroupForL2(this.thisL2Identifier);
  }

}
