/*
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Configuration.
 *
 * The Initial Developer of the Covered Software is
 * Terracotta, Inc., a Software AG company
 *
 */
package com.tc.config;

import org.terracotta.config.Property;
import org.terracotta.config.Server;
import org.terracotta.config.Servers;
import org.terracotta.config.TcProperties;

import com.tc.classloader.ServiceLocator;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.properties.TCPropertiesImpl;
import com.tc.text.PrettyPrintable;
import com.terracotta.config.Configuration;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class ServerConfigurationManager implements PrettyPrintable {

  private final Configuration configuration;
  private final GroupConfiguration groupConfiguration;
  private final boolean consistentStartup;
  private final boolean upgrade;
  private final ServerConfiguration serverConfiguration;
  private final ServiceLocator serviceLocator;
  private final String[] startUpArgs;

  public ServerConfigurationManager(String serverName,
                                    Configuration configuration,
                                    boolean consistentStartup,
                                    boolean upgrade,
                                    ClassLoader classLoader,
                                    String[] startUpArgs) throws ConfigurationSetupException {
    Objects.requireNonNull(configuration);
    Objects.requireNonNull(classLoader);
    Objects.requireNonNull(startUpArgs);

    this.configuration = configuration;
    this.serviceLocator = new ServiceLocator(classLoader);

    Servers servers = configuration.getPlatformConfiguration().getServers();

    if (servers == null || servers.getServer() == null) {
      throw new NullPointerException("servers is null");
    }

    Server defaultServer;
    if (serverName != null) {
      defaultServer = findServer(servers, serverName);
    } else {
      defaultServer = getDefaultServer(servers);
    }

    this.serverConfiguration = new ServerConfiguration(defaultServer, servers.getClientReconnectWindow());

    Map<String, ServerConfiguration> serverConfigurationMap = getServerConfigurationMap(servers);

    this.groupConfiguration = new GroupConfiguration(serverConfigurationMap, this.serverConfiguration.getName());

    this.consistentStartup = consistentStartup;
    this.upgrade = upgrade;

    this.startUpArgs = Arrays.copyOf(startUpArgs, startUpArgs.length);

    processTcProperties(configuration.getPlatformConfiguration().getTcProperties());
  }

  public String[] getProcessArguments() {
    return Arrays.copyOf(startUpArgs, startUpArgs.length);
  }

  public ServerConfiguration getServerConfiguration() {
    return this.serverConfiguration;
  }

  public GroupConfiguration getGroupConfiguration() {
    return this.groupConfiguration;
  }

  public InputStream rawConfigFile() {
    String text = configuration.getRawConfiguration();
    return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
  }

  public String[] allCurrentlyKnownServers() {
    return groupConfiguration.getMembers();
  }

  public boolean consistentStartup() {
    return this.consistentStartup;
  }
  
  public boolean upgradeCompatiblity() {
    return this.upgrade;
  }

  public boolean isPartialConfiguration() {
    return this.configuration.isPartialConfiguration();
  }

  public ServiceLocator getServiceLocator() {
    return this.serviceLocator;
  }

  public Configuration getConfiguration() {
    return configuration;
  }

  private static Server findServer(Servers servers, String serverName) throws ConfigurationSetupException {
    for (Server server : servers.getServer()) {
      if (server.getName().equals(serverName)) {
        return server;
      }
    }

    throw new ConfigurationSetupException("You have specified server name '" + serverName
                                          + "' which does not exist in the specified configuration. \n\n"
                                          + "Please check your settings and try again.");
  }

  private Server getDefaultServer(Servers servers) throws ConfigurationSetupException {
    List<Server> serverList = servers.getServer();
    if (serverList.size() == 1) {
      return serverList.get(0);
    }

    try {
      Set<InetAddress> allLocalInetAddresses = getAllLocalInetAddresses();
      Server defaultServer = null;
      for (Server server : serverList) {
        if (allLocalInetAddresses.contains(InetAddress.getByName(server.getHost()))) {
          if (defaultServer == null) {
            defaultServer = server;
          } else {
            throw new ConfigurationSetupException("You have not specified a name for your Terracotta server, and" + " there are "
                                                  + serverList.size() + " servers defined in the Terracotta configuration file. "
                                                  + "The script can not automatically choose between the following server names: "
                                                  + defaultServer.getName() + ", " + server.getName()
                                                  + ". Pass the desired server name to the script using " + "the -n flag.");

          }
        }
      }
      return defaultServer;
    } catch (UnknownHostException uhe) {
      throw new ConfigurationSetupException("Exception when trying to find the default server configuration", uhe);
    }
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

  private Map<String, ServerConfiguration> getServerConfigurationMap(Servers servers) {
    Map<String, ServerConfiguration> serverConfigurationMap = new HashMap<>();
    for (Server server : servers.getServer()) {
      if (server.getName() != null) {
        serverConfigurationMap.put(server.getName(), new ServerConfiguration(server, servers.getClientReconnectWindow()));
      }
    }
    return serverConfigurationMap;
  }

  private static void processTcProperties(TcProperties tcProperties) {
    Map<String, String> propMap = new HashMap<>();

    if (tcProperties != null) {
      for (Property tcp : tcProperties.getProperty()) {
        propMap.put(tcp.getName().trim(), tcp.getValue().trim());
      }
    }

    TCPropertiesImpl.getProperties().overwriteTcPropertiesFromConfig(propMap);
  }

  @Override
  public Map<String, ?> getStateMap() {
    if (configuration instanceof PrettyPrintable) {
      return ((PrettyPrintable)configuration).getStateMap();
    } else {
      return Collections.emptyMap();
    }
  }
}
