/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema;

import com.tc.config.schema.dynamic.ConfigItem;

/**
 * Called when someone tries to make an illegal change to configuration.
 */
public interface IllegalConfigurationChangeHandler {

  void changeFailed(ConfigItem item, Object oldValue, Object newValue);
  
}
