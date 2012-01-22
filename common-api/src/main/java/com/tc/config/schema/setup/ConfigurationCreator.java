/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema.setup;

import com.tc.config.schema.repository.ApplicationsRepository;
import com.tc.config.schema.repository.MutableBeanRepository;

import java.io.File;

/**
 * An object that knows how to create configuration from some source and put it into repositories.
 */
public interface ConfigurationCreator {

  /**
   * Load up the configuration into the various repositories.
   */
  void createConfigurationIntoRepositories(MutableBeanRepository l1BeanRepository,
                                           MutableBeanRepository l2sBeanRepository,
                                           MutableBeanRepository systemBeanRepository,
                                           MutableBeanRepository tcPropertiesRepository,
                                           ApplicationsRepository applicationsRepository, boolean isClient)
      throws ConfigurationSetupException;

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

  String describeSources();

  String reloadServersConfiguration(MutableBeanRepository l2sBeanRepository, boolean shouldLogConfig,
                                    boolean reportToConsole) throws ConfigurationSetupException;
}
