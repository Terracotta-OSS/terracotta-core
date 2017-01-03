/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.config.schema.setup;

import com.tc.config.TcProperty;
import com.tc.config.schema.ActiveServerGroupConfig;
import com.tc.config.schema.ActiveServerGroupConfigObject;
import com.tc.config.schema.CommonL2Config;
import com.tc.config.schema.CommonL2ConfigObject;
import com.tc.config.schema.ConfigTCProperties;
import com.tc.config.schema.ConfigTCPropertiesFromObject;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.management.TSAManagementEventPayload;
import com.tc.management.TerracottaRemoteManagement;
import com.tc.object.config.schema.L2Config;
import com.tc.object.config.schema.L2ConfigObject;
import com.tc.operatorevent.TerracottaOperatorEventFactory;
import com.tc.operatorevent.TerracottaOperatorEventLogger;
import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesImpl;
import com.tc.server.ServerConnectionValidator;
import com.tc.util.Assert;
import org.terracotta.config.Server;
import org.terracotta.config.Servers;
import org.terracotta.config.TcConfig;
import org.terracotta.config.TcConfiguration;
import org.terracotta.config.TcProperties;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * The standard implementation of {@link com.tc.config.schema.setup.L2ConfigurationSetupManager}.
 */
public class L2ConfigurationSetupManagerImpl extends BaseConfigurationSetupManager implements L2ConfigurationSetupManager {
  private final Map<String, L2ConfigData> l2ConfigData;
  private final String thisL2Identifier;
  private final L2ConfigData myConfigData;
  private final ConfigTCProperties configTCProperties;
  private final Set<InetAddress> localInetAddresses;
  private final TcConfiguration configuration;

  private volatile ActiveServerGroupConfig activeServerGroupConfig;
  private volatile boolean secure;

  private Servers serversBean;

  public L2ConfigurationSetupManagerImpl(ConfigurationCreator configurationCreator, String thisL2Identifier, ClassLoader loader)
      throws ConfigurationSetupException {
    this(null, configurationCreator, thisL2Identifier, loader);
  }

  public L2ConfigurationSetupManagerImpl(String[] args, ConfigurationCreator configurationCreator, String thisL2Identifier, ClassLoader loader)
      throws ConfigurationSetupException {
    super(args, configurationCreator);

    this.l2ConfigData = new HashMap<>();

    this.localInetAddresses = getAllLocalInetAddresses();

    // this sets the beans in each repository
    runConfigurationCreator(loader);
    this.configTCProperties = new ConfigTCPropertiesFromObject(tcPropertiesRepository());
    overwriteTcPropertiesFromConfig();

    // do this after runConfigurationCreator method call, after serversBeanRepository is set


    serversBean = serversBeanRepository();
    this.secure = serversBean != null && serversBean.isSecure();
    Server[] servers = serversBean != null ? L2ConfigObject.getServers(serversBean) : null;
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

    configuration = tcConfigurationRepository();

    verifyL2Identifier(servers, this.thisL2Identifier);
    this.myConfigData = setupConfigDataForL2(this.thisL2Identifier);
    this.activeServerGroupConfig = new ActiveServerGroupConfigObject(configuration.getPlatformConfiguration().getServers(), this);
  }

  @Override
  public TopologyReloadStatus reloadConfiguration(ServerConnectionValidator serverConnectionValidator,
                                                  TerracottaOperatorEventLogger opEventLogger) throws ConfigurationSetupException {
    String reloadSource = configurationCreator().reloadServersConfiguration(false, false);
    Servers changedL2sBeanRepository = configurationCreator().getParsedConfiguration().getPlatformConfiguration().getServers();
    opEventLogger.fireOperatorEvent(TerracottaOperatorEventFactory.createConfigReloadedEvent(reloadSource));

    TopologyVerifier topologyVerifier = new TopologyVerifier(serversBeanRepository(), changedL2sBeanRepository,
                                                             this.activeServerGroupConfig, serverConnectionValidator);
    TopologyReloadStatus status = topologyVerifier.checkAndValidateConfig();
    if (TopologyReloadStatus.TOPOLOGY_CHANGE_ACCEPTABLE != status) { return status; }

    configurationCreator().reloadServersConfiguration(true, true);
    this.l2ConfigData.clear();

    this.serversBean = configurationCreator().getParsedConfiguration().getPlatformConfiguration().getServers();
    this.activeServerGroupConfig = new ActiveServerGroupConfigObject(serversBean, this);

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

  private void verifyL2Identifier(Server[] servers, String l2Identifier) throws ConfigurationSetupException {
    if (servers == null) return;
    boolean found = false;
    for (Server server : servers) {
      if (server.getName().equals(l2Identifier)) {
        found = true;
        break;
      }
    }

    if ((servers.length > 0) && !found) { throw new ConfigurationSetupException("You have specified server name '" + l2Identifier
                                                                                + "' which does not "
                                                                                + "exist in the specified tc-config file. \n\n"
                                                                                + "Please check your settings and try again.");

    }
  }

  private class L2ConfigData {
    private final String name;
    private final CommonL2Config commonL2Config;
    private final L2Config dsoL2Config;

    public L2ConfigData(String name, Servers serversBean) throws ConfigurationSetupException {
      this.name = name;
      Server s = findMyL2Bean(); // To get the exception in case things are screwed up
      this.commonL2Config = new CommonL2ConfigObject(s, configuration, secure);
      this.dsoL2Config = new L2ConfigObject(s, serversBean.getClientReconnectWindow());
    }

    public CommonL2Config commonL2Config() {
      return this.commonL2Config;
    }

    public L2Config dsoL2Config() {
      return this.dsoL2Config;
    }

    public boolean explicitlySpecifiedInConfigFile() throws ConfigurationSetupException {
      return findMyL2Bean() != null;
    }

    private Server findMyL2Bean() throws ConfigurationSetupException {
      Servers servers = serversBeanRepository();
      Server[] l2Array = servers == null ? null : L2ConfigObject.getServers(servers);

      if (l2Array == null || l2Array.length == 0) {
        return null;
      } else if (this.name == null) {
        if (l2Array.length > 1) {
          Server rv = autoChooseThisL2(l2Array);
          if (rv == null) {
            throw new ConfigurationSetupException("You have not specified a name for your Terracotta server, and" + " there are "
                                                  + l2Array.length + " servers defined in the Terracotta configuration file. "
                                                  + " Pass the desired server name to the script using " + "the -n flag.");

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
              throw new ConfigurationSetupException("You have not specified a name for your Terracotta server, and" + " there are "
                                                    + servers.length + " servers defined in the Terracotta configuration file. "
                                                    + "The script can not automatically choose between " + "the following server names: "
                                                    + myL2.getName() + ", " + server.getName()
                                                    + ". Pass the desired server name to the script using " + "the -n flag.");

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
    Set<InetAddress> localAddresses = new HashSet<>();
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
        Servers servers = this.serversBeanRepository();
        String list;
        if (servers == null) {
          list = "[data unavailable]";
        } else {
          Server[] serverList = L2ConfigObject.getServers(servers);
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

  private L2ConfigData setupConfigDataForL2(String l2Identifier) throws ConfigurationSetupException {
    L2ConfigData serverConfigData = configDataFor(l2Identifier);
    return serverConfigData;
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
  public L2Config dsoL2ConfigFor(String name) throws ConfigurationSetupException {
    return configDataFor(name).dsoL2Config();
  }

  @Override
  public L2Config dsoL2Config() {
    return this.myConfigData.dsoL2Config();
  }

  @Override
  public String[] allCurrentlyKnownServers() {
    List<String> servers = new ArrayList<>();
      for (String member : activeServerGroupConfig.getMembers()) {
        servers.add(member);
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

    TcConfig config = new TcConfig();

    TcProperties tcProperties = this.tcPropertiesRepository();
    Servers servers = this.serversBeanRepository();

    if (servers != null) config.setServers(servers);
    if (tcProperties != null) config.setTcProperties(tcProperties);

    try {
      JAXBContext jaxbContext = JAXBContext.newInstance(TcConfig.class);      
      StringWriter sw = new StringWriter();
      Marshaller marshaller = jaxbContext.createMarshaller();
      marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
      marshaller.marshal(config, sw);      
      return new ByteArrayInputStream(sw.toString().getBytes("UTF-8"));
    } catch (JAXBException | UnsupportedEncodingException e) {
      throw Assert.failure("This shouldn't be possible", e);
    }
  }

  private void overwriteTcPropertiesFromConfig() {
    TCProperties tcProps = TCPropertiesImpl.getProperties();

    Map<String, String> propMap = new HashMap<>();
    for (TcProperty tcp : this.configTCProperties.getTcPropertiesArray()) {
      propMap.put(tcp.getPropertyName().trim(), tcp.getPropertyValue().trim());
    }

    tcProps.overwriteTcPropertiesFromConfig(propMap);
  }

  @Override
  public ActiveServerGroupConfig getActiveServerGroupForThisL2() {
    return this.activeServerGroupConfig;
  }

}
