package com.tc.services;

import org.terracotta.entity.PlatformConfiguration;

/**
 * @author vmad
 */
public class PlatformConfigurationImpl implements PlatformConfiguration {

  private final String serverName;

  public PlatformConfigurationImpl(String serverName) {
    this.serverName = serverName;
  }

  @Override
  public String getServerName() {
    return this.serverName;
  }
}
