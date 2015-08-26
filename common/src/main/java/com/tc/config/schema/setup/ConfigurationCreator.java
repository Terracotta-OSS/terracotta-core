/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema.setup;

import com.tc.config.schema.repository.BeanRepository;
import org.terracotta.config.TcConfiguration;

import java.io.File;

/**
 * An object that knows how to create configuration from some source and put it into repositories.
 */
public interface ConfigurationCreator {

  /**
   * Load up the configuration.
   */
  void createConfiguration() throws ConfigurationSetupException;

  /**
   * @return the directory containing the configuration file from which config was loaded,
   *         <em>IF<em> such a thing exists; this may well return <code>null</code> (for
   *         example, if configuration was loaded from a URL rather than a file).
   */
  File directoryConfigurationLoadedFrom();

  /**
   * @return true if the ConfigurationSource was a trusted one. Non-trusted sources require that the server be queried
   *         to enforce that the configuration-mode is development.
   */
  boolean loadedFromTrustedSource();

  /**
   * Return the config text as retrieved from source.
   */
  String rawConfigText();
  
  String source();

  String describeSources();

  String reloadServersConfiguration(boolean shouldLogConfig,
                                    boolean reportToConsole) throws ConfigurationSetupException;


  TcConfiguration getParsedConfiguration();
}