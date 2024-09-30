/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
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
import java.io.Closeable;
import java.io.File;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.configuration.FailoverBehavior;
import org.terracotta.entity.ServiceProviderConfiguration;

public class ServerConfigurationManager implements PrettyPrintable {

  private final Logger LOGGER = LoggerFactory.getLogger(ServerConfigurationManager.class);
  private final CachingConfigurationProvider configurationProvider;
  private ServerConfiguration thisServer;
  private final ServiceLocator serviceLocator;
  private final List<String> startUpArgs;
  private final ProductInfo productInfo;

  public ServerConfigurationManager(ConfigurationProvider configurationProvider,
          ServiceLocator classLoader,
          List<String> startUpArgs) {
    Objects.requireNonNull(configurationProvider);
    Objects.requireNonNull(classLoader);
    Objects.requireNonNull(startUpArgs);

    this.configurationProvider = new CachingConfigurationProvider(configurationProvider);
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
    Lock lock = this.configurationProvider.lockAndInitialize(this.startUpArgs);

    try {
      Configuration configuration = configurationProvider.getConfiguration();
      if (configuration == null) {
        throw new ConfigurationException("unable to determine server configuration");
      }

      ServerConfiguration base = configuration.getServerConfiguration();
      if (base == null) {
        throw new ConfigurationException("unable to determine server configuration");
      }
      thisServer = new StableServerConfiguration(base);

      processTcProperties(configuration.getTcProperties());
    } finally {
      lock.unlock();
    }
  }

  public void close() {
    configurationProvider.close();
  }

  public String[] getProcessArguments() {
    return startUpArgs.toArray(new String[startUpArgs.size()]);
  }

  public ServerConfiguration getServerConfiguration() {
    return thisServer;
  }

  public GroupConfiguration getGroupConfiguration() {
      List<ServerConfiguration> serverConfigurationMap = configurationProvider.getStableServerConfigurations();
      return new GroupConfiguration(serverConfigurationMap, getServerConfiguration().getName());
  }

  public InputStream rawConfigFile() {
    try (LockedConfiguration config = configurationProvider.getLockedConfiguration()) {
      String text = config.getRawConfiguration();
      return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
    }
  }

  public String rawConfigString() {
    try (LockedConfiguration config = configurationProvider.getLockedConfiguration()) {
      return config.getRawConfiguration();
    }
  }

  public String[] allCurrentlyKnownServers() {
    return getGroupConfiguration().getMembers();
  }
  
  public boolean isConsistentStartup() {
    try (LockedConfiguration config = configurationProvider.getLockedConfiguration()) {
      return config.isConsistentStartup();
    }
  }
  
  public boolean isPartialConfiguration() {
    try (LockedConfiguration config = configurationProvider.getLockedConfiguration()) {
      return config.isPartialConfiguration();
    }
  }
  
  public FailoverBehavior getFailoverPriority() {
    try (LockedConfiguration config = configurationProvider.getLockedConfiguration()) {
      return config.getFailoverPriority();
    }
  }
    
  public <T> List<T> getExtendedConfiguration(Class<T> type) {
    try (LockedConfiguration config = configurationProvider.getLockedConfiguration()) {
      return config.getExtendedConfiguration(type);
    }
  }

  public List<ServiceProviderConfiguration> getServiceConfigurations() {
    try (LockedConfiguration config = configurationProvider.getLockedConfiguration()) {
      return config.getServiceConfigurations();
    }
  }
  
  public ServiceLocator getServiceLocator() {
    return this.serviceLocator;
  }

  public ConfigurationProvider getConfigurationProvider() {
    return configurationProvider.delegateProvider;
  }

  private static void processTcProperties(Properties tcProperties) {
    Map<String, String> propMap = new HashMap<>();

    if (tcProperties != null) {
      tcProperties.forEach((k, v) -> propMap.put(k.toString().trim(), v.toString().trim()));
    }

    TCPropertiesImpl.getProperties().overwriteTcPropertiesFromConfig(propMap);
  }

  @Override
  public Map<String, ?> getStateMap() {
    try (LockedConfiguration config = configurationProvider.getLockedConfiguration()) {
      if (config instanceof PrettyPrintable) {
        return ((PrettyPrintable) config).getStateMap();
      } else {
        return Collections.emptyMap();
      }
    }
  }
  
  private static class LockedConfiguration implements Configuration, Closeable {
    private final Configuration delegate;
    private final Lock close;

    public LockedConfiguration(Configuration delegate, Lock close) {
      this.delegate = delegate;
      this.close = close;
    }
    
    public void close() {
      close.unlock();
    }

    @Override
    public ServerConfiguration getServerConfiguration() throws ConfigurationException {
      return delegate.getServerConfiguration();
    }

    @Override
    public List<ServerConfiguration> getServerConfigurations() {
      return delegate.getServerConfigurations();
    }

    @Override
    public List<ServiceProviderConfiguration> getServiceConfigurations() {
      return delegate.getServiceConfigurations();
    }

    @Override
    public <T> List<T> getExtendedConfiguration(Class<T> type) {
      return delegate.getExtendedConfiguration(type);
    }

    @Override
    public String getRawConfiguration() {
      return delegate.getRawConfiguration();
    }

    @Override
    public Properties getTcProperties() {
      return delegate.getTcProperties();
    }

    @Override
    public FailoverBehavior getFailoverPriority() {
      return delegate.getFailoverPriority();
    }

    @Override
    public boolean isConsistentStartup() {
      return delegate.isConsistentStartup();
    }

    @Override
    public boolean isPartialConfiguration() {
      return delegate.isPartialConfiguration();
    }
  } 

  private static final class CachingConfigurationProvider implements ConfigurationProvider {

    private final ConfigurationProvider delegateProvider;
    private final Lock lock = new ReentrantLock();

    public CachingConfigurationProvider(ConfigurationProvider delegateProvider) {
      this.delegateProvider = delegateProvider;
    }
    
    List<ServerConfiguration> getStableServerConfigurations() {
      lock.lock();
      try {
        return delegateProvider.getConfiguration().getServerConfigurations().stream().map(StableServerConfiguration::new).collect(Collectors.toList());
      } finally {
        lock.unlock();
      }
    }
    
    Lock lockAndInitialize(List<String> configurationParams) throws ConfigurationException {
      lock.lock();
      initialize(configurationParams);
      return lock;
    }
    
    @Override
    public void initialize(List<String> configurationParams) throws ConfigurationException {
      delegateProvider.initialize(configurationParams);
    }
    
    public LockedConfiguration getLockedConfiguration() {
      lock.lock();
      return new LockedConfiguration(delegateProvider.getConfiguration(), lock);
    }

    @Override
    public Configuration getConfiguration() {
      lock.lock();
      try {
        return delegateProvider.getConfiguration();
      } finally {
        lock.unlock();
      }
    }

    @Override
    public String getConfigurationParamsDescription() {
      return delegateProvider.getConfigurationParamsDescription();
    }

    @Override
    public void close() {
      delegateProvider.close();
    }

    @Override
    public byte[] getSyncData() {
      return delegateProvider.getSyncData();
    }

    @Override
    public void sync(byte[] syncData) {
      lock.lock();
      try {
        delegateProvider.sync(syncData);
      } finally {
        lock.unlock();
      }
    }
  }
  
  private static class StableServerConfiguration implements ServerConfiguration {
    
    private final InetSocketAddress tsaPort;
    private final InetSocketAddress groupPort;
    private final String host;
    private final String name;
    private final int reconnectWindow;
    private final File logDir;
    
    public StableServerConfiguration(ServerConfiguration base) {
      tsaPort = base.getTsaPort();
      groupPort = base.getGroupPort();
      host = base.getHost();
      name = base.getName();
      reconnectWindow = base.getClientReconnectWindow();
      logDir = base.getLogsLocation();
    }

    @Override
    public InetSocketAddress getTsaPort() {
      return tsaPort;
    }

    @Override
    public InetSocketAddress getGroupPort() {
      return groupPort;
    }

    @Override
    public String getHost() {
      return host;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public int getClientReconnectWindow() {
      return reconnectWindow;
    }

    @Override
    public File getLogsLocation() {
      return logDir;
    }
  }
}
