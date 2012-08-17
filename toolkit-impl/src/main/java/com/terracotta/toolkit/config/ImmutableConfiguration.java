/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.config;

import org.terracotta.toolkit.config.Configuration;

import java.io.Serializable;

public class ImmutableConfiguration extends UnclusteredConfiguration {

  public ImmutableConfiguration(Configuration configuration) {
    super(configuration);
  }

  @Override
  public final void internalSetConfigMapping(String name, Serializable value) {
    throw new UnsupportedOperationException("This configuration is immutable, cannot change '" + name + "' to " + value);
  }

}
