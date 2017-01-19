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
  private final boolean restartable;

  public PlatformConfigurationImpl(String serverName, TcConfiguration config, boolean restartable) {
    this.serverName = serverName;
    this.config = config;
    this.restartable = restartable;
  }

  @Override
  public String getServerName() {
    return this.serverName;
  }

  @Override
  public <T> Collection<T> getExtendedConfiguration(Class<T> type) {
    return config.getExtendedConfiguration(type);
  }

  @Override
  public boolean isRestartable() {
    return restartable;
  }
}
