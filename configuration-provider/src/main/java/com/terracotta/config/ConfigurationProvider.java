package com.terracotta.config;

import java.util.List;
import java.util.ServiceLoader;

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
   * Starts the configuration sync
   *
   * @return the serialized configuration to sync
   */
  default byte[] startSync() {
    return new byte[0];
  }

  /**
   * Syncs with the given configuration
   *
   * @param configuration the serialized configuration
   */
  default void sync(byte[] configuration) {
    // no-op
  }

  /**
   * Ends the ongoing configuration sync if any
   */
  default void endSync() {
    // no-op
  }
}