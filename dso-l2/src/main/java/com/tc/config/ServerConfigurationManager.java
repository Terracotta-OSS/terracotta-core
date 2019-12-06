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
package com.tc.config;


import com.tc.classloader.ServiceLocator;
import com.tc.properties.TCPropertiesImpl;
import com.tc.text.PrettyPrintable;
import org.terracotta.config.Configuration;
import org.terracotta.config.ConfigurationProvider;
import org.terracotta.config.ServerConfiguration;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import org.terracotta.config.ConfigurationException;

public class ServerConfigurationManager implements PrettyPrintable {

  private final ConfigurationProvider configurationProvider;
  private final Configuration configuration;
  private final GroupConfiguration groupConfiguration;
  private final boolean consistentStartup;
  private final boolean upgrade;
  private final ServerConfiguration serverConfiguration;
  private final ServiceLocator serviceLocator;
  private final String[] startUpArgs;

  public ServerConfigurationManager(String serverName,
                                    ConfigurationProvider configurationProvider,
                                    boolean consistentStartup,
                                    boolean upgrade,
                                    ServiceLocator classLoader,
                                    String[] startUpArgs) throws ConfigurationException {
    Objects.requireNonNull(configurationProvider);
    Objects.requireNonNull(classLoader);
    Objects.requireNonNull(startUpArgs);

    this.configurationProvider = configurationProvider;
    this.configuration = configurationProvider.getConfiguration();
    this.serviceLocator = classLoader;

    this.serverConfiguration = this.configuration.getDefaultServerConfiguration(serverName);
    
    if (this.serverConfiguration == null) {
      if (serverName != null) {
        throw new ConfigurationException("server:" + serverName + " is not a valid server");
      } else {
        throw new ConfigurationException("unable to determine a valid default server");
      } 
    }

    Map<String, ServerConfiguration> serverConfigurationMap = getServerConfigurationMap(configuration.getServerConfigurations());

    this.groupConfiguration = new GroupConfiguration(serverConfigurationMap, this.serverConfiguration.getName());

    this.consistentStartup = consistentStartup;
    this.upgrade = upgrade;

    this.startUpArgs = Arrays.copyOf(startUpArgs, startUpArgs.length);

    processTcProperties(configuration.getTcProperties());
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

  public ConfigurationProvider getConfigurationProvider() {
    return configurationProvider;
  }

  private Map<String, ServerConfiguration> getServerConfigurationMap(Collection<ServerConfiguration> servers) {
    Map<String, ServerConfiguration> serverConfigurationMap = new HashMap<>();
    for (ServerConfiguration server : servers) {
      if (server.getName() != null) {
        serverConfigurationMap.put(server.getName(), server);
      }
    }
    return serverConfigurationMap;
  }

  private static void processTcProperties(Properties tcProperties) {
    Map<String, String> propMap = new HashMap<>();

    if (tcProperties != null) {
      tcProperties.forEach((k, v)->propMap.put(k.toString().trim(), v.toString().trim()));
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
