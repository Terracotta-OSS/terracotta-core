/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.config.schema;

import com.tc.config.schema.Config;

/**
 * Represents all configuration read directly by the L1 for DSO and which is independent of application.
 */
public interface L1DSOConfig extends Config {

  int faultCount();

  DSOInstrumentationLoggingOptions instrumentationLoggingOptions();

  DSORuntimeLoggingOptions runtimeLoggingOptions();

  DSORuntimeOutputOptions runtimeOutputOptions();

}
