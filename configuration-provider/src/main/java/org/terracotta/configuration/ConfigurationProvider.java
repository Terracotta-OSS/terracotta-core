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

import java.util.List;

/**
 * A {@link ConfigurationProvider} provides the configuration for the server start-up
 */

public interface ConfigurationProvider {
  /**
   * Initializes this {@link ConfigurationProvider} using the given configuration parameters
   *
   * <p>Typically configuration parameters are command-line arguments passed during server start-up.</p>
   *
   * @param configurationParams list of configuration parameters supported by this {@link ConfigurationProvider}
   * @throws ConfigurationException if any issues during initialization
   *
   * @see #getConfigurationParamsDescription()
   */
  void initialize(List<String> configurationParams) throws ConfigurationException;

  /**
   * Returns the latest server configuration
   *
   * @return latest {@link Configuration}
   */
  Configuration getConfiguration();

  /**
   * Provides the description for configuration params supported by this {@link Configuration}
   *
   * @return the description for configuration params
   *
   * @see #initialize(List)
   */
  String getConfigurationParamsDescription();

  /**
   * closes this {@link ConfigurationProvider}
   */
  void close();

  /**
   * Provides the data to sync configuration
   *
   * @return the sync data
   */
  default byte[] getSyncData() {
    return new byte[0];
  }

  /**
   * Syncs configuration with the given data
   *
   * @param syncData the sync data
   */
  default void sync(byte[] syncData) {
    // no-op
  }
}