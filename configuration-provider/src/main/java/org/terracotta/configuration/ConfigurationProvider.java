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