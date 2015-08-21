/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema.setup.sources;

import com.tc.config.schema.setup.ConfigurationSetupException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Knows how to fetch configuration &mdash; just as an {@link InputStream} &mdash; from some source.
 */
public interface ConfigurationSource {

  /**
   * This method should throw an {@link IOException} if it tried to get the data but couldn't, but it's possible that it
   * might be able to in the future (for example, URLs). It should throw a {@link ConfigurationSetupException} if it
   * tried to get the data but couldn't, and will never be able to (for example, files).
   */
  InputStream getInputStream(long maxTimeoutMillis) throws IOException, ConfigurationSetupException;

  /**
   * Returns the directory from which the configuration was loaded, <em>if</em> such a thing exists. For example, when
   * data is loaded from URLs or resources, this will be <code>null</code>; however, when loaded from files, this
   * will return a valid value.
   */
  File directoryLoadedFrom();

  /**
   * Returns true iff the source was a TCProtocolConfigurationSource.
   */
  boolean isTrusted();
}
