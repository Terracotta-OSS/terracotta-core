/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.schema;

import com.tc.config.schema.dynamic.BooleanConfigItem;
import com.tc.config.schema.dynamic.IntConfigItem;

public interface UpdateCheckConfig extends NewConfig {
  BooleanConfigItem isEnabled();

  IntConfigItem periodDays();
}
