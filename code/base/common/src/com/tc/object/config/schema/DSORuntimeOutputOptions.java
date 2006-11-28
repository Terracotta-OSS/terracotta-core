/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.object.config.schema;

import com.tc.config.schema.NewConfig;
import com.tc.config.schema.dynamic.BooleanConfigItem;

/**
 * Represents the runtime-output options for DSO.
 */
public interface DSORuntimeOutputOptions extends NewConfig {

  BooleanConfigItem doAutoLockDetails();
  
  BooleanConfigItem doCaller();
  
  BooleanConfigItem doFullStack();
  
  BooleanConfigItem doFindNeededIncludes();
  
}
