/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.config.schema;

import com.tc.config.schema.dynamic.ConfigItem;

/**
 * Called when someone tries to make an illegal change to configuration.
 */
public interface IllegalConfigurationChangeHandler {

  void changeFailed(ConfigItem item, Object oldValue, Object newValue);
  
}
