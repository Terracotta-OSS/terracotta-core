/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.object.config.schema;

import com.tc.config.schema.NewConfig;
import com.tc.config.schema.dynamic.IntConfigItem;

/**
 * Represents all configuration read directly by the L1 for DSO and which is independent of application.
 */
public interface NewL1DSOConfig extends NewConfig {

  IntConfigItem faultCount();

  DSOInstrumentationLoggingOptions instrumentationLoggingOptions();

  DSORuntimeLoggingOptions runtimeLoggingOptions();

  DSORuntimeOutputOptions runtimeOutputOptions();

}
