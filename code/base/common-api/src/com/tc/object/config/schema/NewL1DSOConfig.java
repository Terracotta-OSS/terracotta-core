/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.config.schema;

import com.tc.config.schema.NewConfig;

/**
 * Represents all configuration read directly by the L1 for DSO and which is independent of application.
 */
public interface NewL1DSOConfig extends NewConfig {

  int faultCount();

  DSOInstrumentationLoggingOptions instrumentationLoggingOptions();

  DSORuntimeLoggingOptions runtimeLoggingOptions();

  DSORuntimeOutputOptions runtimeOutputOptions();

}
