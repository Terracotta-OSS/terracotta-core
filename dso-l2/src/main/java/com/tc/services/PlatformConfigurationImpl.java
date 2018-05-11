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
import org.terracotta.config.TcConfiguration;
import org.terracotta.entity.PlatformConfiguration;

import com.tc.object.config.schema.L2Config;

/**
 * @author vmad
 */
public class PlatformConfigurationImpl implements PlatformConfiguration {

  private final L2Config l2Config;
  private final TcConfiguration config;

  public PlatformConfigurationImpl(L2Config l2Config, TcConfiguration config) {
    this.l2Config = l2Config;
    this.config = config;
  }

  @Override
  public String getServerName() {
    return l2Config.serverName();
  }

  @Override
  public String getHost() {
    return l2Config.host();
  }

  @Override
  public int getTsaPort() {
    return l2Config.tsaPort().getValue();
  }

  @Override
  public <T> Collection<T> getExtendedConfiguration(Class<T> type) {
    return config.getExtendedConfiguration(type);
  }
}
