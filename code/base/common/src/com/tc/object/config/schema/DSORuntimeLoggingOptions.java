/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.object.config.schema;

import com.tc.config.schema.NewConfig;
import com.tc.config.schema.dynamic.BooleanConfigItem;

/**
 * Represents the runtime-logging options for DSO.
 */
public interface DSORuntimeLoggingOptions extends NewConfig {
  
  BooleanConfigItem logLockDebug();
  
  BooleanConfigItem logPartialInstrumentation();
  
  BooleanConfigItem logFieldChangeDebug();
  
  BooleanConfigItem logNonPortableWarning();
  
  BooleanConfigItem logWaitNotifyDebug();
  
  BooleanConfigItem logDistributedMethodDebug();
  
  BooleanConfigItem logNewObjectDebug();

}
