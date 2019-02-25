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
package com.tc.services;

import java.util.Collection;

import org.terracotta.entity.PlatformConfiguration;

import com.tc.config.ServerConfiguration;
import com.terracotta.config.Configuration;

/**
 * @author vmad
 */
public class PlatformConfigurationImpl implements PlatformConfiguration {

  private final ServerConfiguration serverConfig;
  private final Configuration config;

  public PlatformConfigurationImpl(ServerConfiguration serverConfig, Configuration config) {
    this.serverConfig = serverConfig;
    this.config = config;
  }

  @Override
  public String getServerName() {
    return serverConfig.getName();
  }

  @Override
  public String getHost() {
    return serverConfig.getHost();
  }

  @Override
  public int getTsaPort() {
    return serverConfig.getTsaPort().getValue();
  }

  @Override
  public <T> Collection<T> getExtendedConfiguration(Class<T> type) {
    return config.getExtendedConfiguration(type);
  }
}
