/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema;

import com.tc.config.schema.dynamic.ConfigItem;

/**
 * Called when someone tries to make an illegal change to configuration.
 */
public interface IllegalConfigurationChangeHandler {

  void changeFailed(ConfigItem item, Object oldValue, Object newValue);
  
}
