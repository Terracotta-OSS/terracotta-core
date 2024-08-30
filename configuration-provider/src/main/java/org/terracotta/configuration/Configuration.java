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
package org.terracotta.configuration;

import java.net.InetSocketAddress;
import org.terracotta.entity.ServiceProviderConfiguration;

import java.util.List;
import java.util.Properties;

public interface Configuration {
  ServerConfiguration getServerConfiguration() throws ConfigurationException;

  List<ServerConfiguration> getServerConfigurations();
  
  List<ServiceProviderConfiguration> getServiceConfigurations();

  <T> List<T> getExtendedConfiguration(Class<T> type);

  String getRawConfiguration();

  Properties getTcProperties();
  
  FailoverBehavior getFailoverPriority();

  default boolean isConsistentStartup() {
    return false;
  }
  
  default boolean isPartialConfiguration() {
    return false;
  }
  
  default boolean isRelaySource() {
    return false;
  }

  default boolean isRelayDestination() {
    return false;
  }
  
  default InetSocketAddress getRelayPeer() {
    return null;
  }
}
