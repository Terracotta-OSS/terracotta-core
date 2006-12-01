/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.config.schema;

import com.tc.config.schema.NewConfig;
import com.tc.config.schema.dynamic.ObjectArrayConfigItem;

/**
 * Represents the per-application configuration for Spring.
 */
public interface NewSpringApplicationConfig extends NewConfig {
  
  ObjectArrayConfigItem springApps();

}
