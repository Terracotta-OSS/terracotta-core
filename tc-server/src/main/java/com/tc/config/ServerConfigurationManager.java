/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2026
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.config;


import com.tc.classloader.ServiceLocator;
import com.tc.productinfo.ProductInfo;
import com.tc.properties.TCPropertiesImpl;
import org.terracotta.configuration.Configuration;
import org.terracotta.configuration.ConfigurationProvider;
import org.terracotta.configuration.ServerConfiguration;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import org.terracotta.configuration.ConfigurationException;
import com.tc.text.PrettyPrintable;
import java.net.InetSocketAddress;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.configuration.FailoverBehavior;
import org.terracotta.entity.ServiceProviderConfiguration;

public class ServerConfigurationManager implements PrettyPrintable {

  private static final Logger LOGGER = LoggerFactory.getLogger(ServerConfigurationManager.class);

  private final ConfigurationProvider configurationProvider;
  private final ServiceLocator serviceLocator;
  private final List<String> startUpArgs;
  private final ProductInfo productInfo;

  private Configuration configuration;
  private ServerConfiguration serverConfiguration;
  private GroupConfiguration cachedGroupConfig = new GroupConfiguration(Collections.emptyList(), "");

  public ServerConfigurationManager(ConfigurationProvider configurationProvider,
                                    ServiceLocator classLoader,
                                    List<String> startUpArgs) {
    Objects.requireNonNull(configurationProvider);
    Objects.requireNonNull(classLoader);
    Objects.requireNonNull(startUpArgs);

    LOGGER.info("server started with the following command line arguments {}", startUpArgs);

    this.configurationProvider = configurationProvider;
    this.serviceLocator = classLoader;
    this.startUpArgs = startUpArgs;
    this.productInfo = generateProductInfo(serviceLocator);
  }

  private ProductInfo generateProductInfo(ServiceLocator locator) {
    return ProductInfo.getInstance(locator.createUniversalClassLoader());
  }

  public ProductInfo getProductInfo() {
    return productInfo;
  }

  public void initialize() throws ConfigurationException {
    this.configurationProvider.initialize(this.startUpArgs);

    this.configuration = configurationProvider.getConfiguration();
    if (this.configuration == null) {
      throw new ConfigurationException("unable to determine server configuration");
    }

    this.serverConfiguration = this.configuration.getServerConfiguration();
    if (this.serverConfiguration == null) {
      throw new ConfigurationException("unable to determine server configuration");
    }
    this.cachedGroupConfig = new GroupConfiguration(Collections.singletonList(serverConfiguration), this.serverConfiguration.getName());
    processTcProperties(configuration.getTcProperties());
  }

  public void close() {
    configurationProvider.close();
  }

  public String[] getProcessArguments() {
    return startUpArgs.toArray(String[]::new);
  }

  public ServerConfiguration getServerConfiguration() {
    return this.serverConfiguration;
  }

  public GroupConfiguration getGroupConfiguration() {
    try {
      List<ServerConfiguration> serverConfigurationMap = configuration.getServerConfigurations();
      if (configuration.isRelaySource()) {
        InetSocketAddress relayName = configuration.getRelayPeer();
        cachedGroupConfig = new GroupConfiguration(serverConfigurationMap, this.serverConfiguration.getName(), relayName.getHostString(), relayName.getPort(), 0);
      } else if (configuration.isRelayDestination()) {
        InetSocketAddress relay = configuration.getRelayPeerGroupPort();
        InetSocketAddress relayName = configuration.getRelayPeer();
        cachedGroupConfig = new GroupConfiguration(serverConfigurationMap, this.serverConfiguration.getName(), relayName.getHostString(), relayName.getPort(), relay.getPort());
      } else {
        cachedGroupConfig = new GroupConfiguration(serverConfigurationMap, this.serverConfiguration.getName());
      }
    } catch (Throwable t) {
      LOGGER.info("unable to read configuration, using cached version", t);
    }
    return cachedGroupConfig;
  }

  public InputStream rawConfigFile() {
    String text = configuration.getRawConfiguration();
    return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
  }

  public String rawConfigString() {
    return configuration.getRawConfiguration();
  }

  public boolean isPartialConfiguration() {
    return this.configuration.isPartialConfiguration();
  }

  public ServiceLocator getServiceLocator() {
    return this.serviceLocator;
  }

  public byte[] getSyncData() {
    return configurationProvider.getSyncData();
  }

  public void sync(byte[] data) {
    configurationProvider.sync(data);
  }

  public InetSocketAddress getRelayPeer() {
    return configuration.getRelayPeer();
  }

  public InetSocketAddress getRelayPeerGroupPort() {
    return configuration.getRelayPeerGroupPort();
  }

  public int getNumberOfServers() {
    try {
      return configuration.getServerConfigurations().size();
    } catch (Throwable t) {
      LOGGER.info("unable to read configuration, using cached version", t);
      return cachedGroupConfig.getNodes().size();
    }
  }

  public FailoverBehavior getFailoverPriority() {
    return configuration.getFailoverPriority();
  }

  public <T> List<T> getExtendedConfiguration(Class<T> type) {
    return configuration.getExtendedConfiguration(type);
  }

  public List<ServiceProviderConfiguration> getServiceConfigurations() {
      return configuration.getServiceConfigurations();
  }

  public boolean isRelaySource() {
    return configuration.isRelaySource();
  }

  public boolean isRelayDestination() {
    return configuration.isRelayDestination();
  }

  public boolean isConsistentStartup() {
    return configuration.isConsistentStartup();
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
