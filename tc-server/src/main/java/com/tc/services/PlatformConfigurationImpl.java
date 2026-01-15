/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
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
package com.tc.services;

import com.tc.config.ServerConfigurationManager;
import java.util.Collection;

import org.terracotta.entity.PlatformConfiguration;

import org.terracotta.configuration.Configuration;
import org.terracotta.configuration.ServerConfiguration;

/**
 * @author vmad
 */
public class PlatformConfigurationImpl implements PlatformConfiguration {

  private final ServerConfigurationManager serverConfig;

  public PlatformConfigurationImpl(ServerConfigurationManager serverConfig) {
    this.serverConfig = serverConfig;
  }

  @Override
  public String getServerName() {
    return serverConfig.getServerConfiguration().getName();
  }

  @Override
  public String getHost() {
    return serverConfig.getServerConfiguration().getHost();
  }

  @Override
  public int getTsaPort() {
    return serverConfig.getServerConfiguration().getTsaPort().getPort();
  }

  @Override
  public <T> Collection<T> getExtendedConfiguration(Class<T> type) {
    return serverConfig.getExtendedConfiguration(type);
  }
}
