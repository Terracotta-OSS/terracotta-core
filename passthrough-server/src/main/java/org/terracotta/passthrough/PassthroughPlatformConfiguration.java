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
 *  The Covered Software is Entity API.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package org.terracotta.passthrough;

import java.util.Collection;
import java.util.Vector;

import org.terracotta.entity.PlatformConfiguration;


/**
 * The wrapper around the thread running as the "server process", within the PassthroughServer.
 * Note that this currently handles not only message processing, but also message execution.
 * In the future, message execution will likely be split out into other threads to better support entity read-write locking
 * and also test concurrency strategy.
 */
public class PassthroughPlatformConfiguration implements PlatformConfiguration {
  private final String serverName;
  private final Collection<Object> extendedConfigurationObjects;
  
  public PassthroughPlatformConfiguration(String serverName, Collection<Object> extendedConfigurationObjects) {
    this.serverName = serverName;
    this.extendedConfigurationObjects = extendedConfigurationObjects;
  }
  
  @Override
  public String getServerName() {
    return this.serverName;
  }

  @Override
  public <T> Collection<T> getExtendedConfiguration(Class<T> type) {
    Collection<T> filtered = new Vector<T>();
    for (Object instance : this.extendedConfigurationObjects) {
      if (type.isInstance(instance)) {
        filtered.add(type.cast(instance));
      }
    }
    return filtered;
  }
}
