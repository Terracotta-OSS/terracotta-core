/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.schema;

import com.tc.config.schema.dynamic.IntConfigItem;
import com.tc.config.schema.dynamic.StringConfigItem;

public interface NewHaConfig extends NewConfig {
  StringConfigItem haMode();

  IntConfigItem electionTime();

  boolean isNetworkedActivePassive();
}
