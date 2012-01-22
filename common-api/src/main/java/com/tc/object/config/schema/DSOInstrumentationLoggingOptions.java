/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.config.schema;

import com.tc.config.schema.Config;

/**
 * Represents the instrumentation-logging options for DSO.
 */
public interface DSOInstrumentationLoggingOptions extends Config {

  boolean logClass();

  boolean logLocks();

  boolean logTransientRoot();

  boolean logRoots();

  boolean logDistributedMethods();
  
  void setLogDistributedMethods(boolean val);

}
