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
