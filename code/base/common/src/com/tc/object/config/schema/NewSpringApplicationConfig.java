/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
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
