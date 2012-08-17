/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.object;

import org.terracotta.toolkit.config.Configuration;

import com.terracotta.toolkit.config.ConfigChangeListener;

import java.io.Serializable;

/**
 * A <tt>ClusteredObject</tt> that itself contains one or more other <tt>ClusteredObject</tt>s and a common
 * configuration
 */
public interface ToolkitObjectStripe<C extends TCToolkitObject> extends TCToolkitObject, Iterable<C> {
  /**
   * Returns the configuration associated with this {@link ToolkitObjectStripe}
   */
  Configuration getConfiguration();

  /**
   * Change config parameter and propagate it to the L2.
   */
  void setConfigField(String key, Serializable value);

  void addConfigChangeListener(ConfigChangeListener listener);
}
