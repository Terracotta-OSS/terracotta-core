package com.tc.services;

import java.util.Collection;
import org.terracotta.config.TcConfiguration;
import org.terracotta.entity.PlatformConfiguration;

/**
 * @author vmad
 */
public class PlatformConfigurationImpl implements PlatformConfiguration {

  private final String serverName;
  private final TcConfiguration config;

  public PlatformConfigurationImpl(String serverName, TcConfiguration config) {
    this.serverName = serverName;
    this.config = config;
  }

  @Override
  public String getServerName() {
    return this.serverName;
  }

  @Override
  public <T> Collection<T> getExtendedConfiguration(Class<T> type) {
    return config.getExtendedConfiguration(type);
  }
}
