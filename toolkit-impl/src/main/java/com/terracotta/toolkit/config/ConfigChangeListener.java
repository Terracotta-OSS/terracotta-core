/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.config;

import java.io.Serializable;

public interface ConfigChangeListener {
  void configChanged(String fieldChanged, Serializable changedValue);
}
